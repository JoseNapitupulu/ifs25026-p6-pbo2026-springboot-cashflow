package org.delcom.ifs25026p6pbo2026springbootcashflow.framework.auth;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import jakarta.servlet.http.HttpSession;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.User;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IUserRepository;

import java.util.Optional;

public final class AuthSession {
    private static final String USER_KEY = "cashflow.currentUser";
    private static final String USER_ID_KEY = "cashflow.currentUserId";
    private static final String LOGGED_OUT_KEY = "authLoggedOut";

    private AuthSession() {
    }

    public static void login(User user) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            return;
        }
        session.setAttribute(LOGGED_OUT_KEY, Boolean.FALSE);
        session.setAttribute(USER_KEY, user);
        session.setAttribute(USER_ID_KEY, user.getId());
        WrappedSession httpSession = session.getSession();
        if (httpSession != null) {
            httpSession.setAttribute(LOGGED_OUT_KEY, Boolean.FALSE);
            httpSession.setAttribute(USER_ID_KEY, user.getId());
        }
    }

    public static void login(HttpSession session, User user) {
        session.setAttribute(LOGGED_OUT_KEY, Boolean.FALSE);
        session.setAttribute(USER_ID_KEY, user.getId());
    }

    public static void logout() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            return;
        }
        session.setAttribute(USER_KEY, null);
        session.setAttribute(USER_ID_KEY, null);
        session.setAttribute(LOGGED_OUT_KEY, Boolean.TRUE);
        WrappedSession httpSession = session.getSession();
        if (httpSession != null) {
            httpSession.setAttribute(USER_ID_KEY, null);
            httpSession.setAttribute(LOGGED_OUT_KEY, Boolean.TRUE);
        }
    }

    public static Optional<User> currentUser(IUserRepository userRepository) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null || isLoggedOut(session)) {
            return Optional.empty();
        }
        Object stored = session.getAttribute(USER_ID_KEY);
        if (!(stored instanceof Integer)) {
            WrappedSession httpSession = session.getSession();
            stored = httpSession == null ? null : httpSession.getAttribute(USER_ID_KEY);
        }
        if (!(stored instanceof Integer userId)) {
            return Optional.empty();
        }
        Optional<User> user = userRepository.findById(userId);
        user.ifPresent(AuthSession::login);
        if (user.isEmpty()) {
            logout();
        }
        return user;
    }

    public static Optional<User> currentUser(HttpSession session, IUserRepository userRepository) {
        if (session == null || Boolean.TRUE.equals(session.getAttribute(LOGGED_OUT_KEY))) {
            return Optional.empty();
        }
        Object stored = session.getAttribute(USER_ID_KEY);
        if (!(stored instanceof Integer userId)) {
            return Optional.empty();
        }
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            session.removeAttribute(USER_ID_KEY);
            session.setAttribute(LOGGED_OUT_KEY, Boolean.TRUE);
        }
        return user;
    }

    private static boolean isLoggedOut(VaadinSession session) {
        WrappedSession httpSession = session.getSession();
        if (httpSession != null && httpSession.getAttribute(LOGGED_OUT_KEY) instanceof Boolean loggedOut) {
            return loggedOut;
        }
        return Boolean.TRUE.equals(session.getAttribute(LOGGED_OUT_KEY));
    }
}
