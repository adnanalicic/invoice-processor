package com.invoiceprocessor.application.port.out;

import java.util.List;

/**
 * Port for fetching emails from a mailbox.
 */
public interface EmailFetcher {

    /**
     * Fetches unread emails from the specified folder.
     * May aggregate emails from multiple configured email source endpoints.
     *
     * @param folder The mailbox folder to fetch from (e.g., "INBOX")
     * @return List of email messages
     */
    List<EmailMessage> fetchUnreadEmails(String folder);

    /**
     * Marks an email as read.
     *
     * @param endpointId The identifier of the email source endpoint
     * @param messageId The unique identifier of the email message
     * @param folder The folder where the email is located
     */
    void markAsRead(String endpointId, String messageId, String folder);

    /**
     * Represents an email message with its content and attachments.
     */
    record EmailMessage(
        String endpointId,
        String folder,
        String messageId,
        String from,
        String to,
        String subject,
        String body,
        List<EmailAttachment> attachments
    ) {}

    /**
     * Represents an email attachment.
     */
    record EmailAttachment(
        String filename,
        String contentType,
        java.io.InputStream content
    ) {}
}
