package org.delcom.ifs25026p6pbo2026springbootcashflow;

import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.repository.InMemoryUserRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.PasswordHasher;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Role;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25026p6pbo2026springbootcashflow.usecase.AuthUseCase;
import org.junit.jupiter.api.Test;

import java.security.Provider;
import java.security.Security;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        assertThrows(ValidationException.class, () -> useCase.register(null, "secret123", "Jose"));
        assertThrows(ValidationException.class, () -> useCase.register("jose", "short", "Jose"));
        assertThrows(ValidationException.class, () -> useCase.register("jose", null, "Jose"));
        assertThrows(ValidationException.class, () -> useCase.register("jose", "secret123", " "));
        assertThrows(ValidationException.class, () -> useCase.register("jose", "secret123", null));
    }

    @Test
    void validatesLoginInputsAndRootAdminConfiguration() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        AuthUseCase useCase = new AuthUseCase(repository);

        assertThrows(ValidationException.class, () -> useCase.login(null, "password"));
        assertThrows(ValidationException.class, () -> useCase.login(" ", "password"));
        assertThrows(ValidationException.class, () -> useCase.login("user", null));
        assertThrows(ValidationException.class, () -> useCase.login("user", ""));
        assertThrows(ValidationException.class, () -> useCase.login("missing", "password"));
        assertThrows(ValidationException.class, () -> useCase.ensureRootAdmin(null, "password"));
        assertThrows(ValidationException.class, () -> useCase.ensureRootAdmin(" ", "password"));
        assertThrows(ValidationException.class, () -> useCase.ensureRootAdmin("admin", null));
        assertThrows(ValidationException.class, () -> useCase.ensureRootAdmin("admin", " "));

        var admin = useCase.ensureRootAdmin("admin", "securepass");
        assertEquals(admin.getId(), useCase.ensureRootAdmin("admin", "different").getId());
    }

    @Test
    void passwordHasherReportsUnavailableSha256Provider() {
        Provider[] providers = Security.getProviders("MessageDigest.SHA-256");
        Provider[] allProviders = Security.getProviders();
        try {
            for (Provider provider : providers) {
                Security.removeProvider(provider.getName());
            }
            assertThrows(IllegalStateException.class, () -> PasswordHasher.hash("password"));
        } finally {
            for (Provider provider : providers) {
                Security.insertProviderAt(provider, Arrays.asList(allProviders).indexOf(provider) + 1);
            }
        }
    }

    @Test
    void privatePasswordHasherConstructorCannotBeUsed() throws Exception {
        var constructor = PasswordHasher.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }
}
