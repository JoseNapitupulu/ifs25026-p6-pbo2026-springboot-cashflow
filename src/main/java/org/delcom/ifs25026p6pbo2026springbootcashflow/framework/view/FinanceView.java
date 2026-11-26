package org.delcom.ifs25026p6pbo2026springbootcashflow.framework.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.User;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25026p6pbo2026springbootcashflow.usecase.FinanceAiUseCase;
import org.delcom.ifs25026p6pbo2026springbootcashflow.usecase.FinanceUseCase;

import java.util.List;
import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Ringkasan | Cashflow")
public class FinanceView extends VerticalLayout {
    private static final String DESCRIPTION_LABEL = "Keterangan";
    private static final String TRANSACTION_PROOF_LABEL = "Bukti transaksi";
    private final transient FinanceUseCase financeUseCase;
    private final transient FinanceAiUseCase financeAiUseCase;
    private final transient FinancePresenter presenter;
    private final transient FileStorage fileStorage;
    private final transient User currentUser;
    private final Grid<Transaction> grid = new Grid<>(Transaction.class, false);
    private final TextField search = new TextField("Cari " + DESCRIPTION_LABEL.toLowerCase());
    private final ComboBox<TransactionTypeFilter> typeFilter = new ComboBox<>("Tipe");
    private final ComboBox<SortOption> sortOption = new ComboBox<>("Urutkan");
    private final Span incomeValue = new Span();
    private final Span expenseValue = new Span();
    private final Span balanceValue = new Span();
    private final Span transactionCount = new Span();

    public FinanceView(
            FinanceUseCase financeUseCase,
            FinanceAiUseCase financeAiUseCase,
            FinancePresenter presenter,
            FileStorage fileStorage,
            IUserRepository userRepository
    ) {
        this.financeUseCase = financeUseCase;
        this.financeAiUseCase = financeAiUseCase;
        this.presenter = presenter;
        this.fileStorage = fileStorage;
        this.currentUser = AuthSession.currentUser(userRepository)
                .orElseThrow(() -> new IllegalStateException("Sesi login tidak ditemukan"));
        setSizeFull();
        setPadding(true);
        add(new H2("Ringkasan keuangan"), summaryCards(), toolbar(), buildGrid());
        expand(grid);
        refresh();
    }

    private HorizontalLayout summaryCards() {
        HorizontalLayout cards = new HorizontalLayout(
                summaryCard("Pemasukan", incomeValue),
                summaryCard("Pengeluaran", expenseValue),
                summaryCard("Saldo", balanceValue),
                summaryCard("Transaksi", transactionCount)
        );
        cards.setWidthFull();
        cards.setFlexGrow(1);
        return cards;
    }

    private VerticalLayout summaryCard(String label, Span value) {
        Span title = new Span(label);
        title.getStyle().set("color", "#64748b").set("font-size", "0.9rem");
        value.getStyle().set("font-size", "1.25rem").set("font-weight", "700");
        VerticalLayout card = new VerticalLayout(title, value);
        card.setPadding(true);
        card.setSpacing(false);
        card.getStyle().set("background", "white").set("border-radius", "10px")
                .set("border", "1px solid #e2e8f0");
        return card;
    }

    private HorizontalLayout toolbar() {
        search.setValueChangeMode(ValueChangeMode.EAGER);
        search.addValueChangeListener(event -> refresh());
        typeFilter.setItems(TransactionTypeFilter.values());
        typeFilter.setItemLabelGenerator(TransactionTypeFilter::label);
        typeFilter.setValue(TransactionTypeFilter.ALL);
        typeFilter.addValueChangeListener(event -> refresh());
        sortOption.setItems(SortOption.values());
        sortOption.setItemLabelGenerator(SortOption::label);
        sortOption.setValue(SortOption.NEWEST_FIRST);
        sortOption.addValueChangeListener(event -> refresh());
        Button income = new Button("Tambah pemasukan", event -> openTransactionDialog(TransactionType.INCOME));
        Button expense = new Button("Tambah pengeluaran", event -> openTransactionDialog(TransactionType.EXPENSE));
        Button ai = new Button("Generate AI", event -> openAiDialog());
        Button reload = new Button("Muat ulang", event -> refresh());
        HorizontalLayout layout = new HorizontalLayout(search, typeFilter, sortOption, reload, ai, income, expense);
        layout.setWidthFull();
        layout.setAlignItems(Alignment.END);
        return layout;
    }

    private Grid<Transaction> buildGrid() {
        grid.addColumn(Transaction::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(Transaction::getDescription).setHeader(DESCRIPTION_LABEL).setFlexGrow(1);
        grid.addColumn(transaction -> presenter.formatMoney(transaction.getAmount()))
                .setHeader("Jumlah").setAutoWidth(true);
        grid.addColumn(transaction -> transaction.getType().label()).setHeader("Tipe").setAutoWidth(true);
        grid.addComponentColumn(transaction -> {
            if (transaction.getProofPath() == null || transaction.getProofPath().isBlank()) {
                return new Span("—");
            }
            Image image = new Image("/uploads/" + transaction.getProofPath(), TRANSACTION_PROOF_LABEL);
            image.setWidth("48px");
            image.setHeight("48px");
            image.getStyle().set("object-fit", "cover").set("cursor", "zoom-in");
            image.addClickListener(event -> showProof(transaction.getProofPath()));
            return image;
        }).setHeader("Bukti").setAutoWidth(true);
        grid.addColumn(transaction -> transaction.getCreatedAt().toString().replace('T', ' '))
                .setHeader("Waktu").setAutoWidth(true);
        grid.addComponentColumn(transaction -> new Button("Hapus", event -> confirmDelete(transaction)))
                .setHeader("Tindakan").setAutoWidth(true);
        grid.setSizeFull();
        return grid;
    }

    private void refresh() {
        FinanceUseCase.Balance summary = financeUseCase.getBalance(currentUser.getId());
        incomeValue.setText(presenter.formatMoney(summary.income()));
        expenseValue.setText(presenter.formatMoney(summary.expense()));
        balanceValue.setText(presenter.formatMoney(summary.balance()));
        transactionCount.setText(Long.toString(summary.totalTransactions()));
        grid.setItems(financeUseCase.listTransactions(
                currentUser.getId(), search.getValue(), typeFilter.getValue(), sortOption.getValue()));
    }

    private void openTransactionDialog(TransactionType type) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(type == TransactionType.INCOME ? "Tambah pemasukan" : "Tambah pengeluaran");
        TextField description = new TextField(DESCRIPTION_LABEL);
        description.setWidthFull();
        TextField amount = new TextField("Jumlah (rupiah)");
        amount.setPlaceholder("Contoh: 150000");
        ProofUploadState proofUpload = new ProofUploadState();
        Upload upload = new Upload(UploadHandler.inMemory((metadata, data) ->
                proofUpload.set(data, metadata.contentType(), metadata.contentLength())));
        upload.setAcceptedMimeTypes("image/png", "image/jpeg", "image/webp");
        upload.setMaxFiles(1);
        upload.setMaxFileSize(5 * 1024 * 1024);
        upload.setDropLabel(new Span("Unggah bukti (opsional, maks. 5 MB)"));
        Button save = new Button("Simpan", event -> {
            try {
                long parsedAmount = Long.parseLong(amount.getValue().trim().replace(",", ""));
                if (description.getValue() == null || description.getValue().isBlank()) {
                    throw new IllegalArgumentException("Keterangan transaksi tidak boleh kosong");
                }
                if (parsedAmount <= 0) {
                    throw new IllegalArgumentException("Jumlah harus lebih besar dari nol");
                }
                String path = proofUpload.contentType() == null
                        ? null
                    : fileStorage.save(new ByteArrayInputStream(proofUpload.data()),
                        proofUpload.contentType(), proofUpload.size());
                financeUseCase.addTransaction(currentUser.getId(), description.getValue(),
                    parsedAmount, type, path);
                dialog.close();
                refresh();
                Notification.show("Transaksi berhasil disimpan");
            } catch (NumberFormatException ex) {
                Notification.show("Jumlah harus berupa bilangan bulat rupiah", 3000,
                        Notification.Position.TOP_CENTER);
            } catch (DomainException | IllegalArgumentException ex) {
                Notification.show(ex.getMessage(), 3500, Notification.Position.TOP_CENTER);
            }
        });
        Button cancel = new Button("Batal", event -> dialog.close());
        dialog.add(new VerticalLayout(description, amount, upload));
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void confirmDelete(Transaction transaction) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Hapus transaksi?");
        dialog.add("Transaksi \"" + transaction.getDescription() + "\" akan dihapus permanen.");
        Button cancel = new Button("Batal", event -> dialog.close());
        Button delete = new Button("Hapus", event -> {
            try {
                financeUseCase.removeTransactionForUser(currentUser.getId(), transaction.getId());
                fileStorage.delete(transaction.getProofPath());
                dialog.close();
                refresh();
                Notification.show("Transaksi dihapus");
            } catch (DomainException ex) {
                Notification.show(ex.getMessage(), 3500, Notification.Position.TOP_CENTER);
            }
        });
        dialog.getFooter().add(cancel, delete);
        dialog.open();
    }

    private void showProof(String path) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(TRANSACTION_PROOF_LABEL);
        Image image = new Image("/uploads/" + path, TRANSACTION_PROOF_LABEL);
        image.setMaxWidth("80vw");
        image.setMaxHeight("70vh");
        dialog.add(image);
        dialog.getFooter().add(new Button("Tutup", event -> dialog.close()));
        dialog.open();
    }

    private static final class ProofUploadState implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private byte[] data;
        private String contentType;
        private long size;

        void set(byte[] data, String contentType, long size) {
            this.data = data;
            this.contentType = contentType;
            this.size = size;
        }

        byte[] data() {
            return data;
        }

        String contentType() {
            return contentType;
        }

        long size() {
            return size;
        }
    }

    private void openAiDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Buat transaksi dengan AI");
        TextArea prompt = new TextArea("Catatan keuangan");
        prompt.setPlaceholder("Contoh: hari ini menerima gaji Rp 5.000.000 dan belanja Rp 250.000");
        prompt.setWidth("min(640px, 80vw)");
        ProgressBar progress = new ProgressBar();
        progress.setIndeterminate(true);
        progress.setVisible(false);
        Grid<AiGeneratedTransaction> preview = new Grid<>();
        preview.addColumn(AiGeneratedTransaction::description).setHeader(DESCRIPTION_LABEL);
        preview.addColumn(item -> presenter.formatMoney(item.amount())).setHeader("Jumlah");
        preview.addColumn(item -> item.type().label()).setHeader("Tipe");
        preview.setSelectionMode(Grid.SelectionMode.MULTI);
        preview.setVisible(false);
        Button generate = new Button("Proses AI");
        Button save = new Button("Simpan pilihan");
        save.setEnabled(false);
        generate.addClickListener(event -> generateAiTransactions(prompt, generate, progress, preview, save));
        save.addClickListener(event -> saveGeneratedTransactions(dialog, preview));
        Button cancel = new Button("Tutup", event -> dialog.close());
        dialog.add(new VerticalLayout(prompt, generate, progress, preview));
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void generateAiTransactions(
            TextArea prompt,
            Button generate,
            ProgressBar progress,
            Grid<AiGeneratedTransaction> preview,
            Button save
    ) {
        if (prompt.getValue() == null || prompt.getValue().isBlank()) {
            Notification.show("Catatan keuangan tidak boleh kosong");
            return;
        }
        generate.setEnabled(false);
        progress.setVisible(true);
        UI ui = UI.getCurrent();
        CompletableFuture.supplyAsync(() -> financeAiUseCase.generateTransactions(prompt.getValue()))
                .whenComplete((items, error) -> ui.access(() ->
                        finishAiGeneration(items, error, generate, progress, preview, save)));
    }

    private void finishAiGeneration(
            List<AiGeneratedTransaction> items,
            Throwable error,
            Button generate,
            ProgressBar progress,
            Grid<AiGeneratedTransaction> preview,
            Button save
    ) {
        generate.setEnabled(true);
        progress.setVisible(false);
        if (error != null) {
            Notification.show(aiErrorMessage(error), 5000, Notification.Position.TOP_CENTER);
            return;
        }
        preview.setItems(items);
        preview.setVisible(true);
        save.setEnabled(true);
    }

    private String aiErrorMessage(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null
                ? error.getCause() : error;
        return cause.getMessage() == null ? "Gagal memproses permintaan AI" : cause.getMessage();
    }

    private void saveGeneratedTransactions(Dialog dialog, Grid<AiGeneratedTransaction> preview) {
        try {
            List<AiGeneratedTransaction> selected = List.copyOf(preview.getSelectedItems());
            financeAiUseCase.saveGenerated(currentUser.getId(), selected);
            dialog.close();
            refresh();
            Notification.show("Transaksi AI berhasil disimpan");
        } catch (DomainException ex) {
            Notification.show(ex.getMessage(), 3500, Notification.Position.TOP_CENTER);
        }
    }
}
