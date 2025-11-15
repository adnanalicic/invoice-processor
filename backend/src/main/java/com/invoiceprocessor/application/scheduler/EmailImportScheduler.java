package com.invoiceprocessor.application.scheduler;

import com.invoiceprocessor.application.usecase.ImportEmailsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically triggers the email import use case so manual clicking isn't required.
 */
@Component
public class EmailImportScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EmailImportScheduler.class);

    private final ImportEmailsUseCase importEmailsUseCase;
    private final long pollIntervalMs;

    public EmailImportScheduler(
            ImportEmailsUseCase importEmailsUseCase,
            @Value("${email.import.poll-interval-ms:30000}") long pollIntervalMs) {
        this.importEmailsUseCase = importEmailsUseCase;
        this.pollIntervalMs = pollIntervalMs;
    }

    @Scheduled(fixedDelayString = "${email.import.poll-interval-ms:30000}")
    public void runImport() {
        try {
            ImportEmailsUseCase.ImportEmailsResponse response = importEmailsUseCase.execute();
            logger.debug(
                "Scheduled email import finished: {} emails found, {} stacks created, {} documents created, {} errors",
                response.emailsFound(),
                response.stacksCreated(),
                response.documentsCreated(),
                response.errors()
            );
        } catch (Exception ex) {
            logger.error("Scheduled email import failed: {}", ex.getMessage(), ex);
        }
    }
}
