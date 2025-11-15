package com.invoiceprocessor.adapter.out.db;

import com.invoiceprocessor.domain.entity.Stack;
import com.invoiceprocessor.domain.entity.StackStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StackJpaEntity {
    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false)
    private Instant receivedAt;

    @Column(nullable = false)
    private String fromAddress;

    @Column(nullable = false)
    private String toAddress;

    @Column(nullable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StackStatus status;

    public static StackJpaEntity fromDomain(Stack stack) {
        StackJpaEntity entity = new StackJpaEntity();
        entity.setId(stack.getId());
        entity.setReceivedAt(stack.getReceivedAt());
        entity.setFromAddress(stack.getFromAddress());
        entity.setToAddress(stack.getToAddress());
        entity.setSubject(stack.getSubject());
        entity.setStatus(stack.getStatus());
        return entity;
    }

    public Stack toDomain() {
        Stack stack = new Stack();
        stack.setId(this.id);
        stack.setReceivedAt(this.receivedAt);
        stack.setFromAddress(this.fromAddress);
        stack.setToAddress(this.toAddress);
        stack.setSubject(this.subject);
        stack.setStatus(this.status);
        return stack;
    }
}
