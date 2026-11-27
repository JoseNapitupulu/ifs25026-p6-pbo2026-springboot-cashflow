package org.delcom.ifs25026p6pbo2026springbootcashflow;

import jakarta.servlet.http.HttpSession;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Role;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.User;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25026p6pbo2026springbootcashflow.framework.auth.LoginController;
import org.delcom.ifs25026p6pbo2026springbootcashflow.usecase.AuthUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginControllerTests {
    @Test
    void showLoginReturnsLightweightPageAndStoresCsrfToken() {
        LoginController controller = new LoginController(mock(AuthUseCase.class));
        MockHttpServletRequest request = new MockHttpServletRequest();

        var response = controller.showLogin(request, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("<html lang=\"id\">"));
        assertTrue(response.getBody().contains("<main>"));
        assertTrue(response.getBody().contains("id=\"login-username\""));
        assertTrue(response.getBody().contains("id=\"login-password\""));
        assertTrue(response.getBody().contains("id=\"login-submit\""));
        assertNotNull(request.getSession().getAttribute("cashflow.loginCsrfToken"));
        assertTrue(response.getBody().contains("name=\"_csrf\""));
    }

    @Test
    void showLoginRemainsReachableForAuthenticatedSession() {
        LoginController controller = new LoginController(mock(AuthUseCase.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute("cashflow.currentUserId", 42);

        var response = controller.showLogin(request, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("id=\"login-submit\""));
    }

    @Test
    void loginRejectsInvalidCsrfTokenWithoutCallingAuthUseCase() {
        AuthUseCase authUseCase = mock(AuthUseCase.class);
        LoginController controller = new LoginController(authUseCase);

        var response = controller.login("user", "password", "invalid", new MockHttpServletRequest());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(authUseCase, never()).login("user", "password");
    }

    @Test
    void successfulLoginSharesServletSessionWithVaadinAuthentication() {
        AuthUseCase authUseCase = mock(AuthUseCase.class);
        IUserRepository userRepository = mock(IUserRepository.class);
        LoginController controller = new LoginController(authUseCase);
        User user = new User(42, "user", "hash", "Test User", Role.USER, null);
        when(authUseCase.login("user", "password")).thenReturn(user);
        when(userRepository.findById(42)).thenReturn(Optional.of(user));
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpSession session = request.getSession(true);
        session.setAttribute("cashflow.loginCsrfToken", "valid-token");

        var response = controller.login("user", "password", "valid-token", request);

        assertEquals(HttpStatus.SEE_OTHER, response.getStatusCode());
        assertEquals("/", response.getHeaders().getLocation().toString());
        assertEquals(42, session.getAttribute("cashflow.currentUserId"));
        assertSame(user, AuthSession.currentUser(session, userRepository).orElseThrow());
    }
}
