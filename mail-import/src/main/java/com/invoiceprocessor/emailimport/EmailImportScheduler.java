package com.invoiceprocessor.emailimport;

import com.invoiceprocessor.application.port.out.EmailFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EmailImportScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EmailImportScheduler.class);

    private final EmailFetcher emailFetcher;
    private final EmailForwarder emailForwarder;
    private final String emailFolder;

    public EmailImportScheduler(
            EmailFetcher emailFetcher,
            EmailForwarder emailForwarder,
            @Value("${email.import.folder:Test123}") String emailFolder) {
        this.emailFetcher = emailFetcher;
        this.emailForwarder = emailForwarder;
        this.emailFolder = emailFolder;
    }

    @Scheduled(fixedDelayString = "${email.import.poll-interval-ms:30000}")
    @Transactional(readOnly = true)
    public void runImport() {
        try {
            logger.info("Starting email import from folder: {}", emailFolder);
            List<EmailFetcher.EmailMessage> emails = emailFetcher.fetchUnreadEmails(emailFolder);
            logger.info("Found {} unread emails", emails.size());

            int success = 0;
            int errors = 0;

            for (EmailFetcher.EmailMessage email : emails) {
                try {
                    logger.debug("Forwarding email {} - {}", email.messageId(), email.subject());
                    emailForwarder.forwardEmail(email);
                    emailFetcher.markAsRead(email.messageId(), emailFolder);
                    success++;
                } catch (Exception ex) {
                    errors++;
                    logger.error("Error processing email {}: {}", email.messageId(), ex.getMessage(), ex);
                }
            }

            logger.info("Email import finished: {} emails processed successfully, {} errors", success, errors);
        } catch (Exception ex) {
            logger.error("Scheduled email import failed: {}", ex.getMessage(), ex);
        }
    }
}
