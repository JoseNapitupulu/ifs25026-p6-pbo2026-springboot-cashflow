package org.delcom.ifs25026p6pbo2026springbootcashflow;

import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.repository.InMemoryTransactionRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25026p6pbo2026springbootcashflow.usecase.FinanceUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinanceUseCaseTests {
    private InMemoryTransactionRepository repository;
    private FinanceUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();
        useCase = new FinanceUseCase(repository);
    }

    @Test
    void addsTransactionsAndCalculatesIncomeExpenseAndBalance() {
        useCase.addTransaction(1, "Gaji", 5_000_000, TransactionType.INCOME, null);
        useCase.addTransaction(1, "Belanja", 250_000, TransactionType.EXPENSE, null);
        useCase.addTransaction(2, "Transaksi orang lain", 700_000, TransactionType.INCOME, null);

        FinanceUseCase.Balance balance = useCase.getBalance(1);

        assertEquals(5_000_000, balance.income());
        assertEquals(250_000, balance.expense());
        assertEquals(4_750_000, balance.balance());
        assertEquals(2, balance.totalTransactions());
    }

    @Test
    void filtersSearchesAndSortsOnlyTransactionsForRequestedUser() {
        useCase.addTransaction(1, "Gaji", 5_000_000, TransactionType.INCOME, null);
        useCase.addTransaction(1, "Belanja", 250_000, TransactionType.EXPENSE, null);
        useCase.addTransaction(2, "Gaji lain", 9_000_000, TransactionType.INCOME, null);

        var result = useCase.listTransactions(1, "gaji", TransactionTypeFilter.INCOME, SortOption.AMOUNT_ASC);

        assertEquals(1, result.size());
        assertEquals("Gaji", result.getFirst().getDescription());
    }

    @Test
    void rejectsInvalidInputAndCrossUserDeletion() {
        assertThrows(ValidationException.class,
                () -> useCase.addTransaction(0, "Gaji", 100, TransactionType.INCOME, null));
        assertThrows(ValidationException.class,
                () -> useCase.addTransaction(1, " ", 100, TransactionType.INCOME, null));
        assertThrows(ValidationException.class,
                () -> useCase.addTransaction(1, "Gaji", 0, TransactionType.INCOME, null));
        assertThrows(ValidationException.class,
                () -> useCase.addTransaction(1, "Gaji", 100, null, null));

        var transaction = useCase.addTransaction(1, "Gaji", 100, TransactionType.INCOME, null);
        assertThrows(ValidationException.class,
                () -> useCase.removeTransactionForUser(2, transaction.getId()));
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void deletingOwnerTransactionUpdatesBalanceAndUnknownIdsFail() {
        var transaction = useCase.addTransaction(1, "Gaji", 100, TransactionType.INCOME, null);

        useCase.removeTransactionForUser(1, transaction.getId());

        assertEquals(0, useCase.getBalance(1).balance());
        assertThrows(EntityNotFoundException.class, () -> useCase.removeTransaction(999));
    }

    @Test
    void supportsDefaultListingAndAdminListing() {
        var first = useCase.addTransaction(1, "First", 100, TransactionType.INCOME, null);
        var second = useCase.addTransaction(1, "Second", 50, TransactionType.EXPENSE, null);

        assertEquals(2, useCase.listAllTransactions().size());
        assertEquals(second.getId(), useCase.listTransactions(1, null, null, null).getFirst().getId());
        assertEquals(2, useCase.listTransactions(1, " ", TransactionTypeFilter.ALL, SortOption.OLDEST_FIRST).size());
        assertEquals(first.getId(), useCase.listTransactions(
                1, "first", TransactionTypeFilter.ALL, SortOption.AMOUNT_DESC).getFirst().getId());
        assertEquals(second.getId(), useCase.listTransactions(
                1, null, TransactionTypeFilter.ALL, SortOption.AMOUNT_ASC).getFirst().getId());
    }

    @Test
    void rejectsNullTextAndInvalidOwnerRemoval() {
        assertThrows(ValidationException.class,
                () -> useCase.addTransaction(1, null, 100, TransactionType.INCOME, null));
        assertThrows(ValidationException.class,
                () -> useCase.addTransaction(1, "Gaji", -1, TransactionType.INCOME, null));
        assertThrows(ValidationException.class, () -> useCase.listTransactions(0, null, null, null));
        assertThrows(EntityNotFoundException.class, () -> useCase.removeTransactionForUser(1, 4));
        assertThrows(EntityNotFoundException.class, () -> useCase.removeTransaction(0));
        assertThrows(EntityNotFoundException.class, () -> useCase.removeTransaction(-1));
    }

    @Test
    void balanceArithmeticRejectsOverflowRatherThanReturningCorruptTotals() {
        useCase.addTransaction(1, "large", Long.MAX_VALUE, TransactionType.INCOME, null);
        useCase.addTransaction(1, "overflow", 1, TransactionType.INCOME, null);
        assertThrows(ArithmeticException.class, () -> useCase.getBalance(1));
    }
}
