package org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.repository;

import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class InMemoryRepository<T, I> implements IRepository<T, I> {
    private final Map<I, T> entities = new LinkedHashMap<>();
    private final Function<T, I> idExtractor;

    public InMemoryRepository(Function<T, I> idExtractor) {
        this.idExtractor = idExtractor;
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(entities.values());
    }

    @Override
    public Optional<T> findById(I id) {
        return Optional.ofNullable(entities.get(id));
    }

    @Override
    public T save(T entity) {
        I id = idExtractor.apply(entity);
        if (entities.putIfAbsent(id, entity) != null) {
            throw new IllegalStateException("Data dengan ID tersebut sudah ada");
        }
        return entity;
    }

    @Override
    public boolean deleteById(I id) {
        return entities.remove(id) != null;
    }

    @Override
    public void update(T entity) {
        I id = idExtractor.apply(entity);
        if (!entities.containsKey(id)) {
            throw new IllegalStateException("Data yang akan diubah tidak ditemukan");
        }
        entities.put(id, entity);
    }

    @Override
    public List<T> findBy(Predicate<T> predicate) {
        return entities.values().stream().filter(predicate).toList();
    }
}
