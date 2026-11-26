package org.delcom.ifs25026p6pbo2026springbootcashflow;

import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.repository.InMemoryTransactionRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IFinanceAiService;
import org.delcom.ifs25026p6pbo2026springbootcashflow.usecase.FinanceAiUseCase;
import org.delcom.ifs25026p6pbo2026springbootcashflow.usecase.FinanceUseCase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinanceAiUseCaseTests {
    @Test
    void generatesAndSavesValidatedTransactionsThroughUseCases() {
        FinanceUseCase finance = new FinanceUseCase(new InMemoryTransactionRepository());
        IFinanceAiService service = prompt -> List.of(
                new AiGeneratedTransaction("Gaji", 1_000_000, TransactionType.INCOME));
        FinanceAiUseCase useCase = new FinanceAiUseCase(service, finance);

        var generated = useCase.generateTransactions("catat gaji");
        var saved = useCase.saveGenerated(5, generated);

        assertEquals("Gaji", saved.getFirst().getDescription());
        assertEquals(1_000_000, finance.getBalance(5).income());
    }

    @Test
    void rejectsBlankPromptsEmptyResponsesAndInvalidGeneratedRows() {
        FinanceUseCase finance = new FinanceUseCase(new InMemoryTransactionRepository());
        FinanceAiUseCase emptyResponse = new FinanceAiUseCase(prompt -> List.of(), finance);
        assertThrows(ValidationException.class, () -> emptyResponse.generateTransactions(" "));

        FinanceAiUseCase invalidResponse = new FinanceAiUseCase(prompt -> List.of(), finance);
        assertThrows(ValidationException.class, () -> invalidResponse.generateTransactions("catat"));
        assertThrows(ValidationException.class, () -> invalidResponse.saveGenerated(1, List.of(
                new AiGeneratedTransaction(" ", 0, TransactionType.INCOME))));
        assertThrows(ValidationException.class, () -> invalidResponse.saveGenerated(1, List.of()));
    }
}
