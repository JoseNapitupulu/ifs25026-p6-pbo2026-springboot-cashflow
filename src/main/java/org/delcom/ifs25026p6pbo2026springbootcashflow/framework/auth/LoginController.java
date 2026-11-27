package org.delcom.ifs25026p6pbo2026springbootcashflow.framework.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.User;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.usecase.AuthUseCase;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.security.MessageDigest;
import java.util.UUID;

@Controller
public class LoginController {
    private static final String CSRF_TOKEN_KEY = "cashflow.loginCsrfToken";
    private static final String CSRF_TOKEN_PLACEHOLDER = "{{csrf}}";
    private static final String ERROR_PLACEHOLDER = "{{error}}";
    private static final String LOGIN_PAGE = """
            <!doctype html>
            <html lang="id">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <meta name="description" content="Masuk ke Cashflow untuk mencatat dan mengelola pemasukan, pengeluaran, serta saldo keuangan.">
              <link rel="icon" href="/icons/favicon.svg" type="image/svg+xml">
              <title>Masuk | Cashflow</title>
              <style>
                :root { color-scheme: light; font-family: system-ui, sans-serif; color: #0f172a; background: #f8fafc; }
                * { box-sizing: border-box; }
                body { min-height: 100vh; margin: 0; padding: 1.5rem; display: grid; place-items: center; }
                main { width: min(100%, 26.25rem); padding: 2rem; background: #fff; border: 1px solid #e2e8f0; border-radius: 0.75rem; box-shadow: 0 12px 30px rgb(15 23 42 / 8%); }
                h1 { margin: 0; font-size: 1.75rem; }
                .intro { margin: 0.5rem 0 1.75rem; color: #475569; }
                label { display: block; margin: 1rem 0 0.4rem; font-weight: 600; }
                input { width: 100%; min-height: 2.75rem; padding: 0.6rem 0.75rem; border: 1px solid #94a3b8; border-radius: 0.4rem; font: inherit; }
                input:focus-visible, button:focus-visible, a:focus-visible { outline: 3px solid #2563eb; outline-offset: 2px; }
                .password-row { display: flex; gap: 0.5rem; }
                .password-row input { flex: 1; min-width: 0; }
                .password-row button { padding: 0 0.75rem; border: 1px solid #64748b; border-radius: 0.4rem; background: #fff; color: #0f172a; font: inherit; }
                .submit { width: 100%; min-height: 2.75rem; margin-top: 1.25rem; border: 0; border-radius: 0.4rem; background: #1d4ed8; color: #fff; font: inherit; font-weight: 700; cursor: pointer; }
                .register { display: inline-block; margin-top: 1rem; color: #1d4ed8; }
                .error { margin: 0 0 1rem; padding: 0.75rem; border-radius: 0.4rem; background: #fef2f2; color: #991b1b; }
              </style>
            </head>
            <body>
              <main>
                <h1>Cashflow</h1>
                <p class="intro">Catat pemasukan dan pengeluaran dengan mudah.</p>
                {{error}}
                <form method="post" action="/login">
                  <input type="hidden" name="_csrf" value="{{csrf}}">
                  <label for="username">Username</label>
                  <input id="username" name="username" type="text" autocomplete="username" required>
                  <label for="password">Password</label>
                  <div class="password-row">
                    <input id="password" name="password" type="password" autocomplete="current-password" required>
                    <button id="toggle-password" type="button" aria-controls="password" aria-pressed="false">Tampilkan</button>
                  </div>
                  <button class="submit" type="submit">Masuk</button>
                </form>
                <a class="register" href="/register">Belum punya akun? Daftar</a>
              </main>
              <script>
                const password = document.getElementById('password');
                const toggle = document.getElementById('toggle-password');
                toggle.addEventListener('click', () => {
                  const visible = password.type === 'text';
                  password.type = visible ? 'password' : 'text';
                  toggle.textContent = visible ? 'Tampilkan' : 'Sembunyikan';
                  toggle.setAttribute('aria-pressed', String(!visible));
                });
              </script>
            </body>
            </html>
            """;

    private static final String LOGIN_ERROR = """
            <p class="error" role="alert">Username atau password salah. Periksa kembali lalu coba lagi.</p>
            """;
    private static final String INVALID_SESSION = """
            <!doctype html>
            <html lang="id">
            <head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Sesi berakhir | Cashflow</title></head>
            <body><main><h1>Sesi berakhir</h1><p>Muat ulang halaman masuk lalu coba lagi.</p><a href="/login">Kembali ke halaman masuk</a></main></body>
            </html>
            """;

    private final AuthUseCase authUseCase;
    private final IUserRepository userRepository;

    public LoginController(AuthUseCase authUseCase, IUserRepository userRepository) {
        this.authUseCase = authUseCase;
        this.userRepository = userRepository;
    }

    @GetMapping(value = "/login", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> showLogin(
            HttpServletRequest request,
            @RequestParam(name = "error", required = false) String error
    ) {
        if (AuthSession.currentUser(request.getSession(false), userRepository).isPresent()) {
            return redirect("/");
        }
        String csrfToken = UUID.randomUUID().toString();
        request.getSession(true).setAttribute(CSRF_TOKEN_KEY, csrfToken);
        String errorMarkup = "1".equals(error) ? LOGIN_ERROR : "";
        String page = LOGIN_PAGE
                .replace(CSRF_TOKEN_PLACEHOLDER, csrfToken)
                .replace(ERROR_PLACEHOLDER, errorMarkup);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .cacheControl(CacheControl.noStore())
                .body(page);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(name = "_csrf", required = false) String submittedToken,
            HttpServletRequest request
    ) {
        HttpSession session = request.getSession(false);
        Object expectedToken = session == null ? null : session.getAttribute(CSRF_TOKEN_KEY);
        if (!(expectedToken instanceof String token) || submittedToken == null
                || !MessageDigest.isEqual(token.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                submittedToken.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.TEXT_HTML)
                    .cacheControl(CacheControl.noStore())
                    .body(INVALID_SESSION);
        }

        session.removeAttribute(CSRF_TOKEN_KEY);
        try {
            User user = authUseCase.login(username, password);
            request.changeSessionId();
            AuthSession.login(session, user);
            return redirect("/");
        } catch (DomainException ex) {
            return redirect("/login?error=1");
        }
    }

    private static ResponseEntity<String> redirect(String location) {
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .header(HttpHeaders.LOCATION, URI.create(location).toString())
                .cacheControl(CacheControl.noStore())
                .build();
    }
}
