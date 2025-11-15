package com.invoiceprocessor.adapter.in.web;

import com.invoiceprocessor.application.port.out.DocumentRepository;
import com.invoiceprocessor.application.port.out.StorageService;
import com.invoiceprocessor.application.usecase.ReextractDocumentUseCase;
import com.invoiceprocessor.domain.entity.Document;
import com.invoiceprocessor.domain.entity.DocumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    private final ReextractDocumentUseCase reextractDocumentUseCase;
    private final DocumentRepository documentRepository;
    private final StorageService storageService;

    public DocumentController(
            ReextractDocumentUseCase reextractDocumentUseCase,
            DocumentRepository documentRepository,
            StorageService storageService) {
        this.reextractDocumentUseCase = reextractDocumentUseCase;
        this.documentRepository = documentRepository;
        this.storageService = storageService;
    }

    @PostMapping("/{documentId}/reextract")
    public ResponseEntity<ReextractResponse> reextractDocument(@PathVariable UUID documentId) {
        try {
            reextractDocumentUseCase.execute(documentId);
            return ResponseEntity.ok(new ReextractResponse("Re-extraction started successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ReextractResponse("Document not found: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ReextractResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/{documentId}/content")
    public ResponseEntity<byte[]> getDocumentContent(@PathVariable UUID documentId) {
        Optional<Document> optionalDocument = documentRepository.findById(documentId);
        if (optionalDocument.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Document document = optionalDocument.get();
        String key = document.getContentLocation();
        if (key == null || key.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try (InputStream in = storageService.downloadFile(key);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

            byte[] data = new byte[8192];
            int nRead;
            while ((nRead = in.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            byte[] bytes = buffer.toByteArray();
            MediaType mediaType = determineMediaType(document);

            String filename = document.getFilename() != null && !document.getFilename().isBlank()
                ? document.getFilename()
                : document.getId() + "";

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(bytes);
        } catch (Exception e) {
            logger.error("Failed to load content for document {} with key {}: {}", documentId, key, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }

    private MediaType determineMediaType(Document document) {
        DocumentType type = document.getType();
        String filename = document.getFilename() != null ? document.getFilename().toLowerCase() : "";

        if (type == DocumentType.PDF_ATTACHMENT || filename.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        }
        if (type == DocumentType.EMAIL_BODY || filename.endsWith(".txt")) {
            return MediaType.TEXT_PLAIN;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    public record ReextractResponse(String message) {}
}
