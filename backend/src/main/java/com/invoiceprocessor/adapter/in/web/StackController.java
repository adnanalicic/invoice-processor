package com.invoiceprocessor.adapter.in.web;

import com.invoiceprocessor.application.usecase.CreateManualStackUseCase;
import com.invoiceprocessor.application.usecase.GetStackDetailsUseCase;
import com.invoiceprocessor.application.usecase.ImportEmailsUseCase;
import com.invoiceprocessor.application.usecase.ListStacksUseCase;
import com.invoiceprocessor.application.usecase.ProcessNewEmailStackUseCase;
import com.invoiceprocessor.domain.entity.DocumentType;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stacks")
@CrossOrigin(origins = "*")
public class StackController {

    private final ListStacksUseCase listStacksUseCase;
    private final GetStackDetailsUseCase getStackDetailsUseCase;
    private final ProcessNewEmailStackUseCase processNewEmailStackUseCase;
    private final ImportEmailsUseCase importEmailsUseCase;
    private final CreateManualStackUseCase createManualStackUseCase;

    public StackController(
            ListStacksUseCase listStacksUseCase,
            GetStackDetailsUseCase getStackDetailsUseCase,
            ProcessNewEmailStackUseCase processNewEmailStackUseCase,
            ImportEmailsUseCase importEmailsUseCase,
            CreateManualStackUseCase createManualStackUseCase) {
        this.listStacksUseCase = listStacksUseCase;
        this.getStackDetailsUseCase = getStackDetailsUseCase;
        this.processNewEmailStackUseCase = processNewEmailStackUseCase;
        this.importEmailsUseCase = importEmailsUseCase;
        this.createManualStackUseCase = createManualStackUseCase;
    }

    @GetMapping
    public ResponseEntity<ListStacksUseCase.StackListResponse> listStacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ListStacksUseCase.StackListResponse response = listStacksUseCase.execute(page, size);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/manualUpload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ManualStackResponse> manualUpload(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam(required = false) String body,
            @RequestPart(value = "attachments", required = false) MultipartFile[] attachments) {
        try {
            List<CreateManualStackUseCase.AttachmentContent> files = attachments == null
                ? List.of()
                : attachmentsWithContent(attachments);

            UUID stackId = createManualStackUseCase.execute(
                new CreateManualStackUseCase.CreateManualStackRequest(
                    from,
                    to,
                    subject,
                    body,
                    files
                )
            );

            return ResponseEntity.ok(new ManualStackResponse(stackId, "Stack created successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ManualStackResponse(null, "Error: " + e.getMessage()));
        }
    }

    private List<CreateManualStackUseCase.AttachmentContent> attachmentsWithContent(MultipartFile[] attachments) {
        return java.util.Arrays.stream(attachments)
            .map(file -> {
                try {
                    return new CreateManualStackUseCase.AttachmentContent(
                        file.getOriginalFilename(),
                        file.getContentType(),
                        file.getBytes()
                    );
                } catch (java.io.IOException e) {
                    throw new RuntimeException("Failed to read attachment " + file.getOriginalFilename(), e);
                }
            })
            .toList();
    }

    @GetMapping("/{stackId}")
    public ResponseEntity<GetStackDetailsUseCase.StackDetailsResponse> getStackDetails(
            @PathVariable UUID stackId) {
        try {
            GetStackDetailsUseCase.StackDetailsResponse response = getStackDetailsUseCase.execute(stackId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/simulateEmail")
    public ResponseEntity<SimulateEmailResponse> simulateEmail(@RequestBody SimulateEmailRequest request) {
        try {
            UUID stackId = processNewEmailStackUseCase.execute(
                new ProcessNewEmailStackUseCase.SimulateEmailRequest(
                    request.from(),
                    request.to(),
                    request.subject(),
                    request.body(),
                    request.attachments() != null ? request.attachments().stream()
                        .map(att -> new ProcessNewEmailStackUseCase.AttachmentInfo(
                            att.filename(),
                            att.type(),
                            att.contentReference()
                        ))
                        .toList() : List.of()
                )
            );
            return ResponseEntity.ok(new SimulateEmailResponse(stackId, "Email processed successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new SimulateEmailResponse(null, "Error: " + e.getMessage()));
        }
    }

    @PostMapping("/importEmails")
    public ResponseEntity<ImportEmailsResponse> importEmails() {
        try {
            ImportEmailsUseCase.ImportEmailsResponse response = importEmailsUseCase.execute();
            return ResponseEntity.ok(new ImportEmailsResponse(
                response.emailsFound(),
                response.stacksCreated(),
                response.documentsCreated(),
                response.errors(),
                "Emails imported successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ImportEmailsResponse(0, 0, 0, 1, "Error: " + e.getMessage()));
        }
    }


    public record SimulateEmailRequest(
        String from,
        String to,
        String subject,
        String body,
        List<AttachmentRequest> attachments
    ) {}

    public record AttachmentRequest(
        String filename,
        DocumentType type,
        String contentReference
    ) {}

    public record SimulateEmailResponse(
        UUID stackId,
        String message
    ) {}

    public record ImportEmailsResponse(
        int emailsFound,
        int stacksCreated,
        int documentsCreated,
        int errors,
        String message
    ) {}

    public record ManualStackResponse(
        UUID stackId,
        String message
    ) {}

}
