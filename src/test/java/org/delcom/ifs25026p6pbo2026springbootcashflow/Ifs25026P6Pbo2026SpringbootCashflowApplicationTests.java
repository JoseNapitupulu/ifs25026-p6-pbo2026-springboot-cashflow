package org.delcom.ifs25026p6pbo2026springbootcashflow;

import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Role;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.ITransactionRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class Ifs25026P6Pbo2026SpringbootCashflowApplicationTests {
    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private ITransactionRepository transactionRepository;

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
        } finally {
            transactionRepository.deleteById(transaction.getId());
            userRepository.deleteById(user.getId());
        }
    }
}
