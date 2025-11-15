package com.invoiceprocessor.adapter.in.web;

import com.invoiceprocessor.application.usecase.CreateManualStackUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/internal/email-import")
public class EmailImportController {

    private final CreateManualStackUseCase createManualStackUseCase;

    public EmailImportController(CreateManualStackUseCase createManualStackUseCase) {
        this.createManualStackUseCase = createManualStackUseCase;
    }

    @PostMapping
    public ResponseEntity<ImportEmailResponse> importEmail(@RequestBody EmailImportRequest request) {
        CreateManualStackUseCase.CreateManualStackRequest internalRequest =
            new CreateManualStackUseCase.CreateManualStackRequest(
                request.getFrom(),
                request.getTo(),
                request.getSubject(),
                request.getBody(),
                mapAttachments(request.getAttachments())
            );

        UUID stackId = createManualStackUseCase.execute(internalRequest);
        return ResponseEntity.ok(new ImportEmailResponse(stackId));
    }

    private List<CreateManualStackUseCase.AttachmentContent> mapAttachments(List<EmailImportRequest.Attachment> attachments) {
        if (attachments == null) {
            return List.of();
        }

        return attachments.stream()
            .map(a -> new CreateManualStackUseCase.AttachmentContent(
                a.getFilename(),
                a.getContentType(),
                a.getContent()
            ))
            .toList();
    }

    public record ImportEmailResponse(UUID stackId) {
    }
}

