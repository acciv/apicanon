package dev.acciv.apicanon;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/** Stable SHA-256 fingerprint of a canonical request. */
public final class Fingerprint {
    private Fingerprint() {
    }

    public static String of(Map<String, Object> canonical) {
        String json = Json.stringify(canonical);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + hex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            out.append(Character.forDigit((value >> 4) & 0xf, 16));
            out.append(Character.forDigit(value & 0xf, 16));
        }
        return out.toString();
    }
}
