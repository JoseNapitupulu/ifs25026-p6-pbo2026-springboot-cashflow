package org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

public class FileStorage {
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private final Path root;

    public FileStorage(String uploadDir) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new UncheckedIOException("Tidak dapat membuat direktori upload", ex);
        }
    }

    public Path root() {
        return root;
    }

    public String save(InputStream content, String contentType, long size) {
        if (content == null || size <= 0 || size > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Ukuran gambar harus antara 1 byte dan 5 MB");
        }
        String extension = switch (contentType == null ? "" : contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new IllegalArgumentException("Format bukti harus JPEG, PNG, atau WebP");
        };
        String fileName = UUID.randomUUID() + extension;
        try (InputStream input = content) {
            Path target = root.resolve(fileName).normalize();
            byte[] data = input.readNBytes((int) MAX_FILE_SIZE + 1);
            if (data.length == 0 || data.length > MAX_FILE_SIZE || data.length != size) {
                throw new IllegalArgumentException("Ukuran gambar tidak valid");
            }
            Files.write(target, data);
            return fileName;
        } catch (IOException ex) {
            throw new UncheckedIOException("Gagal menyimpan gambar", ex);
        }
    }

    public void delete(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        Path target = root.resolve(fileName).normalize();
        if (!target.startsWith(root) || target.equals(root)) {
            throw new IllegalArgumentException("Path file tidak valid");
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new UncheckedIOException("Gagal menghapus gambar", ex);
        }
    }
}
