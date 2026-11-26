package org.delcom.ifs25026p6pbo2026springbootcashflow;

import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.repository.JdbcTransactionRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.repository.JdbcUserRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.repository.SchemaInitializer;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Role;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.User;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

class JdbcRepositoryErrorTests {
    @Test
    void translatesConnectionErrorsFromEveryJdbcRepositoryOperation() {
        DataSource dataSource = failingDataSource();
        JdbcTransactionRepository transactions = new JdbcTransactionRepository(dataSource);
        JdbcUserRepository users = new JdbcUserRepository(dataSource);
        Transaction transaction = new Transaction(
                1, 1, "Gaji", 1, TransactionType.INCOME, null, LocalDateTime.now());
        User user = new User(1, "jose", "hash", "Jose", Role.USER, null);

        assertThrows(IllegalStateException.class, transactions::findAll);
        assertThrows(IllegalStateException.class, () -> transactions.findByUserId(1));
        assertThrows(IllegalStateException.class, () -> transactions.findById(1));
        assertThrows(IllegalStateException.class,
                () -> transactions.save(1, "Gaji", 1, "INCOME", null));
        assertThrows(IllegalStateException.class, () -> transactions.deleteById(1));

        assertThrows(IllegalStateException.class, users::findAll);
        assertThrows(IllegalStateException.class, () -> users.findById(1));
        assertThrows(IllegalStateException.class, () -> users.findByUsername("jose"));
        assertThrows(IllegalStateException.class, () -> users.saveNew("jose", "hash", "Jose", Role.USER));
        assertThrows(IllegalStateException.class, () -> users.deleteById(1));
        assertThrows(IllegalStateException.class, () -> users.update(user));
        assertThrows(IllegalStateException.class, () -> users.findBy(item -> true));
        assertThrows(IllegalStateException.class, () -> transactions.findBy(item -> true));
        assertThrows(IllegalStateException.class, () -> SchemaInitializer.initialize(dataSource));
        assertThrows(UnsupportedOperationException.class, () -> transactions.update(transaction));
    }

    @Test
    void reportsWhenDatabaseDoesNotReturnGeneratedIdentifiers() {
        DataSource dataSource = dataSourceWithoutGeneratedKeys();
        JdbcTransactionRepository transactions = new JdbcTransactionRepository(dataSource);
        JdbcUserRepository users = new JdbcUserRepository(dataSource);

        assertThrows(IllegalStateException.class,
                () -> transactions.save(1, "Gaji", 100, "INCOME", null));
        assertThrows(IllegalStateException.class, () -> users.saveNew("jose", "hash", "Jose", Role.USER));
    }

    private static DataSource failingDataSource() {
        return proxy(DataSource.class, (method, arguments) -> {
            if (method.equals("getConnection")) {
                throw new SQLException("connection unavailable");
            }
            return null;
        });
    }

    private static DataSource dataSourceWithoutGeneratedKeys() {
        ResultSet emptyKeys = proxy(ResultSet.class, (method, arguments) -> {
            if (method.equals("next")) {
                return false;
            }
            return null;
        });
        PreparedStatement statement = proxy(PreparedStatement.class, (method, arguments) -> {
            if (method.equals("executeUpdate")) {
                return 1;
            }
            if (method.equals("getGeneratedKeys")) {
                return emptyKeys;
            }
            return null;
        });
        Connection connection = proxy(Connection.class, (method, arguments) -> {
            if (method.equals("prepareStatement")) {
                return statement;
            }
            return null;
        });
        return proxy(DataSource.class, (method, arguments) -> {
            if (method.equals("getConnection")) {
                return connection;
            }
            return null;
        });
    }

    private static <T> T proxy(Class<T> type, Invocation invocation) {
        Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (instance, method, arguments) -> invocation.invoke(method.getName(), arguments));
        return type.cast(proxy);
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(String method, Object[] arguments) throws Throwable;
    }
}
