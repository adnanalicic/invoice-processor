package com.invoiceprocessor.adapter.out.email;

import com.invoiceprocessor.application.port.out.EmailFetcher;
import com.invoiceprocessor.application.port.out.IntegrationEndpointRepository;
import com.invoiceprocessor.domain.entity.EndpointType;
import com.invoiceprocessor.domain.entity.IntegrationEndpoint;
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
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

@Component
public class ImapEmailFetcher implements EmailFetcher {

    private static final Logger logger = LoggerFactory.getLogger(ImapEmailFetcher.class);

    private final IntegrationEndpointRepository integrationEndpointRepository;
    private final String defaultHost;
    private final int defaultPort;
    private final String defaultUsername;
    private final String defaultPassword;
    private final boolean defaultUseSSL;

    public ImapEmailFetcher(
            IntegrationEndpointRepository integrationEndpointRepository,
            @Value("${email.imap.host:localhost}") String host,
            @Value("${email.imap.port:993}") int port,
            @Value("${email.imap.username:}") String username,
            @Value("${email.imap.password:}") String password,
            @Value("${email.imap.ssl:true}") boolean useSSL) {
        this.integrationEndpointRepository = integrationEndpointRepository;
        this.defaultHost = host;
        this.defaultPort = port;
        this.defaultUsername = username;
        this.defaultPassword = password;
        this.defaultUseSSL = useSSL;
    }

    private Store connect() throws MessagingException {
        EmailConfig config = getConnectionConfig();
        return connect(config);
    }

    private Store connect(EmailConfig config) throws MessagingException {
        logger.info("Connecting to IMAP server: {}:{} (SSL: {})", config.host(), config.port(), config.useSSL());

        Properties props = new Properties();
        props.put("mail.store.protocol", "imap");
        props.put("mail.imap.host", config.host());
        props.put("mail.imap.port", config.port());
        if (config.useSSL()) {
            props.put("mail.imap.ssl.enable", "true");
            props.put("mail.imap.ssl.trust", config.host());
        } else {
            props.put("mail.imap.starttls.enable", "true");
        }

        Session session = Session.getDefaultInstance(props);
        Store store = session.getStore("imap");
        logger.debug("Attempting to connect with username: {}", config.username());
        store.connect(config.host(), config.port(), config.username(), config.password());
        logger.info("Successfully connected to IMAP server");
        return store;
    }

    @Override
    public java.util.List<EmailMessage> fetchUnreadEmails(String folder) {
        java.util.List<EmailMessage> messages = new java.util.ArrayList<>();

        List<IntegrationEndpoint> emailSources = integrationEndpointRepository.findAllByType(EndpointType.EMAIL_SOURCE);
        if (emailSources.isEmpty()) {
            logger.warn("No EMAIL_SOURCE integration endpoints configured. Falling back to application properties");
            messages.addAll(fetchFromConfig(null, folder));
            return messages;
        }

        for (IntegrationEndpoint endpoint : emailSources) {
            String endpointFolder = endpoint.getSettings().getOrDefault("folder", folder);
            try {
                logger.info("Fetching emails for endpoint {} ({}) from folder {}", endpoint.getName(), endpoint.getId(), endpointFolder);
                messages.addAll(fetchFromConfig(endpoint, endpointFolder));
            } catch (Exception e) {
                logger.error("Failed to fetch emails for endpoint {} ({}): {}", endpoint.getName(), endpoint.getId(), e.getMessage(), e);
            }
        }

        return messages;
    }

    private List<EmailMessage> fetchFromConfig(IntegrationEndpoint endpoint, String folder) {
        List<EmailMessage> messages = new ArrayList<>();

        EmailConfig config = (endpoint != null) ? fromEndpoint(endpoint) : getConnectionConfig();
        try (Store store = connect(config)) {
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

            String endpointId = endpoint != null ? endpoint.getId().toString() : null;

            for (Message message : unreadMessages) {
                try {
                    EmailMessage emailMessage = parseMessage(endpointId, folder, (MimeMessage) message);
                    messages.add(emailMessage);
                    logger.debug("Parsed email: {} - {}", emailMessage.messageId(), emailMessage.subject());
                } catch (Exception e) {
                    logger.error("Error parsing message {}: {}", message.getMessageNumber(), e.getMessage(), e);
                }
            }

            emailFolder.close(false);
        } catch (Exception e) {
            logger.error("Unexpected error while fetching emails: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch emails: " + e.getMessage(), e);
        }

        return messages;
    }

    @Override
    public void markAsRead(String endpointId, String messageId, String folder) {
        try {
            IntegrationEndpoint endpoint = null;
            if (endpointId != null && !endpointId.isBlank()) {
                try {
                    UUID id = UUID.fromString(endpointId);
                    endpoint = integrationEndpointRepository.findById(id).orElse(null);
                } catch (IllegalArgumentException ex) {
                    logger.warn("Invalid endpointId '{}' when marking message as read, falling back to default connection", endpointId);
                }
            }

            EmailConfig config = endpoint != null ? fromEndpoint(endpoint) : getConnectionConfig();

            try (Store store = connect(config)) {
                logger.debug("Marking email {} as read in folder {} for endpoint {}", messageId, folder, endpointId);
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
            }
        } catch (Exception e) {
            logger.error("Failed to mark email {} as read in folder {} for endpoint {}: {}", messageId, folder, endpointId, e.getMessage(), e);
            // Don't throw - we don't want to fail the whole import if marking as read fails
        }
    }

    private EmailMessage parseMessage(String endpointId, String folder, MimeMessage message) throws Exception {
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
        java.util.List<EmailAttachment> attachments = extractAttachments(message);

        return new EmailMessage(endpointId, folder, messageId, from, to, subject, body, attachments);
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

    private java.util.List<EmailAttachment> extractAttachments(Message message) throws Exception {
        java.util.List<EmailAttachment> attachments = new java.util.ArrayList<>();

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

    private EmailConfig getConnectionConfig() {
        return integrationEndpointRepository.findByType(EndpointType.EMAIL_SOURCE)
            .map(this::fromEndpoint)
            .orElseGet(() -> {
                logger.warn("No EMAIL_SOURCE integration endpoint configured. Falling back to application properties");
                return new EmailConfig(defaultHost, defaultPort, defaultUsername, defaultPassword, defaultUseSSL);
            });
    }

    private EmailConfig fromEndpoint(IntegrationEndpoint endpoint) {
        Map<String, String> settings = endpoint.getSettings();
        String host = valueOrDefault(settings, "host", "imapHost", defaultHost);
        int port = parsePort(settings.getOrDefault("port", String.valueOf(defaultPort)));
        String username = valueOrDefault(settings, "username", "user", defaultUsername);
        String password = valueOrDefault(settings, "password", "pass", defaultPassword);
        boolean useSsl = parseBoolean(settings.getOrDefault("ssl", String.valueOf(defaultUseSSL)));

        if (host == null || host.isBlank()) {
            host = defaultHost;
        }
        if (username == null || username.isBlank()) {
            username = defaultUsername;
        }
        if (password == null || password.isBlank()) {
            password = defaultPassword;
        }

        return new EmailConfig(host, port, username, password, useSsl);
    }

    private String valueOrDefault(Map<String, String> settings, String primaryKey, String secondaryKey, String fallback) {
        String value = settings.get(primaryKey);
        if ((value == null || value.isBlank()) && secondaryKey != null) {
            value = settings.get(secondaryKey);
        }
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private int parsePort(String portValue) {
        try {
            return Integer.parseInt(portValue);
        } catch (NumberFormatException e) {
            logger.warn("Invalid IMAP port value '{}', falling back to {}", portValue, defaultPort);
            return defaultPort;
        }
    }

    private boolean parseBoolean(String value) {
        return Boolean.parseBoolean(value);
    }

    private record EmailConfig(
        String host,
        int port,
        String username,
        String password,
        boolean useSSL
    ) {}
}
