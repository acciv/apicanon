package dev.acciv.apicanon;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Builds a stable, comparable representation of an API request. */
public final class Canonicalizer {
    private static final Set<String> IGNORED_HEADERS = Set.of(
            "accept-encoding",
            "content-length",
            "date",
            "host",
            "user-agent",
            "x-correlation-id",
            "x-request-id"
    );

    private Canonicalizer() {
    }

    public static Map<String, Object> canonical(Request request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("method", request.method());
        result.put("path", Url.normalizePath(request.path()));
        result.put("query", canonicalQuery(request.query()));
        result.put("headers", canonicalHeaders(request.headers()));
        result.put("body", request.body() == null ? null : Json.canonical(request.body()));
        return result;
    }

    public static String canonicalJson(Request request) {
        return Json.stringify(canonical(request));
    }

    private static List<Object> canonicalQuery(Map<String, Object> query) {
        List<Object[]> pairs = new ArrayList<>();
        for (Map.Entry<String, Object> entry : query.entrySet()) {
            String key = Url.decodeQuery(entry.getKey());
            if (entry.getValue() instanceof List<?> list) {
                for (Object item : list) {
                    pairs.add(new Object[]{key, scalar(item)});
                }
            } else {
                pairs.add(new Object[]{key, scalar(entry.getValue())});
            }
        }
        pairs.sort((a, b) -> {
            int byKey = String.valueOf(a[0]).compareTo(String.valueOf(b[0]));
            return byKey != 0 ? byKey : String.valueOf(a[1]).compareTo(String.valueOf(b[1]));
        });
        List<Object> result = new ArrayList<>();
        for (Object[] pair : pairs) {
            result.add(List.of(pair[0], pair[1]));
        }
        return result;
    }

    private static List<Object> canonicalHeaders(Map<String, Object> headers) {
        List<Object[]> pairs = new ArrayList<>();
        for (Map.Entry<String, Object> entry : headers.entrySet()) {
            String name = entry.getKey().trim().toLowerCase(Locale.ROOT);
            if (name.isEmpty() || IGNORED_HEADERS.contains(name)) {
                continue;
            }
            pairs.add(new Object[]{name, redact(name, scalar(entry.getValue()).trim())});
        }
        pairs.sort((a, b) -> String.valueOf(a[0]).compareTo(String.valueOf(b[0])));
        List<Object> result = new ArrayList<>();
        for (Object[] pair : pairs) {
            result.add(List.of(pair[0], pair[1]));
        }
        return result;
    }

    private static String redact(String name, String value) {
        if (!name.equals("authorization")) {
            return value;
        }
        int space = value.indexOf(' ');
        return space < 0 ? "***" : value.substring(0, space) + " ***";
    }

    private static String scalar(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Map<?, ?> || value instanceof List<?>) {
            return Json.stringify(value);
        }
        return String.valueOf(value);
    }
}
