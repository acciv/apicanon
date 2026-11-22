package dev.acciv.apicanon;

import java.util.Objects;

/** Minimal assertion helpers so tests need no external framework. */
public final class Assert {
    private Assert() {
    }

    public static void equal(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("expected <" + expected + "> but got <" + actual + ">");
        }
    }

    public static void trueValue(boolean condition) {
        if (!condition) {
            throw new AssertionError("expected true");
        }
    }

    public static void contains(String haystack, String needle) {
        if (!haystack.contains(needle)) {
            throw new AssertionError("expected to find <" + needle + "> in <" + haystack + ">");
        }
    }

    public static void fail(String message) {
        throw new AssertionError(message);
    }
}
