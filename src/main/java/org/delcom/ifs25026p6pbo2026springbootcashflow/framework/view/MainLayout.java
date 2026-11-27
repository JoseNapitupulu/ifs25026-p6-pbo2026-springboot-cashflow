package org.delcom.ifs25026p6pbo2026springbootcashflow.framework.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.RouterLink;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.User;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.framework.auth.AuthSession;

@PreserveOnRefresh
public class MainLayout extends AppLayout implements BeforeEnterObserver {
    private final transient IUserRepository userRepository;
    private final VerticalLayout drawer = new VerticalLayout();

    public MainLayout(IUserRepository userRepository) {
        this.userRepository = userRepository;
        setPrimarySection(Section.DRAWER);
        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), new H2("Cashflow"));
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.expand(header.getComponentAt(1));
        addToNavbar(header);

        drawer.setPadding(true);
        drawer.setSpacing(true);
        addToDrawer(drawer);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        User user = AuthSession.currentUser(userRepository).orElse(null);
        if (user == null) {
            event.forwardTo(LoginRedirectView.class);
            return;
        }
        drawer.removeAll();
        drawer.add(new Span("Halo, " + user.getFullName()));
        drawer.add(new RouterLink("Ringkasan", FinanceView.class));
        drawer.add(new RouterLink("Profil", ProfileView.class));
        if (user.isAdmin()) {
            drawer.add(new RouterLink("Daftar pengguna", UsersView.class));
        }
        Button logout = new Button("Keluar", click -> {
            AuthSession.logout();
            UI.getCurrent().getPage().setLocation("/login");
        });
        drawer.add(logout);
    }
}
