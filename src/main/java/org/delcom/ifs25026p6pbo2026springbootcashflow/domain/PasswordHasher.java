package org.delcom.ifs25026p6pbo2026springbootcashflow.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class PasswordHasher {
    private PasswordHasher() {
    }

    public static String hash(String rawPassword) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 tidak tersedia", ex);
        }
    }

    public static boolean matches(String rawPassword, String passwordHash) {
        return MessageDigest.isEqual(
                hash(rawPassword).getBytes(StandardCharsets.US_ASCII),
                passwordHash.getBytes(StandardCharsets.US_ASCII)
        );
    }
}
