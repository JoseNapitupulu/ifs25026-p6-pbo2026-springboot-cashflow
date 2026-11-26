package org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.repository;

import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.ITransactionRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class JdbcTransactionRepository implements ITransactionRepository {
    private static final String COLUMNS =
            "id, user_id, description, amount, type, proof_path, created_at";
    private final DataSource dataSource;

    public JdbcTransactionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Transaction> findAll() {
        return query("SELECT " + COLUMNS + " FROM transactions ORDER BY created_at DESC, id DESC", null);
    }

    @Override
    public List<Transaction> findByUserId(int userId) {
        return query("SELECT " + COLUMNS
                + " FROM transactions WHERE user_id = ? ORDER BY created_at DESC, id DESC", userId);
    }

    @Override
    public Optional<Transaction> findById(Integer id) {
        String sql = "SELECT " + COLUMNS + " FROM transactions WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapRow(result)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Gagal membaca transaksi", ex);
        }
    }

    @Override
    public Transaction save(Transaction entity) {
        return save(entity.getUserId(), entity.getDescription(), entity.getAmount(),
                entity.getType().name(), entity.getProofPath());
    }

    @Override
    public Transaction save(int userId, String description, long amount, String type, String proofPath) {
        String sql = """
                INSERT INTO transactions (user_id, description, amount, type, proof_path, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        LocalDateTime createdAt = LocalDateTime.now();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userId);
            statement.setString(2, description);
            statement.setLong(3, amount);
            statement.setString(4, type);
            statement.setString(5, proofPath);
            statement.setTimestamp(6, Timestamp.valueOf(createdAt));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Database tidak mengembalikan ID transaksi baru");
                }
                return new Transaction(keys.getInt(1), userId, description, amount,
                        TransactionType.valueOf(type), proofPath, createdAt);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Gagal menyimpan transaksi", ex);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM transactions WHERE id = ?")) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Gagal menghapus transaksi", ex);
        }
    }

    @Override
    public void update(Transaction entity) {
        throw new UnsupportedOperationException("Transaksi yang sudah dicatat bersifat tetap");
    }

    @Override
    public List<Transaction> findBy(Predicate<Transaction> predicate) {
        return findAll().stream().filter(predicate).toList();
    }

    private List<Transaction> query(String sql, Integer userId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (userId != null) {
                statement.setInt(1, userId);
            }
            try (ResultSet result = statement.executeQuery()) {
                List<Transaction> transactions = new ArrayList<>();
                while (result.next()) {
                    transactions.add(mapRow(result));
                }
                return transactions;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Gagal membaca daftar transaksi", ex);
        }
    }

    private static Transaction mapRow(ResultSet result) throws SQLException {
        return new Transaction(
                result.getInt("id"),
                result.getInt("user_id"),
                result.getString("description"),
                result.getLong("amount"),
                TransactionType.valueOf(result.getString("type")),
                result.getString("proof_path"),
                result.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
