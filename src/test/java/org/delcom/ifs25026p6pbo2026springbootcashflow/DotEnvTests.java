package org.delcom.ifs25026p6pbo2026springbootcashflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DotEnvTests {
    @TempDir
    Path tempDir;

    @Test
    void loadsEnvLinesAndPreservesExistingSystemProperties() throws Exception {
        String quotedKey = "CASHFLOW_TEST_QUOTED_" + UUID.randomUUID().toString().replace("-", "");
        String plainKey = "CASHFLOW_TEST_PLAIN_" + UUID.randomUUID().toString().replace("-", "");
        String existingKey = "CASHFLOW_TEST_EXISTING_" + UUID.randomUUID().toString().replace("-", "");
        Path envFile = tempDir.resolve(".env");
        try {
            System.setProperty(existingKey, "existing");
            Files.writeString(envFile, String.join(System.lineSeparator(),
                    "# comment",
                    "",
                    "invalid-line",
                    " =missing-key",
                    quotedKey + " = \"quoted value\"",
                    plainKey + " = unquoted value ",
                    existingKey + " = ignored"));

            DotEnv.load(envFile);

            assertEquals("quoted value", System.getProperty(quotedKey));
            assertEquals("unquoted value", System.getProperty(plainKey));
            assertEquals("existing", System.getProperty(existingKey));
        } finally {
            System.clearProperty(quotedKey);
            System.clearProperty(plainKey);
            System.clearProperty(existingKey);
        }
    }

    @Test
    void ignoresMissingFilesAndSkipsInvalidOrAlreadyDefinedValues() {
        String key = "CASHFLOW_TEST_LINE_" + UUID.randomUUID().toString().replace("-", "");
        try {
            DotEnv.load(tempDir.resolve("missing.env"));
            DotEnv.applyLine(" ");
            DotEnv.applyLine("# comment");
            DotEnv.applyLine("invalid");
            DotEnv.applyLine("=missing-key");
            DotEnv.applyLine(key + "= 'value'");
            assertEquals("value", System.getProperty(key));
            DotEnv.applyLine(key + "=overwritten");
            assertEquals("value", System.getProperty(key));

            for (String value : java.util.List.of("", "x", "\"open", "close\"", "'open", "close'")) {
                System.clearProperty(key);
                DotEnv.applyLine(key + "=" + value);
                assertEquals(value, System.getProperty(key));
            }

            String environmentKey = "PATH";
            String originalPathProperty = System.getProperty(environmentKey);
            DotEnv.applyLine(environmentKey + "=should-not-replace-environment");
            assertEquals(originalPathProperty, System.getProperty(environmentKey));
        } catch (java.io.IOException ex) {
            throw new AssertionError(ex);
        } finally {
            System.clearProperty(key);
        }
        assertNull(System.getProperty(key));
    }

    @Test
    void defaultLoaderDoesNotOverrideExplicitConfiguration() throws Exception {
        Map<String, String> original = new LinkedHashMap<>();
        for (String key : ListOfEnvKeys.VALUES) {
            original.put(key, System.getProperty(key));
            System.setProperty(key, "explicit");
        }
        try {
            DotEnv.load();
            for (String key : ListOfEnvKeys.VALUES) {
                assertEquals("explicit", System.getProperty(key));
            }
        } finally {
            original.forEach((key, value) -> {
                if (value == null) {
                    System.clearProperty(key);
                } else {
                    System.setProperty(key, value);
                }
            });
        }
    }

    @Test
    void privateConstructorCanBeInvoked() throws Exception {
        var constructor = DotEnv.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        org.junit.jupiter.api.Assertions.assertNotNull(constructor.newInstance());
    }

    private static final class ListOfEnvKeys {
        private static final java.util.List<String> VALUES = java.util.List.of(
                "APP_PORT", "DB_CONNECTION", "DB_HOST", "DB_PORT", "DB_DATABASE",
                "DB_USERNAME", "DB_PASSWORD", "ROOT_USERNAME", "ROOT_PASSWORD",
                "UPLOAD_DIR", "OPENAI_ENDPOINT", "OPENAI_KEY", "OPENAI_MODEL");
    }
}
