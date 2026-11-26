package org.delcom.ifs25026p6pbo2026springbootcashflow;

import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Role;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.ITransactionRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.repository.SchemaInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class Ifs25026P6Pbo2026SpringbootCashflowApplicationTests {
    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private ITransactionRepository transactionRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads() {
    }

    @Test
    void jdbcRepositoriesPersistAndScopeTransactions() {
        String username = "integration-" + UUID.randomUUID();
        var user = userRepository.saveNew(username, "hash", "Integration User", Role.USER);
        var transaction = transactionRepository.save(
                user.getId(), "Test income", 150_000, TransactionType.INCOME.name(), null);

        try {
            assertTrue(transactionRepository.findById(transaction.getId()).isPresent());
            assertTrue(transactionRepository.findByUserId(user.getId()).stream()
                    .anyMatch(item -> item.getId() == transaction.getId()));
            assertTrue(transactionRepository.findAll().stream()
                    .anyMatch(item -> item.getId() == transaction.getId()));
            assertTrue(transactionRepository.findBy(item -> item.getId() == transaction.getId()).size() == 1);
            assertTrue(transactionRepository.deleteById(transaction.getId()));
            assertTrue(!transactionRepository.deleteById(transaction.getId()));
            assertTrue(transactionRepository.findById(transaction.getId()).isEmpty());

            Transaction savedEntity = transactionRepository.save(new Transaction(
                    999, user.getId(), "Saved through entity", 20, TransactionType.EXPENSE,
                    "proof.png", java.time.LocalDateTime.now()));
            assertTrue(transactionRepository.findById(savedEntity.getId()).isPresent());
            org.junit.jupiter.api.Assertions.assertThrows(
                    UnsupportedOperationException.class, () -> transactionRepository.update(savedEntity));
            transactionRepository.deleteById(savedEntity.getId());

            var secondUser = userRepository.save(new org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.User(
                    999, username + "-copy", "hash", "Copied User", Role.USER, null));
            assertTrue(userRepository.findByUsername(secondUser.getUsername()).isPresent());
            assertTrue(userRepository.findById(999999).isEmpty());
            assertTrue(userRepository.findByUsername("missing-" + UUID.randomUUID()).isEmpty());
            var updatedUser = new org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.User(
                    secondUser.getId(), secondUser.getUsername(), "new-hash", "Updated User", Role.USER, "photo.png");
            userRepository.update(updatedUser);
            assertTrue(userRepository.findById(secondUser.getId()).orElseThrow().getPhotoPath().equals("photo.png"));
            assertTrue(userRepository.findAll().stream().anyMatch(item -> item.getId() == secondUser.getId()));
            assertTrue(userRepository.findBy(item -> item.getId() == secondUser.getId()).size() == 1);
            assertTrue(userRepository.deleteById(secondUser.getId()));
            assertTrue(!userRepository.deleteById(secondUser.getId()));
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                    () -> userRepository.update(new org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.User(
                            999999, "missing", "hash", "Missing", Role.USER, null)));

            SchemaInitializer.initialize(dataSource);
        } finally {
            transactionRepository.deleteById(transaction.getId());
            userRepository.deleteById(user.getId());
        }
    }
}
