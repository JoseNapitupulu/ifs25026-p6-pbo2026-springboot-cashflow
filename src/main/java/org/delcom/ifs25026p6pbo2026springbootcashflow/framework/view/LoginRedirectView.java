package org.delcom.ifs25026p6pbo2026springbootcashflow.framework.view;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("login-redirect")
public class LoginRedirectView extends Div {
    public LoginRedirectView() {
        add(new Span("Mengarahkan ke halaman masuk..."));
        getElement().executeJs("window.location.replace('/login')");
    }
}
