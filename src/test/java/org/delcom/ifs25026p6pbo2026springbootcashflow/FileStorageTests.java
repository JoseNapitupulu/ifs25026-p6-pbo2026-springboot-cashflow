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
    }
}
