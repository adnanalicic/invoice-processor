package com.invoiceprocessor.application.usecase;

import com.invoiceprocessor.application.port.out.DocumentRepository;
import com.invoiceprocessor.application.port.out.StackRepository;
import com.invoiceprocessor.domain.entity.Document;
import com.invoiceprocessor.domain.entity.Stack;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ListStacksUseCase {

    private final StackRepository stackRepository;
    private final DocumentRepository documentRepository;

    public ListStacksUseCase(StackRepository stackRepository, DocumentRepository documentRepository) {
        this.stackRepository = stackRepository;
        this.documentRepository = documentRepository;
    }

    public StackListResponse execute(int page, int size) {
        List<Stack> stacks = stackRepository.findAll(page, size);
        long total = stackRepository.count();

        Map<UUID, List<Document>> documentsByStackId = stacks.stream()
            .map(Stack::getId)
            .collect(Collectors.toMap(
                Function.identity(),
                stackId -> documentRepository.findByStackId(stackId)
            ));

        List<StackSummary> summaries = stacks.stream()
            .map(stack -> {
                List<Document> documents = documentsByStackId.getOrDefault(stack.getId(), List.of());
                long documentCount = documents.size();
                long invoiceCount = documents.stream()
                    .filter(doc -> doc.getExtractionStatus().equals(com.invoiceprocessor.domain.entity.ExtractionStatus.PROCESSED))
                    .count();
                
                return new StackSummary(
                    stack.getId(),
                    stack.getSubject(),
                    stack.getFromAddress(),
                    stack.getReceivedAt(),
                    stack.getStatus(),
                    documentCount,
                    invoiceCount
                );
            })
            .collect(Collectors.toList());

        return new StackListResponse(summaries, total, page, size);
    }

    public record StackSummary(
        UUID id,
        String subject,
        String fromAddress,
        java.time.Instant receivedAt,
        com.invoiceprocessor.domain.entity.StackStatus status,
        long documentCount,
        long invoiceCount
    ) {}

    public record StackListResponse(
        List<StackSummary> stacks,
        long total,
        int page,
        int size
    ) {}
}
