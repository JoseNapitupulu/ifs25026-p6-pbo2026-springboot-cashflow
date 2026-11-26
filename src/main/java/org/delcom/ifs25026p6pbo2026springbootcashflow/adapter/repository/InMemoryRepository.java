package org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.repository;

import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class InMemoryRepository<T, ID> implements IRepository<T, ID> {
    private final Map<ID, T> entities = new LinkedHashMap<>();
    private final Function<T, ID> idExtractor;

    public InMemoryRepository(Function<T, ID> idExtractor) {
        this.idExtractor = idExtractor;
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(entities.values());
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(entities.get(id));
    }

    @Override
    public T save(T entity) {
        ID id = idExtractor.apply(entity);
        if (entities.putIfAbsent(id, entity) != null) {
            throw new IllegalStateException("Data dengan ID tersebut sudah ada");
        }
        return entity;
    }

    @Override
    public boolean deleteById(ID id) {
        return entities.remove(id) != null;
    }

    @Override
    public void update(T entity) {
        ID id = idExtractor.apply(entity);
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
