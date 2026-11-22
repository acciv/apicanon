package dev.acciv.apicanon;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Command line entry point. */
public final class Cli {
    private Cli() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        try {
            switch (args[0]) {
                case "canon" -> {
                    require(args.length == 2, "usage: canon <request.json>");
                    Map<String, Object> canonical = canonicalOf(read(args[1]));
                    System.out.println(Json.pretty(canonical));
                }
                case "diff" -> {
                    require(args.length == 3, "usage: diff <a.json> <b.json>");
                    Map<String, Object> left = canonicalOf(read(args[1]));
                    Map<String, Object> right = canonicalOf(read(args[2]));
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("left_fingerprint", left.get("fingerprint"));
                    result.put("right_fingerprint", right.get("fingerprint"));
                    result.put("changes", Diff.between(withoutFingerprint(left), withoutFingerprint(right)));
                    System.out.println(Json.pretty(result));
                }
                case "help", "--help", "-h" -> printUsage();
                default -> throw new IllegalArgumentException("unknown command: " + args[0]);
            }
        } catch (Exception e) {
            System.err.println("error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static Map<String, Object> canonicalOf(String text) {
        Map<String, Object> raw = Json.parseObject(text);
        Map<String, Object> canonical = new LinkedHashMap<>(Canonicalizer.canonical(Request.from(raw)));
        canonical.put("fingerprint", Fingerprint.of(canonical));
        return canonical;
    }

    private static Map<String, Object> withoutFingerprint(Map<String, Object> input) {
        Map<String, Object> result = new LinkedHashMap<>(input);
        result.remove("fingerprint");
        return result;
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void printUsage() {
        System.out.println("apicanon — canonical API request analyzer");
        System.out.println();
        System.out.println("  canon <request.json>       print canonical form and fingerprint");
        System.out.println("  diff <a.json> <b.json>     print structural differences");
        System.out.println("  help                       show this text");
    }

    /** Exposed for tests that want the full command output as data. */
    public static Map<String, Object> analyze(String text) {
        return canonicalOf(text);
    }

    /** Exposed for tests that want a diff as data. */
    public static List<Object> diffText(String left, String right) {
        return Diff.between(withoutFingerprint(canonicalOf(left)), withoutFingerprint(canonicalOf(right)));
    }
}
