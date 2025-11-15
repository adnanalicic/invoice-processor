package com.invoiceprocessor.application.port.out;

import java.util.List;

/**
 * Port for fetching emails from a mailbox.
 */
public interface EmailFetcher {

    /**
     * Fetches unread emails from the specified folder.
     *
     * @param folder The mailbox folder to fetch from (e.g., "INBOX")
     * @return List of email messages
     */
    List<EmailMessage> fetchUnreadEmails(String folder);

    /**
     * Marks an email as read.
     *
     * @param messageId The unique identifier of the email message
     * @param folder The folder where the email is located
     */
    void markAsRead(String messageId, String folder);

    /**
     * Represents an email message with its content and attachments.
     */
    record EmailMessage(
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

