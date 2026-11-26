package org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.repository;

import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.ITransactionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class InMemoryTransactionRepository implements ITransactionRepository {
    private final InMemoryRepository<Transaction, Integer> repository =
            new InMemoryRepository<>(Transaction::getId);
    private final AtomicInteger nextId = new AtomicInteger(1);

    @Override
    public List<Transaction> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Transaction> findById(Integer id) {
        return repository.findById(id);
    }

    @Override
    public Transaction save(Transaction entity) {
        return repository.save(entity);
    }

    @Override
    public Transaction save(int userId, String description, long amount, String type, String proofPath) {
        return save(new Transaction(nextId.getAndIncrement(), userId, description, amount,
                TransactionType.valueOf(type), proofPath, LocalDateTime.now()));
    }

    @Override
    public boolean deleteById(Integer id) {
        return repository.deleteById(id);
    }

    @Override
    public void update(Transaction entity) {
        repository.update(entity);
    }

    @Override
    public List<Transaction> findBy(Predicate<Transaction> predicate) {
        return repository.findBy(predicate);
    }

    @Override
    public List<Transaction> findByUserId(int userId) {
        return repository.findBy(transaction -> transaction.getUserId() == userId);
    }
}
