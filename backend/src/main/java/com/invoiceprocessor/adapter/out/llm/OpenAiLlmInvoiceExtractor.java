package com.invoiceprocessor.adapter.out.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceprocessor.application.port.out.LlmInvoiceExtractor;
import com.invoiceprocessor.application.port.out.StorageService;
import com.invoiceprocessor.domain.entity.Document;
import com.invoiceprocessor.domain.entity.LlmClassification;
import com.invoiceprocessor.domain.service.InvoiceProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;

/**
 * OpenAI-backed implementation of {@link LlmInvoiceExtractor}.
 *
 * Sends document content to a ChatGPT-style model and expects a small JSON
 * response with:
 *   { "isInvoice": true/false, "amount": number, "creditor": "name" }
 *
 * If the document is not an invoice, the model should respond with
 *   { "isInvoice": false, "message": "not an invoice" }
 *
 * This adapter is deliberately conservative: on any failure it falls back
 * to classifying the document as NOT_INVOICE so the pipeline can continue.
 */
@Component
public class OpenAiLlmInvoiceExtractor implements LlmInvoiceExtractor {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiLlmInvoiceExtractor.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestTemplate restTemplate;
    private final StorageService storageService;
    private final String apiKey;
    private final String model;
    private final String apiUrl;

    public OpenAiLlmInvoiceExtractor(
            RestTemplateBuilder restTemplateBuilder,
            StorageService storageService,
            @Value("${openai.api.key:}") String apiKey,
            @Value("${openai.model:gpt-4o-mini}") String model,
            @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}") String apiUrl) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(30))
            .build();
        this.storageService = storageService;
        this.apiKey = apiKey;
        this.model = model;
        this.apiUrl = apiUrl;
    }

    @Override
    public InvoiceProcessingService.LlmExtractionResult extract(Document document) {
        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("OpenAI API key is not configured. Classifying document {} as NOT_INVOICE.", document.getId());
            return notInvoiceResult();
        }

        String content = loadDocumentContent(document);
        if (content == null || content.isBlank()) {
            logger.warn("Document {} content is empty or could not be loaded. Classifying as NOT_INVOICE.", document.getId());
            return notInvoiceResult();
        }

        try {
            String prompt = buildPrompt(content);
            String responseJson = callOpenAi(prompt);
            return mapResponseToResult(responseJson);
        } catch (Exception e) {
            logger.error("Error while calling OpenAI for document {}: {}", document.getId(), e.getMessage(), e);
            return notInvoiceResult();
        }
    }

    private String loadDocumentContent(Document document) {
        try {
            String key = document.getContentLocation();
            if (key == null || key.isBlank()) {
                return null;
            }
            try (InputStream in = storageService.downloadFile(key);
                 ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                byte[] data = new byte[8192];
                int nRead;
                while ((nRead = in.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                byte[] bytes = buffer.toByteArray();
                // For now assume UTF‑8 text; PDFs may not render perfectly but
                // this keeps the integration lightweight.
                String text = new String(bytes, StandardCharsets.UTF_8);
                // Truncate excessively large documents to keep token usage reasonable.
                int maxLength = 8000;
                return text.length() > maxLength ? text.substring(0, maxLength) : text;
            }
        } catch (Exception e) {
            logger.error("Failed to load document content for {}: {}", document.getId(), e.getMessage(), e);
            return null;
        }
    }

    private String buildPrompt(String content) {
        return """
            You are an assistant that classifies documents as invoices or not and extracts at most two key fields.

            Read the following document content and respond in strict JSON with the shape:
            {
              "isInvoice": true or false,
              "amount": number or null,
              "currency": string or null,
              "creditor": string or null,
              "message": string
            }

            Rules:
            - If the document is clearly an invoice, set isInvoice=true, fill amount (total invoice amount), currency and creditor (supplier name).
            - If it is not an invoice, set isInvoice=false, amount=null, currency=null, creditor=null and a short message like "not an invoice".
            - Do NOT include any other fields.

            Document content:
            ```
            """ + content + "\n```";
    }

    private String callOpenAi(String prompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // Minimal chat-completions payload
        String body = OBJECT_MAPPER.createObjectNode()
            .put("model", model)
            .put("temperature", 0.0)
            .set("messages", OBJECT_MAPPER.createArrayNode()
                .add(OBJECT_MAPPER.createObjectNode()
                    .put("role", "system")
                    .put("content", "You are a strict JSON API for invoice detection."))
                .add(OBJECT_MAPPER.createObjectNode()
                    .put("role", "user")
                    .put("content", prompt)))
            .toString();

        logger.debug("Sending extraction request to LLM at {} with prompt: {}", apiUrl, body);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        String response = restTemplate.postForObject(apiUrl, entity, String.class);
        if (response == null || response.isBlank()) {
            throw new IllegalStateException("Empty response from OpenAI");
        }
        return response;
    }

    private InvoiceProcessingService.LlmExtractionResult mapResponseToResult(String responseJson) throws Exception {
        JsonNode root = OBJECT_MAPPER.readTree(responseJson);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
            return notInvoiceResult();
        }

        String content = contentNode.asText();
        // Model should return pure JSON; if it wraps it in text, try to parse the first JSON object.
        String jsonPart = extractFirstJsonObject(content);
        JsonNode result = OBJECT_MAPPER.readTree(jsonPart);

        boolean isInvoice = result.path("isInvoice").asBoolean(false);
        if (!isInvoice) {
            return notInvoiceResult();
        }

        String creditor = textOrNull(result.path("creditor"));
        BigDecimal amount = decimalOrNull(result.path("amount"));
        String currency = textOrNull(result.path("currency"));

        return new InvoiceProcessingService.LlmExtractionResult(
            LlmClassification.INVOICE,
            null,
            LocalDate.now(),
            creditor,
            amount,
            currency
        );
    }

    private String extractFirstJsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private String textOrNull(JsonNode node) {
        String value = node.isMissingNode() || node.isNull() ? null : node.asText();
        return (value == null || value.isBlank()) ? null : value;
    }

    private BigDecimal decimalOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(node.asText());
        } catch (Exception e) {
            return null;
        }
    }

    private InvoiceProcessingService.LlmExtractionResult notInvoiceResult() {
        return new InvoiceProcessingService.LlmExtractionResult(
            LlmClassification.NOT_INVOICE,
            null,
            null,
            null,
            null,
            null
        );
    }
}
