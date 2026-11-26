package org.delcom.ifs25026p6pbo2026springbootcashflow.framework.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25026p6pbo2026springbootcashflow.usecase.AuthUseCase;

@Route("register")
@PageTitle("Daftar | Cashflow")
public class RegisterView extends VerticalLayout implements BeforeEnterObserver {
    private final IUserRepository userRepository;

    public RegisterView(AuthUseCase authUseCase, IUserRepository userRepository) {
        this.userRepository = userRepository;
        TextField fullName = new TextField("Nama lengkap");
        TextField username = new TextField("Username");
        PasswordField password = new PasswordField("Password (minimal 8 karakter)");
        Button submit = new Button("Buat akun", event -> {
            try {
                AuthSession.login(authUseCase.register(username.getValue(), password.getValue(), fullName.getValue()));
                UI.getCurrent().navigate(FinanceView.class);
            } catch (DomainException ex) {
                Notification.show(ex.getMessage(), 3500, Notification.Position.TOP_CENTER);
            }
        });
        submit.setWidthFull();
        add(new H1("Daftar Cashflow"), fullName, username, password, submit,
                new Anchor("login", "Sudah punya akun? Masuk"));
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
