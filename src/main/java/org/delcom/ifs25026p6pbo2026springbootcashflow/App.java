package org.delcom.ifs25026p6pbo2026springbootcashflow;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.lumo.Lumo;

@StyleSheet(Lumo.STYLESHEET)
@StyleSheet("styles.css")
@PWA(name = "Cashflow", shortName = "Cashflow", iconPath = "icons/icon.png")
public class App implements AppShellConfigurator {
    @Override
    public void configurePage(AppShellSettings settings) {
        settings.addMetaTag("description",
                "Masuk ke Cashflow untuk mencatat dan mengelola pemasukan, pengeluaran, serta saldo keuangan.");
    }
}
