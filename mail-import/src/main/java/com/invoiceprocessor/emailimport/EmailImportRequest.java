package com.invoiceprocessor.emailimport;

import java.util.List;

/**
 * REST payload for sending an email (with attachments) from the mail-import
 * microservice to the central backend.
 */
public class EmailImportRequest {

    private String from;
    private String to;
    private String subject;
    private String body;
    private List<Attachment> attachments;

    public EmailImportRequest() {
    }

    public EmailImportRequest(String from, String to, String subject, String body, List<Attachment> attachments) {
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.attachments = attachments;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    public static class Attachment {

        private String filename;
        private String contentType;
        private byte[] content;

        public Attachment() {
        }

        public Attachment(String filename, String contentType, byte[] content) {
            this.filename = filename;
            this.contentType = contentType;
            this.content = content;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public byte[] getContent() {
            return content;
        }

        public void setContent(byte[] content) {
            this.content = content;
        }
    }
}

