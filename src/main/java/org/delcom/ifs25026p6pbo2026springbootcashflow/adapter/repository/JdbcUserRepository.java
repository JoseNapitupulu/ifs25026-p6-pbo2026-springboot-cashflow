package org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.repository;

import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Role;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.User;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IUserRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class JdbcUserRepository implements IUserRepository {
    private final DataSource dataSource;

    public JdbcUserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT id, username, password_hash, full_name, role, photo_path FROM users ORDER BY id";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<User> users = new ArrayList<>();
            while (result.next()) {
                users.add(mapRow(result));
            }
            return users;
        } catch (SQLException ex) {
            throw new IllegalStateException("Gagal membaca daftar pengguna", ex);
        }
    }

    @Override
    public Optional<User> findById(Integer id) {
        return findOne("SELECT id, username, password_hash, full_name, role, photo_path FROM users WHERE id = ?", id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, full_name, role, photo_path FROM users WHERE username = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapRow(result)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Gagal mencari pengguna", ex);
        }
    }

    @Override
    public User save(User entity) {
        return saveNew(entity.getUsername(), entity.getPasswordHash(), entity.getFullName(), entity.getRole());
    }

    @Override
    public User saveNew(String username, String passwordHash, String fullName, Role role) {
        String sql = "INSERT INTO users (username, password_hash, full_name, role) VALUES (?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.setString(3, fullName);
            statement.setString(4, role.name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Database tidak mengembalikan ID pengguna baru");
                }
                return new User(keys.getInt(1), username, passwordHash, fullName, role, null);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Gagal menyimpan pengguna", ex);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM users WHERE id = ?")) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Gagal menghapus pengguna", ex);
        }
    }

    @Override
    public void update(User user) {
        String sql = "UPDATE users SET password_hash = ?, full_name = ?, photo_path = ? WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getPasswordHash());
            statement.setString(2, user.getFullName());
            statement.setString(3, user.getPhotoPath());
            statement.setInt(4, user.getId());
            if (statement.executeUpdate() == 0) {
                throw new IllegalStateException("Pengguna tidak ditemukan saat menyimpan perubahan");
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Gagal mengubah pengguna", ex);
        }
    }

    @Override
    public List<User> findBy(Predicate<User> predicate) {
        return findAll().stream().filter(predicate).toList();
    }

    private Optional<User> findOne(String sql, int id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapRow(result)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Gagal membaca pengguna", ex);
        }
    }

    private static User mapRow(ResultSet result) throws SQLException {
        return new User(result.getInt("id"), result.getString("username"),
                result.getString("password_hash"), result.getString("full_name"),
                Role.valueOf(result.getString("role")), result.getString("photo_path"));
    }
}
