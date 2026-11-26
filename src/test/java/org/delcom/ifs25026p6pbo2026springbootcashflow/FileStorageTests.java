package org.delcom.ifs25026p6pbo2026springbootcashflow;

import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.storage.FileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageTests {
    @TempDir
    Path tempDir;

    @Test
    void savesOnlySupportedImageTypesWithinStorageRoot() throws Exception {
        FileStorage storage = new FileStorage(tempDir.resolve("uploads").toString());
        String fileName = storage.save(new ByteArrayInputStream(new byte[]{1, 2, 3}), "image/png", 3);

        assertTrue(fileName.endsWith(".png"));
        assertEquals(3, Files.size(storage.root().resolve(fileName)));
        assertTrue(storage.save(new ByteArrayInputStream(new byte[]{1}), "image/JPEG", 1).endsWith(".jpg"));
        assertTrue(storage.save(new ByteArrayInputStream(new byte[]{1}), "image/webp", 1).endsWith(".webp"));
        storage.delete(fileName);
        assertTrue(Files.notExists(storage.root().resolve(fileName)));
        storage.delete(fileName);
        storage.delete(null);
        storage.delete(" ");
    }

    @Test
    void rejectsUnsupportedContentTypeAndOversizedInput() {
        FileStorage storage = new FileStorage(tempDir.resolve("uploads").toString());

        assertThrows(IllegalArgumentException.class,
                () -> storage.save(new ByteArrayInputStream(new byte[]{1}), "image/svg+xml", 1));
        assertThrows(IllegalArgumentException.class,
                () -> storage.save(new ByteArrayInputStream(new byte[]{1}), "image/png", 6 * 1024 * 1024));
        assertThrows(IllegalArgumentException.class,
                () -> storage.save(new ByteArrayInputStream(new byte[]{1, 2}), "image/png", 1));
        assertThrows(IllegalArgumentException.class,
                () -> storage.save(new ByteArrayInputStream(new byte[0]), "image/png", 1));
        assertThrows(IllegalArgumentException.class,
                () -> storage.save(new ByteArrayInputStream(new byte[5 * 1024 * 1024 + 1]),
                        "image/png", 1));
        assertThrows(IllegalArgumentException.class,
                () -> storage.save(new ByteArrayInputStream(new byte[]{1}), null, 1));
        assertThrows(IllegalArgumentException.class, () -> storage.save(null, "image/png", 1));
        assertThrows(IllegalArgumentException.class,
                () -> storage.save(new ByteArrayInputStream(new byte[]{1}), "image/png", 0));
        assertThrows(IllegalArgumentException.class, () -> storage.delete("../outside.png"));
        assertThrows(IllegalArgumentException.class, () -> storage.delete("."));
    }

    @Test
    void reportsFilesystemFailuresAndInputErrors() throws Exception {
        FileStorage storage = new FileStorage(tempDir.resolve("uploads").toString());
        assertThrows(java.io.UncheckedIOException.class, () -> storage.save(
                new java.io.InputStream() {
                    @Override
                    public int read() throws java.io.IOException {
                        throw new java.io.IOException("read failed");
                    }
                }, "image/png", 1));

        Files.createDirectories(storage.root().resolve("folder"));
        Files.write(storage.root().resolve("folder").resolve("child"), new byte[]{1});
        assertThrows(java.io.UncheckedIOException.class, () -> storage.delete("folder"));

        Path occupied = tempDir.resolve("occupied");
        Files.write(occupied, new byte[]{1});
        assertThrows(java.io.UncheckedIOException.class,
                () -> new FileStorage(occupied.resolve("child").toString()));
    }
}
