package org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity;

public enum TransactionType {
    INCOME,
    EXPENSE;

    public String label() {
        return this == INCOME ? "Pemasukan" : "Pengeluaran";
    }
}
