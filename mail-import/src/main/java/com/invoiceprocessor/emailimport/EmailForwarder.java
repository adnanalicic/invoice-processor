package com.invoiceprocessor.emailimport;

import com.invoiceprocessor.application.port.out.EmailFetcher;
import com.invoiceprocessor.emailimport.EmailImportRequest.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmailForwarder {

    private static final Logger logger = LoggerFactory.getLogger(EmailForwarder.class);

    private final RestTemplate restTemplate;
    private final String backendBaseUrl;

    public EmailForwarder(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${backend.api.base-url:http://localhost:8080}") String backendBaseUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.backendBaseUrl = backendBaseUrl;
    }

    public void forwardEmail(EmailFetcher.EmailMessage email) {
        try {
            EmailImportRequest request = toRequest(email);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<EmailImportRequest> entity = new HttpEntity<>(request, headers);
            String url = backendBaseUrl + "/api/internal/email-import";

            logger.debug("Forwarding email {} to backend at {}", email.messageId(), url);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            logger.debug("Backend responded with status {}", response.getStatusCode());
        } catch (Exception ex) {
            logger.error("Failed to forward email {} to backend: {}", email.messageId(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to forward email to backend: " + ex.getMessage(), ex);
        }
    }

    private EmailImportRequest toRequest(EmailFetcher.EmailMessage email) {
        List<Attachment> attachments = new ArrayList<>();

        if (email.attachments() != null) {
            for (EmailFetcher.EmailAttachment attachment : email.attachments()) {
                try {
                    attachments.add(new Attachment(
                        attachment.filename(),
                        attachment.contentType(),
                        toByteArray(attachment.content())
                    ));
                } catch (Exception ex) {
                    logger.error("Failed to read attachment {} for email {}: {}",
                        attachment.filename(), email.messageId(), ex.getMessage(), ex);
                }
            }
        }

        return new EmailImportRequest(
            email.from(),
            email.to(),
            email.subject(),
            email.body(),
            attachments
        );
    }

    private byte[] toByteArray(InputStream inputStream) throws Exception {
        try (InputStream in = inputStream; ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[4096];
            int nRead;
            while ((nRead = in.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        }
    }
}

