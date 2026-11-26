package org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity;

public enum TransactionTypeFilter {
    ALL,
    INCOME,
    EXPENSE;

    public boolean matches(Transaction transaction) {
        return this == ALL || transaction.getType().name().equals(name());
    }

    public String label() {
        return switch (this) {
            case ALL -> "Semua tipe";
            case INCOME -> "Pemasukan";
            case EXPENSE -> "Pengeluaran";
        };
    }
}
