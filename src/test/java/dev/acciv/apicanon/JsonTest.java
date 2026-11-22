package dev.acciv.apicanon;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class JsonTest {
    public static void testParseNestedObject() {
        Object value = Json.parse("{\"a\":1,\"b\":[true,null,\"x\"],\"c\":{\"d\":2.50}}");
        Assert.trueValue(value instanceof Map<?, ?>);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) value;
        Assert.equal(1L, map.get("a"));
        Assert.equal(Arrays.asList(true, null, "x"), map.get("b"));
    }

    public static void testCanonicalSortsKeysAndNumbers() {
        Object value = Json.canonical(Json.parse("{\"z\":1,\"a\":{\"y\":2.0,\"x\":1}}"));
        Assert.equal("{\"a\":{\"x\":1,\"y\":2},\"z\":1}", Json.stringify(value));
    }

    public static void testStringifyEscapesControlCharacters() {
        Object value = Json.parse("{\"text\":\"a\\nb\\t\\\"c\"}");
        Assert.contains(Json.stringify(value), "\\n");
        Assert.contains(Json.stringify(value), "\\t");
        Assert.contains(Json.stringify(value), "\\\"");
    }

    public static void testPrettyAddsNewlines() {
        String pretty = Json.pretty(Json.parse("{\"a\":[1,2]}"));
        Assert.contains(pretty, "\n");
        Assert.contains(pretty, "  \"a\"");
    }
}
