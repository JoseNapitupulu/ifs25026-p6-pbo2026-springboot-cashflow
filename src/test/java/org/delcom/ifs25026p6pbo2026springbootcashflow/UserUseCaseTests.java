package org.delcom.ifs25026p6pbo2026springbootcashflow;

import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.repository.InMemoryUserRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.PasswordHasher;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Role;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25026p6pbo2026springbootcashflow.usecase.UserUseCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserUseCaseTests {
    @Test
    void retrievesAndUpdatesProfileWithoutChangingBlankPassword() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        var user = repository.saveNew("user", "old-hash", "Nama awal", Role.USER);
        UserUseCase useCase = new UserUseCase(repository);

        useCase.updateProfile(user.getId(), "Nama baru", " ", "photo.png");

        var updated = useCase.getUser(user.getId());
        assertEquals("Nama baru", updated.getFullName());
        assertEquals("old-hash", updated.getPasswordHash());
        assertEquals("photo.png", updated.getPhotoPath());
        assertEquals(1, useCase.listUsers().size());

        useCase.updateProfile(user.getId(), "Nama tanpa password", null, null);
        assertEquals("Nama tanpa password", useCase.getUser(user.getId()).getFullName());
    }

    @Test
    void hashesNewPasswordAndRejectsInvalidUpdatesAndUnknownUsers() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        var user = repository.saveNew("user", "old-hash", "Nama", Role.USER);
        UserUseCase useCase = new UserUseCase(repository);

        assertThrows(ValidationException.class, () -> useCase.updateProfile(user.getId(), " ", null, null));
        assertThrows(ValidationException.class, () -> useCase.updateProfile(user.getId(), null, null, null));
        assertThrows(ValidationException.class,
                () -> useCase.updateProfile(user.getId(), "Nama", "short", null));
        useCase.updateProfile(user.getId(), "Nama", "password1", null);
        assertTrue(PasswordHasher.matches("password1", useCase.getUser(user.getId()).getPasswordHash()));
        assertThrows(EntityNotFoundException.class, () -> useCase.getUser(999));
    }
}
