package com.invoiceprocessor.application.usecase;

import com.invoiceprocessor.application.port.out.DocumentRepository;
import com.invoiceprocessor.application.port.out.StorageService;
import com.invoiceprocessor.domain.entity.Document;
import com.invoiceprocessor.domain.entity.DocumentType;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChatWithDocumentUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ChatWithDocumentUseCase.class);
    private static final int MAX_DOCUMENT_CHARACTERS = 8000;

    private final ChatUseCase chatUseCase;
    private final DocumentRepository documentRepository;
    private final StorageService storageService;

    public ChatWithDocumentUseCase(
            ChatUseCase chatUseCase,
            DocumentRepository documentRepository,
            StorageService storageService) {
        this.chatUseCase = chatUseCase;
        this.documentRepository = documentRepository;
        this.storageService = storageService;
    }

    public ChatUseCase.ChatResponse chat(UUID documentId, List<ChatUseCase.ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ChatUseCase.ChatResponse("No messages provided.");
        }

        String documentText = loadDocumentText(documentId);
        if (documentText == null || documentText.isBlank()) {
            return new ChatUseCase.ChatResponse("Could not load document content for id " + documentId);
        }

        var contextMessage = new ChatUseCase.ChatMessage(
            "system",
            "You are answering questions about a single PDF/document.\n" +
            "Use the following content as the primary source for your answers.\n" +
            "If the question cannot be answered from this content, say so clearly.\n\n" +
            documentText
        );

        List<ChatUseCase.ChatMessage> augmentedMessages = new ArrayList<>();
        augmentedMessages.add(contextMessage);
        augmentedMessages.addAll(messages);

        return chatUseCase.chat(augmentedMessages);
    }

    private String loadDocumentText(UUID documentId) {
        if (documentId == null) {
            return null;
        }

        Optional<Document> optionalDocument = documentRepository.findById(documentId);
        if (optionalDocument.isEmpty()) {
            logger.warn("Document with id {} not found for chat-with-document.", documentId);
            return null;
        }

        Document document = optionalDocument.get();
        String key = document.getContentLocation();
        if (key == null || key.isBlank()) {
            logger.warn("Document {} has no content location configured.", documentId);
            return null;
        }

        try (InputStream in = storageService.downloadFile(key)) {
            String text;

            if (isPdf(document)) {
                // Use PDFBox to extract text from PDFs so the model sees readable content.
                try (PDDocument pdfDoc = PDDocument.load(in)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    text = stripper.getText(pdfDoc);
                }
            } else {
                // Fallback for non-PDFs: treat as UTF-8 text.
                try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                    byte[] data = new byte[8192];
                    int nRead;
                    while ((nRead = in.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    byte[] bytes = buffer.toByteArray();
                    text = new String(bytes, StandardCharsets.UTF_8);
                }
            }

            if (text == null) {
                return null;
            }

            text = text.trim();
            return text.length() > MAX_DOCUMENT_CHARACTERS
                ? text.substring(0, MAX_DOCUMENT_CHARACTERS)
                : text;
        } catch (Exception e) {
            logger.error("Failed to load content for document {} from key {}: {}", documentId, key, e.getMessage(), e);
            return null;
        }
    }

    private boolean isPdf(Document document) {
        if (document.getType() == DocumentType.PDF_ATTACHMENT) {
            return true;
        }
        String filename = document.getFilename();
        return filename != null && filename.toLowerCase().endsWith(".pdf");
    }
}
