package org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.repository;

import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IFinanceAiService;

import java.util.List;

public class InMemoryFinanceAiService implements IFinanceAiService {
    @Override
    public List<AiGeneratedTransaction> generateTransactions(String prompt) {
        return List.of(
                new AiGeneratedTransaction("Contoh pemasukan dari " + prompt.trim(), 1_000_000, TransactionType.INCOME),
                new AiGeneratedTransaction("Contoh pengeluaran dari " + prompt.trim(), 250_000, TransactionType.EXPENSE)
        );
    }
}
