package dev.acciv.apicanon;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Very small JSON parser and writer. No external libraries. */
public final class Json {
    private Json() {
    }

    public static Object parse(String text) {
        Parser parser = new Parser(text);
        Object value = parser.value();
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw new IllegalArgumentException("Unexpected trailing input at " + parser.position());
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object value = parse(text);
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("Expected JSON object");
        }
        return (Map<String, Object>) value;
    }

    public static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out, 0, false);
        return out.toString();
    }

    public static String pretty(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out, 0, true);
        return out.toString();
    }

    /** Returns a value with sorted object keys and normalized numbers. */
    public static Object canonical(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sorted.put(String.valueOf(entry.getKey()), canonical(entry.getValue()));
            }
            return sorted;
        }
        if (value instanceof List<?> list) {
            List<Object> result = new ArrayList<>();
            for (Object item : list) {
                result.add(canonical(item));
            }
            return result;
        }
        if (value instanceof BigDecimal decimal) {
            BigDecimal stripped = decimal.stripTrailingZeros();
            if (stripped.scale() <= 0) {
                return stripped.longValueExact();
            }
            return stripped;
        }
        if (value instanceof Double number) {
            return normalizeDouble(number);
        }
        if (value instanceof Float number) {
            return normalizeDouble(number.doubleValue());
        }
        return value;
    }

    private static Object normalizeDouble(double number) {
        if (!Double.isFinite(number)) {
            throw new IllegalArgumentException("Non-finite numbers are not supported");
        }
        BigDecimal decimal = BigDecimal.valueOf(number).stripTrailingZeros();
        if (decimal.scale() <= 0) {
            return decimal.longValueExact();
        }
        return decimal;
    }

    private static void write(Object value, StringBuilder out, int depth, boolean pretty) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String text) {
            writeString(text, out);
        } else if (value instanceof Boolean || value instanceof Integer || value instanceof Long) {
            out.append(value);
        } else if (value instanceof BigDecimal decimal) {
            out.append(decimal.stripTrailingZeros().toPlainString());
        } else if (value instanceof Double number) {
            out.append(normalizeDouble(number));
        } else if (value instanceof Map<?, ?> map) {
            writeObject(map, out, depth, pretty);
        } else if (value instanceof List<?> list) {
            writeArray(list, out, depth, pretty);
        } else {
            throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass());
        }
    }

    private static void writeObject(Map<?, ?> map, StringBuilder out, int depth, boolean pretty) {
        if (map.isEmpty()) {
            out.append("{}");
            return;
        }
        out.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            if (pretty) {
                out.append('\n').append(indent(depth + 1));
            }
            writeString(String.valueOf(entry.getKey()), out);
            out.append(':');
            if (pretty) {
                out.append(' ');
            }
            write(entry.getValue(), out, depth + 1, pretty);
        }
        if (pretty) {
            out.append('\n').append(indent(depth));
        }
        out.append('}');
    }

    private static void writeArray(List<?> list, StringBuilder out, int depth, boolean pretty) {
        if (list.isEmpty()) {
            out.append("[]");
            return;
        }
        out.append('[');
        boolean first = true;
        for (Object item : list) {
            if (!first) {
                out.append(',');
            }
            first = false;
            if (pretty) {
                out.append('\n').append(indent(depth + 1));
            }
            write(item, out, depth + 1, pretty);
        }
        if (pretty) {
            out.append('\n').append(indent(depth));
        }
        out.append(']');
    }

    private static String indent(int depth) {
        return "  ".repeat(depth);
    }

    private static void writeString(String text, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    private static final class Parser {
        private final String text;
        private int index;

        Parser(String text) {
            this.text = text;
        }

        Object value() {
            skipWhitespace();
            if (atEnd()) {
                throw error("Unexpected end of input");
            }
            char c = text.charAt(index);
            return switch (c) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (consume('}')) {
                return result;
            }
            while (true) {
                skipWhitespace();
                if (atEnd() || text.charAt(index) != '"') {
                    throw error("Expected object key");
                }
                String key = string();
                skipWhitespace();
                expect(':');
                result.put(key, value());
                skipWhitespace();
                if (consume('}')) {
                    return result;
                }
                expect(',');
            }
        }

        private List<Object> array() {
            expect('[');
            List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (consume(']')) {
                return result;
            }
            while (true) {
                result.add(value());
                skipWhitespace();
                if (consume(']')) {
                    return result;
                }
                expect(',');
            }
        }

        private String string() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (!atEnd()) {
                char c = text.charAt(index++);
                if (c == '"') {
                    return out.toString();
                }
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                if (atEnd()) {
                    throw error("Unterminated escape");
                }
                char escaped = text.charAt(index++);
                switch (escaped) {
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    case '/' -> out.append('/');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (index + 4 > text.length()) {
                            throw error("Short unicode escape");
                        }
                        String hex = text.substring(index, index + 4);
                        try {
                            out.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException e) {
                            throw error("Bad unicode escape");
                        }
                        index += 4;
                    }
                    default -> throw error("Unknown escape: " + escaped);
                }
            }
            throw error("Unterminated string");
        }

        private Object number() {
            int start = index;
            while (!atEnd() && isNumberChar(text.charAt(index))) {
                index++;
            }
            if (start == index) {
                throw error("Expected value");
            }
            String raw = text.substring(start, index);
            if (raw.indexOf('.') < 0 && raw.indexOf('e') < 0 && raw.indexOf('E') < 0) {
                try {
                    return Long.parseLong(raw);
                } catch (NumberFormatException ignored) {
                    // fall through to BigDecimal
                }
            }
            try {
                return new BigDecimal(raw);
            } catch (NumberFormatException e) {
                throw error("Bad number: " + raw);
            }
        }

        private Object literal(String expected, Object value) {
            if (!text.startsWith(expected, index)) {
                throw error("Expected " + expected);
            }
            index += expected.length();
            return value;
        }

        private boolean isNumberChar(char c) {
            return (c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E';
        }

        void skipWhitespace() {
            while (!atEnd() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }

        boolean atEnd() {
            return index >= text.length();
        }

        int position() {
            return index;
        }

        private boolean consume(char c) {
            if (!atEnd() && text.charAt(index) == c) {
                index++;
                return true;
            }
            return false;
        }

        private void expect(char c) {
            if (!consume(c)) {
                throw error("Expected '" + c + "'");
            }
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at " + index);
        }
    }
}
