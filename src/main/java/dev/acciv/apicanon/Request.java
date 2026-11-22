package dev.acciv.apicanon;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Input API request as read from a JSON file. */
public final class Request {
    private final String method;
    private final String path;
    private final Map<String, Object> query;
    private final Map<String, Object> headers;
    private final Object body;

    private Request(String method, String path, Map<String, Object> query,
                    Map<String, Object> headers, Object body) {
        this.method = method;
        this.path = path;
        this.query = query;
        this.headers = headers;
        this.body = body;
    }

    public static Request from(Map<String, Object> raw) {
        String method = string(raw.get("method"));
        String path = string(raw.get("path"));
        Map<String, Object> query = object(raw.get("query"));
        Map<String, Object> headers = object(raw.get("headers"));
        Object body = raw.containsKey("body") ? raw.get("body") : null;

        int question = path.indexOf('?');
        if (question >= 0) {
            String inline = path.substring(question + 1);
            path = path.substring(0, question);
            mergeQuery(query, parseQuery(inline));
        }
        return new Request(method.isEmpty() ? "GET" : method.toUpperCase(), path, query, headers, body);
    }

    public String method() {
        return method;
    }

    public String path() {
        return path;
    }

    public Map<String, Object> query() {
        return query;
    }

    public Map<String, Object> headers() {
        return headers;
    }

    public Object body() {
        return body;
    }

    private static void mergeQuery(Map<String, Object> target, Map<String, Object> extra) {
        for (Map.Entry<String, Object> entry : extra.entrySet()) {
            Object existing = target.get(entry.getKey());
            if (existing == null) {
                target.put(entry.getKey(), entry.getValue());
            } else {
                List<Object> values = new ArrayList<>();
                values.addAll(asList(existing));
                values.addAll(asList(entry.getValue()));
                target.put(entry.getKey(), values);
            }
        }
    }

    private static Map<String, Object> parseQuery(String inline) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (inline.isEmpty()) {
            return result;
        }
        for (String pair : inline.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int equals = pair.indexOf('=');
            String key = equals < 0 ? pair : pair.substring(0, equals);
            String value = equals < 0 ? "" : pair.substring(equals + 1);
            String name = Url.decodeQuery(key);
            String decoded = Url.decodeQuery(value);
            if (result.containsKey(name)) {
                List<Object> values = new ArrayList<>();
                values.addAll(asList(result.get(name)));
                values.add(decoded);
                result.put(name, values);
            } else {
                result.put(name, decoded);
            }
        }
        return result;
    }

    private static List<Object> asList(Object value) {
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        List<Object> result = new ArrayList<>();
        result.add(value);
        return result;
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?>) {
            return new LinkedHashMap<>((Map<String, Object>) value);
        }
        return new LinkedHashMap<>();
    }
}
