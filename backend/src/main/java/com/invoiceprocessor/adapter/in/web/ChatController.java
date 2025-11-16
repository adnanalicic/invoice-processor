package com.invoiceprocessor.adapter.in.web;

import com.invoiceprocessor.application.usecase.ChatUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatUseCase chatUseCase;

    public ChatController(ChatUseCase chatUseCase) {
        this.chatUseCase = chatUseCase;
    }

    @PostMapping
    public ResponseEntity<ChatUseCase.ChatResponse> chat(@RequestBody ChatRequest request) {
        var response = chatUseCase.chat(request.messages());
        return ResponseEntity.ok(response);
    }

    public record ChatRequest(List<ChatUseCase.ChatMessage> messages) {}
}

