package com.invoiceprocessor.adapter.in.web;

import com.invoiceprocessor.application.usecase.ReextractDocumentUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    private final ReextractDocumentUseCase reextractDocumentUseCase;

    public DocumentController(ReextractDocumentUseCase reextractDocumentUseCase) {
        this.reextractDocumentUseCase = reextractDocumentUseCase;
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

    public record ReextractResponse(String message) {}
}
