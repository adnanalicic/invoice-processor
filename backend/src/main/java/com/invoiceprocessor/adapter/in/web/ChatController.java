package com.invoiceprocessor.adapter.in.web;

import com.invoiceprocessor.application.usecase.ChatUseCase;
import com.invoiceprocessor.application.usecase.ChatWithDocumentUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatUseCase chatUseCase;
    private final ChatWithDocumentUseCase chatWithDocumentUseCase;

    public ChatController(ChatUseCase chatUseCase, ChatWithDocumentUseCase chatWithDocumentUseCase) {
        this.chatUseCase = chatUseCase;
        this.chatWithDocumentUseCase = chatWithDocumentUseCase;
    }

    @PostMapping
    public ResponseEntity<ChatUseCase.ChatResponse> chat(@RequestBody ChatRequest request) {
        ChatUseCase.ChatResponse response;
        if (request.documentId() != null) {
            response = chatWithDocumentUseCase.chat(request.documentId(), request.messages());
        } else {
            response = chatUseCase.chat(request.messages());
        }
        return ResponseEntity.ok(response);
    }

    public record ChatRequest(UUID documentId, List<ChatUseCase.ChatMessage> messages) {}
}
