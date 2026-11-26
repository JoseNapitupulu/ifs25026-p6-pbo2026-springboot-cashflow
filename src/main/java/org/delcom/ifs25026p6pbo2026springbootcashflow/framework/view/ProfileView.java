package org.delcom.ifs25026p6pbo2026springbootcashflow.framework.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.User;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25026p6pbo2026springbootcashflow.usecase.UserUseCase;

import java.io.ByteArrayInputStream;

@Route(value = "profile", layout = MainLayout.class)
@PageTitle("Profil | Cashflow")
public class ProfileView extends VerticalLayout {
    public ProfileView(UserUseCase userUseCase, IUserRepository userRepository, FileStorage fileStorage) {
        User user = AuthSession.currentUser(userRepository)
                .orElseThrow(() -> new IllegalStateException("Sesi login tidak ditemukan"));
        TextField fullName = new TextField("Nama lengkap", user.getFullName());
        TextField username = new TextField("Username", user.getUsername());
        username.setReadOnly(true);
        PasswordField password = new PasswordField("Password baru (opsional)");
        password.setHelperText("Kosongkan jika tidak ingin mengganti password.");

        final byte[][] content = {null};
        final String[] contentType = {null};
        final long[] contentLength = {0};
        Upload photo = new Upload(UploadHandler.inMemory((metadata, data) -> {
            content[0] = data;
            contentType[0] = metadata.contentType();
            contentLength[0] = metadata.contentLength();
        }));
        photo.setAcceptedMimeTypes("image/png", "image/jpeg", "image/webp");
        photo.setMaxFiles(1);
        photo.setMaxFileSize(5 * 1024 * 1024);

        Button save = new Button("Simpan profil", event -> {
            try {
                String photoPath = contentType[0] == null ? null
                        : fileStorage.save(new ByteArrayInputStream(content[0]), contentType[0], contentLength[0]);
                userUseCase.updateProfile(user.getId(), fullName.getValue(), password.getValue(), photoPath);
                Notification.show("Profil berhasil diperbarui");
            } catch (DomainException | IllegalArgumentException ex) {
                Notification.show(ex.getMessage(), 3500, Notification.Position.TOP_CENTER);
            }
        });

        add(new H2("Profil pengguna"), fullName, username, password, photo, save);
        setMaxWidth("640px");
        setPadding(true);
    }
}
