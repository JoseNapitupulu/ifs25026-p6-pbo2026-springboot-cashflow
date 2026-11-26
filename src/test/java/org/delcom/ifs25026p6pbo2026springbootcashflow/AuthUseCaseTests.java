package org.delcom.ifs25026p6pbo2026springbootcashflow;

import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.repository.InMemoryUserRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.PasswordHasher;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Role;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25026p6pbo2026springbootcashflow.usecase.AuthUseCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthUseCaseTests {
    @Test
    void registrationHashesPasswordAndAuthenticatesUser() {
        AuthUseCase useCase = new AuthUseCase(new InMemoryUserRepository());

        var registered = useCase.register("jose", "secret123", "Jose Napitupulu");

        assertNotEquals("secret123", registered.getPasswordHash());
        assertTrue(PasswordHasher.matches("secret123", registered.getPasswordHash()));
        assertEquals(registered.getId(), useCase.login("jose", "secret123").getId());
        assertThrows(ValidationException.class, () -> useCase.login("jose", "wrongpass"));
    }

    @Test
    void rejectsDuplicateUserAndSeedsAdmin() {
        AuthUseCase useCase = new AuthUseCase(new InMemoryUserRepository());
        useCase.register("jose", "secret123", "Jose");

        assertThrows(ValidationException.class, () -> useCase.register("jose", "secret123", "Jose"));
        assertEquals(Role.ADMIN, useCase.ensureRootAdmin("admin", "securepass").getRole());
    }

    @Test
    void validatesRequiredRegistrationFields() {
        AuthUseCase useCase = new AuthUseCase(new InMemoryUserRepository());

        assertThrows(ValidationException.class, () -> useCase.register(" ", "secret123", "Jose"));
        assertThrows(ValidationException.class, () -> useCase.register("jose", "short", "Jose"));
        assertThrows(ValidationException.class, () -> useCase.register("jose", "secret123", " "));
    }
}
