package org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository;

import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.AiGeneratedTransaction;

import java.util.List;

public interface IFinanceAiService {
    List<AiGeneratedTransaction> generateTransactions(String prompt);
}
