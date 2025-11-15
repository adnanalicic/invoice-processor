package com.invoiceprocessor.application.port.out;

import com.invoiceprocessor.domain.entity.Stack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StackRepository {
    Stack save(Stack stack);
    Optional<Stack> findById(UUID id);
    List<Stack> findAll(int page, int size);
    long count();
}
