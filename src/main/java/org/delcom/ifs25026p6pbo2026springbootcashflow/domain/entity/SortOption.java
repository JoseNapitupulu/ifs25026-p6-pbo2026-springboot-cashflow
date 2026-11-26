package org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity;

import java.util.Comparator;

public enum SortOption {
    NEWEST_FIRST(
            Comparator.comparing(Transaction::getCreatedAt)
                    .thenComparingInt(Transaction::getId).reversed(),
            "Terbaru"),
    OLDEST_FIRST(
            Comparator.comparing(Transaction::getCreatedAt)
                    .thenComparingInt(Transaction::getId),
            "Terlama"),
    AMOUNT_DESC(Comparator.comparingLong(Transaction::getAmount).reversed(), "Jumlah terbesar"),
    AMOUNT_ASC(Comparator.comparingLong(Transaction::getAmount), "Jumlah terkecil");

    private final Comparator<Transaction> comparator;
    private final String label;

    SortOption(Comparator<Transaction> comparator, String label) {
        this.comparator = comparator;
        this.label = label;
    }

    public Comparator<Transaction> comparator() { return comparator; }
    public String label() { return label; }
}
