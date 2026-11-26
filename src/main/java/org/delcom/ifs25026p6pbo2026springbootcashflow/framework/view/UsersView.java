package org.delcom.ifs25026p6pbo2026springbootcashflow.framework.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.User;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25026p6pbo2026springbootcashflow.usecase.UserUseCase;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("Pengguna | Cashflow")
public class UsersView extends VerticalLayout implements BeforeEnterObserver {
    private final IUserRepository userRepository;
    private final UserUseCase userUseCase;
    private final Grid<User> grid = new Grid<>();

    public UsersView(UserUseCase userUseCase, IUserRepository userRepository) {
        this.userRepository = userRepository;
        this.userUseCase = userUseCase;
        grid.addColumn(User::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(User::getUsername).setHeader("Username");
        grid.addColumn(User::getFullName).setHeader("Nama lengkap");
        grid.addColumn(user -> user.getRole().name()).setHeader("Role");
        grid.addComponentColumn(user -> new Button("Lihat transaksi", event ->
                UI.getCurrent().getPage().setLocation("/user-transactions?userId=" + user.getId())));
        add(new H2("Daftar pengguna"), grid);
        setSizeFull();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        User user = AuthSession.currentUser(userRepository).orElse(null);
        if (user == null) {
            event.forwardTo(LoginView.class);
        } else if (!user.isAdmin()) {
            Notification.show("Halaman ini hanya dapat diakses administrator");
            event.forwardTo(FinanceView.class);
        } else {
            grid.setItems(userUseCase.listUsers());
        }
    }
}
