package org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.presenter;

import java.text.NumberFormat;
import java.util.Locale;

public class FinancePresenter {
    public String formatMoney(long amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        return "Rp " + formatter.format(amount);
    }
}
