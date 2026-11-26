package org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface IRepository<T, I> {
    List<T> findAll();
    Optional<T> findById(I id);
    T save(T entity);
    boolean deleteById(I id);
    void update(T entity);
    List<T> findBy(Predicate<T> predicate);
}
