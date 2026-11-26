package org.delcom.ifs25026p6pbo2026springbootcashflow.framework.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.User;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25026p6pbo2026springbootcashflow.usecase.AuthUseCase;

@Route("login")
@PageTitle("Masuk | Cashflow")
public class LoginView extends VerticalLayout implements BeforeEnterObserver {
    private final transient IUserRepository userRepository;

    public LoginView(AuthUseCase authUseCase, IUserRepository userRepository) {
        this.userRepository = userRepository;
        TextField username = new TextField("Username");
        username.setId("login-username");
        username.getElement().setAttribute("autocomplete", "username");
        username.setWidthFull();

        PasswordField password = new PasswordField("Password");
        password.setId("login-password");
        password.getElement().setAttribute("autocomplete", "current-password");
        password.setWidthFull();

        Button submit = new Button("Masuk", event -> {
            try {
                User user = authUseCase.login(username.getValue(), password.getValue());
                AuthSession.login(user);
                UI.getCurrent().navigate(FinanceView.class);
            } catch (DomainException ex) {
                Notification.show(ex.getMessage(), 3500, Notification.Position.TOP_CENTER);
            }
        });
        submit.setId("login-submit");
        submit.setWidthFull();

        Anchor register = new Anchor("register", "Belum punya akun? Daftar");
        add(new H1("Cashflow"), new Paragraph("Catat pemasukan dan pengeluaran dengan mudah."),
                username, password, submit, register);
        setSizeFull();
        setMaxWidth("420px");
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.STRETCH);
        getStyle().set("margin", "auto");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (AuthSession.currentUser(userRepository).isPresent()) {
            event.forwardTo(FinanceView.class);
        }
    }
}
