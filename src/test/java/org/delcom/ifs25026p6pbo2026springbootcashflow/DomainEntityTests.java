package org.delcom.ifs25026p6pbo2026springbootcashflow;

import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Role;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.User;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainEntityTests {
    @Test
    void exposesTransactionAndGeneratedTransactionFields() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 2, 3, 4);
        Transaction transaction = new Transaction(
                7, 3, "Gaji", 1000, TransactionType.INCOME, "proof.png", createdAt);
        AiGeneratedTransaction generated =
                new AiGeneratedTransaction("Belanja", 500, TransactionType.EXPENSE);

        assertEquals(7, transaction.getId());
        assertEquals(3, transaction.getUserId());
        assertEquals("Gaji", transaction.getDescription());
        assertEquals(1000, transaction.getAmount());
        assertEquals(TransactionType.INCOME, transaction.getType());
        assertEquals("proof.png", transaction.getProofPath());
        assertEquals(createdAt, transaction.getCreatedAt());
        assertEquals("Belanja", generated.description());
        assertEquals(500, generated.amount());
        assertEquals(TransactionType.EXPENSE, generated.type());
    }

    @Test
    void userFieldsAndRoleStateAreAvailable() {
        User user = new User(1, "jose", "hash", "Jose", Role.USER, null);
        assertEquals(1, user.getId());
        assertEquals("jose", user.getUsername());
        assertEquals("hash", user.getPasswordHash());
        assertEquals("Jose", user.getFullName());
        assertEquals(Role.USER, user.getRole());
        assertEquals(null, user.getPhotoPath());
        assertFalse(user.isAdmin());

        user.changeFullName("Jose N.");
        user.changePasswordHash("new-hash");
        user.changePhotoPath("photo.png");
        assertEquals("Jose N.", user.getFullName());
        assertEquals("new-hash", user.getPasswordHash());
        assertEquals("photo.png", user.getPhotoPath());
        assertTrue(new User(2, "admin", "hash", "Admin", Role.ADMIN, null).isAdmin());
    }

    @Test
    void typeLabelsFiltersAndAllSortComparatorsAreDefined() {
        Transaction income = new Transaction(
                1, 1, "Gaji", 200, TransactionType.INCOME, null, LocalDateTime.of(2026, 1, 2, 0, 0));
        Transaction expense = new Transaction(
                2, 1, "Belanja", 100, TransactionType.EXPENSE, null, LocalDateTime.of(2026, 1, 1, 0, 0));

        assertEquals("Pemasukan", TransactionType.INCOME.label());
        assertEquals("Pengeluaran", TransactionType.EXPENSE.label());
        assertTrue(TransactionTypeFilter.ALL.matches(income));
        assertTrue(TransactionTypeFilter.INCOME.matches(income));
        assertFalse(TransactionTypeFilter.INCOME.matches(expense));
        assertTrue(TransactionTypeFilter.EXPENSE.matches(expense));
        assertFalse(TransactionTypeFilter.EXPENSE.matches(income));
        assertEquals("Semua tipe", TransactionTypeFilter.ALL.label());
        assertEquals("Pemasukan", TransactionTypeFilter.INCOME.label());
        assertEquals("Pengeluaran", TransactionTypeFilter.EXPENSE.label());

        assertEquals(List.of(income, expense),
                List.of(income, expense).stream().sorted(SortOption.NEWEST_FIRST.comparator()).toList());
        assertEquals(List.of(expense, income),
                List.of(income, expense).stream().sorted(SortOption.OLDEST_FIRST.comparator()).toList());
        assertEquals(List.of(income, expense),
                List.of(expense, income).stream().sorted(SortOption.AMOUNT_DESC.comparator()).toList());
        assertEquals(List.of(expense, income),
                List.of(income, expense).stream().sorted(SortOption.AMOUNT_ASC.comparator()).toList());
        assertEquals(List.of("Terbaru", "Terlama", "Jumlah terbesar", "Jumlah terkecil"),
                List.of(SortOption.values()).stream().map(SortOption::label).toList());
    }

    @Test
    void domainExceptionTypesKeepTheirMessages() {
        DomainException domain = new DomainException("domain");
        ValidationException validation = new ValidationException("validation");
        EntityNotFoundException missing = new EntityNotFoundException(42);

        assertEquals("domain", domain.getMessage());
        assertEquals("validation", validation.getMessage());
        assertEquals("Data dengan ID 42 tidak ditemukan", missing.getMessage());
    }

    @Test
    void applicationShellCanBeConstructed() {
        assertTrue(new App() instanceof com.vaadin.flow.component.page.AppShellConfigurator);
    }
}
