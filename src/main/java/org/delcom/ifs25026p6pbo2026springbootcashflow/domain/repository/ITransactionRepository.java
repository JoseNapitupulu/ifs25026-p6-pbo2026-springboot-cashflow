package org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository;

import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Transaction;

import java.util.List;

public interface ITransactionRepository extends IRepository<Transaction, Integer> {
    Transaction save(int userId, String description, long amount, String type, String proofPath);
    List<Transaction> findByUserId(int userId);
}
