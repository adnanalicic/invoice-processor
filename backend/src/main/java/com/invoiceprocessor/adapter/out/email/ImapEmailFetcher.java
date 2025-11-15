package com.invoiceprocessor.adapter.out.email;

import com.invoiceprocessor.application.port.out.EmailFetcher;
import jakarta.mail.*;
import jakarta.mail.Flags.Flag;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Component
public class ImapEmailFetcher implements EmailFetcher {

    private static final Logger logger = LoggerFactory.getLogger(ImapEmailFetcher.class);

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final boolean useSSL;

    public ImapEmailFetcher(
            @Value("${email.imap.host:localhost}") String host,
            @Value("${email.imap.port:993}") int port,
            @Value("${email.imap.username:}") String username,
            @Value("${email.imap.password:}") String password,
            @Value("${email.imap.ssl:true}") boolean useSSL) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.useSSL = useSSL;
    }

    private Store connect() throws MessagingException {
        logger.info("Connecting to IMAP server: {}:{} (SSL: {})", host, port, useSSL);
        Properties props = new Properties();
        props.put("mail.store.protocol", "imap");
        props.put("mail.imap.host", host);
        props.put("mail.imap.port", port);
        if (useSSL) {
            props.put("mail.imap.ssl.enable", "true");
            props.put("mail.imap.ssl.trust", host);
        } else {
            props.put("mail.imap.starttls.enable", "true");
        }

        Session session = Session.getDefaultInstance(props);
        Store store = session.getStore("imap");
        logger.debug("Attempting to connect with username: {}", username);
        store.connect(host, port, username, password);
        logger.info("Successfully connected to IMAP server");
        return store;
    }

    @Override
    public List<EmailMessage> fetchUnreadEmails(String folder) {
        List<EmailMessage> messages = new ArrayList<>();
        
        try (Store store = connect()) {
            logger.info("Opening folder: {}", folder);
            Folder emailFolder = store.getFolder(folder);
            if (emailFolder == null || !emailFolder.exists()) {
                logger.error("Folder '{}' does not exist or cannot be accessed", folder);
                throw new RuntimeException("Folder '" + folder + "' does not exist or cannot be accessed");
            }
            emailFolder.open(Folder.READ_WRITE);
            logger.info("Folder opened. Total messages: {}, Unread: {}", 
                emailFolder.getMessageCount(), emailFolder.getUnreadMessageCount());

            Message[] unreadMessages = emailFolder.search(
                new FlagTerm(new Flags(Flags.Flag.SEEN), false)
            );
            logger.info("Found {} unread messages", unreadMessages.length);

            for (Message message : unreadMessages) {
                try {
                    EmailMessage emailMessage = parseMessage((MimeMessage) message);
                    messages.add(emailMessage);
                    logger.debug("Parsed email: {} - {}", emailMessage.messageId(), emailMessage.subject());
                } catch (Exception e) {
                    logger.error("Error parsing message {}: {}", message.getMessageNumber(), e.getMessage(), e);
                }
            }

            emailFolder.close(false);
        } catch (MessagingException e) {
            logger.error("MessagingException while fetching emails: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch emails: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while fetching emails: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch emails: " + e.getMessage(), e);
        }
        
        return messages;
    }

    @Override
    public void markAsRead(String messageId, String folder) {
        try (Store store = connect()) {
            logger.debug("Marking email {} as read in folder {}", messageId, folder);
            Folder emailFolder = store.getFolder(folder);
            if (emailFolder == null || !emailFolder.exists()) {
                logger.error("Folder '{}' does not exist for marking email as read", folder);
                return;
            }
            emailFolder.open(Folder.READ_WRITE);

            Message[] messages = emailFolder.getMessages();
            boolean found = false;
            for (Message message : messages) {
                String[] messageIds = message.getHeader("Message-ID");
                if (messageIds != null && messageIds.length > 0 && messageIds[0].equals(messageId)) {
                    message.setFlag(Flags.Flag.SEEN, true);
                    found = true;
                    logger.debug("Marked email {} as read", messageId);
                    break;
                }
            }
            
            if (!found) {
                logger.warn("Email with Message-ID {} not found in folder {} to mark as read", messageId, folder);
            }

            emailFolder.close(true);
        } catch (Exception e) {
            logger.error("Failed to mark email {} as read in folder {}: {}", messageId, folder, e.getMessage(), e);
            // Don't throw - we don't want to fail the whole import if marking as read fails
        }
    }

    private EmailMessage parseMessage(MimeMessage message) throws Exception {
        String messageId = message.getHeader("Message-ID") != null 
            ? message.getHeader("Message-ID")[0] 
            : String.valueOf(message.getMessageNumber());
        
        String from = message.getFrom() != null && message.getFrom().length > 0
            ? message.getFrom()[0].toString()
            : "";
        
        String to = message.getRecipients(Message.RecipientType.TO) != null 
            && message.getRecipients(Message.RecipientType.TO).length > 0
            ? message.getRecipients(Message.RecipientType.TO)[0].toString()
            : "";
        
        String subject = message.getSubject() != null ? message.getSubject() : "";
        
        String body = extractBody(message);
        List<EmailAttachment> attachments = extractAttachments(message);
        
        return new EmailMessage(messageId, from, to, subject, body, attachments);
    }

    private String extractBody(Message message) throws Exception {
        if (message.isMimeType("text/plain")) {
            return (String) message.getContent();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart multipart = (MimeMultipart) message.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (bodyPart.isMimeType("text/plain")) {
                    return (String) bodyPart.getContent();
                }
            }
        }
        return "";
    }

    private List<EmailAttachment> extractAttachments(Message message) throws Exception {
        List<EmailAttachment> attachments = new ArrayList<>();
        
        if (message.isMimeType("multipart/*")) {
            MimeMultipart multipart = (MimeMultipart) message.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                MimeBodyPart bodyPart = (MimeBodyPart) multipart.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                    String filename = bodyPart.getFileName();
                    String contentType = bodyPart.getContentType();
                    InputStream content = bodyPart.getInputStream();
                    attachments.add(new EmailAttachment(filename, contentType, content));
                }
            }
        }
        
        return attachments;
    }
}

