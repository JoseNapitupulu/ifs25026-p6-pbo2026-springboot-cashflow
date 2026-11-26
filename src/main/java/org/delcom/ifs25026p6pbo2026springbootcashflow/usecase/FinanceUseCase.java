package org.delcom.ifs25026p6pbo2026springbootcashflow.usecase;

import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.ITransactionRepository;

import java.util.List;
import java.util.stream.Stream;

public class FinanceUseCase {
    private final ITransactionRepository transactionRepository;

    public FinanceUseCase(ITransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public record Balance(long income, long expense, long balance, long totalTransactions) {
    }

    public Transaction addTransaction(
            int userId, String description, long amount, TransactionType type, String proofPath
    ) {
        validateTransaction(userId, description, amount, type);
        return transactionRepository.save(userId, description.trim(), amount, type.name(), proofPath);
    }

    public List<Transaction> listTransactions(
            int userId, String keyword, TransactionTypeFilter filter, SortOption sort
    ) {
        if (userId <= 0) {
            throw new ValidationException("ID pengguna tidak valid");
        }
        TransactionTypeFilter selectedFilter = filter == null ? TransactionTypeFilter.ALL : filter;
        SortOption selectedSort = sort == null ? SortOption.NEWEST_FIRST : sort;
        Stream<Transaction> transactions = transactionRepository.findByUserId(userId).stream()
                .filter(selectedFilter::matches);
        if (keyword != null && !keyword.isBlank()) {
            String normalized = keyword.trim().toLowerCase();
            transactions = transactions.filter(
                    transaction -> transaction.getDescription().toLowerCase().contains(normalized));
        }
        return transactions.sorted(selectedSort.comparator()).toList();
    }

    public List<Transaction> listAllTransactions() {
        return transactionRepository.findAll();
    }

    public Balance getBalance(int userId) {
        long income = 0;
        long expense = 0;
        long count = 0;
        for (Transaction transaction : transactionRepository.findByUserId(userId)) {
            if (transaction.getType() == TransactionType.INCOME) {
                income = Math.addExact(income, transaction.getAmount());
            } else {
                expense = Math.addExact(expense, transaction.getAmount());
            }
            count++;
        }
        return new Balance(income, expense, Math.subtractExact(income, expense), count);
    }

    public void removeTransactionForUser(int userId, int id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));
        if (transaction.getUserId() != userId) {
            throw new ValidationException("Transaksi tidak dimiliki pengguna ini");
        }
        removeTransaction(id);
    }

    public void removeTransaction(int id) {
        if (id <= 0 || !transactionRepository.deleteById(id)) {
            throw new EntityNotFoundException(id);
        }
    }

    private static void validateTransaction(int userId, String description, long amount, TransactionType type) {
        if (userId <= 0) {
            throw new ValidationException("ID pengguna tidak valid");
        }
        if (description == null || description.isBlank()) {
            throw new ValidationException("Keterangan transaksi tidak boleh kosong");
        }
        if (amount <= 0) {
            throw new ValidationException("Jumlah harus lebih besar dari nol");
        }
        if (type == null) {
            throw new ValidationException("Tipe transaksi harus dipilih");
        }
    }
}
