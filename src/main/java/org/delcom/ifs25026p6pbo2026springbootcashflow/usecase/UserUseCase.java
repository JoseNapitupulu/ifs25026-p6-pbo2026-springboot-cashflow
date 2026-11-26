package org.delcom.ifs25026p6pbo2026springbootcashflow.usecase;

import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.PasswordHasher;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.User;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IUserRepository;

import java.util.List;

public class UserUseCase {
    private final IUserRepository userRepository;

    public UserUseCase(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> listUsers() {
        return userRepository.findAll();
    }

    public User getUser(int id) {
        return userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    }

    public void updateProfile(int id, String fullName, String newPassword, String photoPath) {
        User user = getUser(id);
        if (fullName == null || fullName.isBlank()) {
            throw new ValidationException("Nama lengkap tidak boleh kosong");
        }
        user.changeFullName(fullName.trim());
        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.length() < 8) {
                throw new ValidationException("Password minimal 8 karakter");
            }
            user.changePasswordHash(PasswordHasher.hash(newPassword));
        }
        if (photoPath != null) {
            user.changePhotoPath(photoPath);
        }
        userRepository.update(user);
    }
}
