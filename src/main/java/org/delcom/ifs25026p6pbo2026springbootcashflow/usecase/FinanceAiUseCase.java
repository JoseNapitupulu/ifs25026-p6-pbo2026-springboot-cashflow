package org.delcom.ifs25026p6pbo2026springbootcashflow.usecase;

import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IFinanceAiService;

import java.util.List;

public class FinanceAiUseCase {
    private final IFinanceAiService financeAiService;
    private final FinanceUseCase financeUseCase;

    public FinanceAiUseCase(IFinanceAiService financeAiService, FinanceUseCase financeUseCase) {
        this.financeAiService = financeAiService;
        this.financeUseCase = financeUseCase;
    }

    public List<AiGeneratedTransaction> generateTransactions(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new ValidationException("Prompt tidak boleh kosong");
        }
        List<AiGeneratedTransaction> generated = financeAiService.generateTransactions(prompt.trim());
        if (generated == null || generated.isEmpty()) {
            throw new ValidationException("AI tidak menghasilkan transaksi");
        }
        return List.copyOf(generated);
    }

    public List<Transaction> saveGenerated(int userId, List<AiGeneratedTransaction> generated) {
        if (generated == null || generated.isEmpty()) {
            throw new ValidationException("Pilih transaksi AI yang akan disimpan");
        }
        for (AiGeneratedTransaction item : generated) {
            if (item == null || item.description() == null || item.description().isBlank()
                    || item.amount() <= 0 || item.type() == null) {
                throw new ValidationException("Data transaksi AI tidak valid");
            }
        }
        return generated.stream().map(item -> financeUseCase.addTransaction(
                userId, item.description(), item.amount(), item.type(), null)).toList();
    }
}
