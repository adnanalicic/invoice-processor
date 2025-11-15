package com.invoiceprocessor.adapter.out.db;

import com.invoiceprocessor.application.port.out.StackRepository;
import com.invoiceprocessor.domain.entity.Stack;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class StackRepositoryImpl implements StackRepository {

    private final StackJpaRepository jpaRepository;

    public StackRepositoryImpl(StackJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Stack save(Stack stack) {
        StackJpaEntity entity = StackJpaEntity.fromDomain(stack);
        entity = jpaRepository.save(entity);
        return entity.toDomain();
    }

    @Override
    public Optional<Stack> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(StackJpaEntity::toDomain);
    }

    @Override
    public List<Stack> findAll(int page, int size) {
        return jpaRepository.findAll(PageRequest.of(page, size))
            .getContent()
            .stream()
            .map(StackJpaEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }
}
