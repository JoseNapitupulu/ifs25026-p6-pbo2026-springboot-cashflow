package org.delcom.ifs25026p6pbo2026springbootcashflow;

import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.presenter.FinancePresenter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinancePresenterTests {
    @Test
    void formatsRupiahWithThousandsSeparatorAndTwoDecimals() {
        assertEquals("Rp 1,000,000.00", new FinancePresenter().formatMoney(1_000_000));
    }
}
