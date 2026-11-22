package dev.acciv.apicanon;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Percent decoding and deterministic path normalization. */
public final class Url {
    private Url() {
    }

    public static String decodeQuery(String value) {
        return decode(value, true);
    }

    public static String decodePath(String value) {
        return decode(value, false);
    }

    public static String normalizePath(String raw) {
        String decoded = decodePath(raw);
        String[] parts = decoded.split("/", -1);
        Deque<String> segments = new ArrayDeque<>();
        for (String part : parts) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            if (part.equals("..")) {
                if (!segments.isEmpty()) {
                    segments.removeLast();
                }
                continue;
            }
            segments.addLast(part);
        }
        if (segments.isEmpty()) {
            return "/";
        }
        return "/" + String.join("/", segments);
    }

    private static String decode(String value, boolean plusAsSpace) {
        byte[] bytes = new byte[value.length()];
        int length = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (plusAsSpace && c == '+') {
                bytes[length++] = ' ';
            } else if (c == '%' && i + 2 < value.length()) {
                int high = Character.digit(value.charAt(i + 1), 16);
                int low = Character.digit(value.charAt(i + 2), 16);
                if (high >= 0 && low >= 0) {
                    bytes[length++] = (byte) ((high << 4) | low);
                    i += 2;
                } else {
                    bytes[length++] = (byte) c;
                }
            } else {
                bytes[length++] = (byte) c;
            }
        }
        return new String(bytes, 0, length, StandardCharsets.UTF_8);
    }

    /** Kept for symmetry with the other utility classes. */
    public static List<String> splitSegments(String path) {
        List<String> result = new ArrayList<>();
        for (String part : path.split("/", -1)) {
            if (!part.isEmpty()) {
                result.add(part);
            }
        }
        return result;
    }
}
