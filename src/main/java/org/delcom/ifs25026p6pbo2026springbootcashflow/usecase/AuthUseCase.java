package org.delcom.ifs25026p6pbo2026springbootcashflow.usecase;

import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.PasswordHasher;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Role;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.User;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IUserRepository;

public class AuthUseCase {
    private final IUserRepository userRepository;

    public AuthUseCase(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String username, String password, String fullName) {
        if (username == null || username.isBlank()) {
            throw new ValidationException("Username tidak boleh kosong");
        }
        if (password == null || password.length() < 8) {
            throw new ValidationException("Password minimal 8 karakter");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new ValidationException("Nama lengkap tidak boleh kosong");
        }
        String normalizedUsername = username.trim();
        if (userRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new ValidationException("Username sudah digunakan");
        }
        return userRepository.saveNew(normalizedUsername, PasswordHasher.hash(password), fullName.trim(), Role.USER);
    }

    public User login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new ValidationException("Username dan password wajib diisi");
        }
        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new ValidationException("Username atau password salah"));
        if (!PasswordHasher.matches(password, user.getPasswordHash())) {
            throw new ValidationException("Username atau password salah");
        }
        return user;
    }

    public User ensureRootAdmin(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new ValidationException("ROOT_USERNAME dan ROOT_PASSWORD wajib diisi");
        }
        return userRepository.findByUsername(username.trim()).orElseGet(() ->
                userRepository.saveNew(username.trim(), PasswordHasher.hash(password), "Administrator", Role.ADMIN));
    }
}
