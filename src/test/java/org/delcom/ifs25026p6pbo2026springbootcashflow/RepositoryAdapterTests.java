package org.delcom.ifs25026p6pbo2026springbootcashflow;

import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.repository.InMemoryFinanceAiService;
import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.repository.InMemoryRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.repository.InMemoryTransactionRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.repository.InMemoryUserRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Role;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryAdapterTests {
    @Test
    void genericInMemoryRepositorySupportsCrudAndRejectsDuplicateOrMissingRows() {
        InMemoryRepository<User, Integer> repository = new InMemoryRepository<>(User::getId);
        User user = new User(1, "jose", "hash", "Jose", Role.USER, null);

        assertEquals(user, repository.save(user));
        assertEquals(user, repository.findById(1).orElseThrow());
        assertEquals(1, repository.findAll().size());
        assertEquals(1, repository.findBy(item -> item.getUsername().startsWith("jo")).size());
        assertThrows(IllegalStateException.class, () -> repository.save(user));

        user.changeFullName("Jose N.");
        repository.update(user);
        assertEquals("Jose N.", repository.findById(1).orElseThrow().getFullName());
        User missingUser = new User(2, "other", "hash", "Other", Role.USER, null);
        assertThrows(IllegalStateException.class, () -> repository.update(missingUser));
        assertTrue(repository.deleteById(1));
        assertFalse(repository.deleteById(1));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void inMemoryTransactionRepositoryImplementsAllPortOperations() {
        InMemoryTransactionRepository repository = new InMemoryTransactionRepository();
        Transaction generated = repository.save(1, "Gaji", 100, "INCOME", null);
        Transaction supplied = new Transaction(
                20, 2, "Belanja", 50, TransactionType.EXPENSE, "proof.png", LocalDateTime.now());
        repository.save(supplied);

        assertEquals(2, repository.findAll().size());
        assertEquals(generated, repository.findById(generated.getId()).orElseThrow());
        assertEquals(1, repository.findByUserId(1).size());
        assertEquals(1, repository.findBy(item -> item.getAmount() > 50).size());
        repository.update(new Transaction(
                20, 2, "Updated", 50, TransactionType.EXPENSE, "proof.png", supplied.getCreatedAt()));
        assertEquals("Updated", repository.findById(20).orElseThrow().getDescription());
        assertTrue(repository.deleteById(20));
        assertFalse(repository.deleteById(20));
    }

    @Test
    void inMemoryUserRepositorySupportsCrudAndRejectsDuplicateUsernames() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User user = repository.saveNew("jose", "hash", "Jose", Role.USER);

        assertEquals(user, repository.findById(user.getId()).orElseThrow());
        assertEquals(user, repository.findByUsername("jose").orElseThrow());
        assertTrue(repository.findByUsername("missing").isEmpty());
        assertThrows(IllegalStateException.class,
                () -> repository.saveNew("jose", "hash", "Other", Role.USER));
        repository.update(new User(user.getId(), "jose", "new-hash", "Updated", Role.USER, null));
        assertEquals("Updated", repository.findById(user.getId()).orElseThrow().getFullName());
        assertEquals(1, repository.findBy(item -> item.getRole() == Role.USER).size());
        assertTrue(repository.deleteById(user.getId()));
        assertFalse(repository.deleteById(user.getId()));
    }

    @Test
    void inMemoryAiAdapterReturnsBothTransactionTypes() {
        var result = new InMemoryFinanceAiService().generateTransactions("catat gaji");

        assertEquals(2, result.size());
        assertEquals(TransactionType.INCOME, result.getFirst().type());
        assertEquals(TransactionType.EXPENSE, result.getLast().type());
        assertTrue(result.getFirst().description().contains("catat gaji"));
    }
}
