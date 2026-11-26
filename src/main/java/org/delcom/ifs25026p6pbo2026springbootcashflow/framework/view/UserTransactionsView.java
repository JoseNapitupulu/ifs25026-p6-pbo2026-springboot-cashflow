package org.delcom.ifs25026p6pbo2026springbootcashflow.framework.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.User;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25026p6pbo2026springbootcashflow.usecase.FinanceUseCase;
import org.delcom.ifs25026p6pbo2026springbootcashflow.usecase.UserUseCase;

import java.util.List;

@Route(value = "user-transactions", layout = MainLayout.class)
@PageTitle("Transaksi pengguna | Cashflow")
public class UserTransactionsView extends VerticalLayout implements BeforeEnterObserver {
    private final transient IUserRepository userRepository;
    private final transient FinanceUseCase financeUseCase;
    private final transient UserUseCase userUseCase;
    private final Grid<Transaction> grid = new Grid<>();

    public UserTransactionsView(
            FinanceUseCase financeUseCase,
            FinancePresenter presenter,
            UserUseCase userUseCase,
            IUserRepository userRepository
    ) {
        this.userRepository = userRepository;
        this.financeUseCase = financeUseCase;
        this.userUseCase = userUseCase;
        add(new H2("Transaksi pengguna"));
        grid.addColumn(Transaction::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(Transaction::getDescription).setHeader("Keterangan");
        grid.addColumn(transaction -> presenter.formatMoney(transaction.getAmount())).setHeader("Jumlah");
        grid.addColumn(transaction -> transaction.getType().label()).setHeader("Tipe");
        grid.addColumn(transaction -> transaction.getCreatedAt().toString().replace('T', ' '))
                .setHeader("Waktu");
        grid.addComponentColumn(transaction -> transaction.getProofPath() == null
                ? new Span("—")
                : new com.vaadin.flow.component.html.Anchor(
                        "/uploads/" + transaction.getProofPath(), "Lihat bukti"));
        add(grid);
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
            List<String> userIds = event.getLocation().getQueryParameters().getParameters().get("userId");
            if (userIds == null || userIds.isEmpty()) {
                Notification.show("Pilih pengguna yang ingin ditinjau");
                event.forwardTo(UsersView.class);
                return;
            }
            try {
                User selectedUser = userUseCase.getUser(Integer.parseInt(userIds.get(0)));
                grid.setItems(financeUseCase.listTransactions(selectedUser.getId(), null, null, null));
                addComponentAsFirst(new Span(
                        "Pengguna: " + selectedUser.getFullName() + " (" + selectedUser.getUsername() + ")"));
            } catch (NumberFormatException ex) {
                Notification.show("ID pengguna tidak valid");
                event.forwardTo(UsersView.class);
            } catch (DomainException ex) {
                Notification.show(ex.getMessage());
                event.forwardTo(UsersView.class);
            }
        }
    }
}
