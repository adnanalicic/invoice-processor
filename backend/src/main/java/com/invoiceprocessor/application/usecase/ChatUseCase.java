package com.invoiceprocessor.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

@Service
public class ChatUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ChatUseCase.class);

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;
    private final String apiUrl;

    public ChatUseCase(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${openai.api.key:}") String apiKey,
            @Value("${openai.model:gpt-4o-mini}") String model,
            @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}") String apiUrl) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(60))
            .build();
        this.apiKey = apiKey;
        this.model = model;
        this.apiUrl = apiUrl;
    }

    public ChatResponse chat(List<ChatMessage> messages) {
        if (apiKey == null || apiKey.isBlank()) {
            return new ChatResponse("LLM API key is not configured. Configure openai.api.key / OPENAI_API_KEY to enable chat.");
        }
        if (messages == null || messages.isEmpty()) {
            return new ChatResponse("No messages provided.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            var mapper = com.fasterxml.jackson.databind.json.JsonMapper.builder().build();
            var root = mapper.createObjectNode();
            root.put("model", model);
            root.put("temperature", 0.2);

            var arr = mapper.createArrayNode();
            for (ChatMessage msg : messages) {
                var node = mapper.createObjectNode();
                node.put("role", msg.role());
                node.put("content", msg.content());
                arr.add(node);
            }
            root.set("messages", arr);

            String body = mapper.writeValueAsString(root);
            logger.debug("Sending chat request to LLM at {} with payload: {}", apiUrl, body);

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(apiUrl, entity, String.class);
            if (response == null || response.isBlank()) {
                return new ChatResponse("Empty response from model.");
            }

            logger.debug("Received chat response from LLM: {}", response);

            var json = mapper.readTree(response);
            String reply = json.path("choices").path(0).path("message").path("content").asText("");
            if (reply.isBlank()) {
                return new ChatResponse("Model did not return any content.");
            }
            return new ChatResponse(reply);
        } catch (Exception e) {
            logger.error("Error talking to LLM: {}", e.getMessage(), e);
            return new ChatResponse("Error talking to model: " + e.getMessage());
        }
    }

    public record ChatMessage(String role, String content) {}

    public record ChatResponse(String reply) {}
}
