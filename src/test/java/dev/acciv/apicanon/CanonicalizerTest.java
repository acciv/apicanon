package dev.acciv.apicanon;

import java.util.Map;

public final class CanonicalizerTest {
    public static void testPathIsNormalized() {
        Request request = Request.from(Json.parseObject("{\"method\":\"get\",\"path\":\"/v1//users/./42/../43\"}"));
        Map<String, Object> canonical = Canonicalizer.canonical(request);
        Assert.equal("GET", canonical.get("method"));
        Assert.equal("/v1/users/43", canonical.get("path"));
    }

    public static void testQueryIsDecodedAndSorted() {
        Request request = Request.from(Json.parseObject(
                "{\"path\":\"/items?b=2&a=1&a=0&c=hello+world\",\"query\":{\"a\":[\"2\"]}}"));
        Map<String, Object> canonical = Canonicalizer.canonical(request);
        Assert.equal("[[\"a\",\"0\"],[\"a\",\"1\"],[\"a\",\"2\"],[\"b\",\"2\"],[\"c\",\"hello world\"]]",
                Json.stringify(canonical.get("query")));
    }

    public static void testHeadersAreLowercasedFilteredAndRedacted() {
        Request request = Request.from(Json.parseObject(
                "{\"headers\":{\"X-Request-Id\":\"r1\",\"Content-Type\":\"application/json\","
                        + "\"Authorization\":\"Bearer ghp_FAKE_SECRET\"}}"));
        Map<String, Object> canonical = Canonicalizer.canonical(request);
        Assert.equal("[[\"authorization\",\"Bearer ***\"],[\"content-type\",\"application/json\"]]",
                Json.stringify(canonical.get("headers")));
    }

    public static void testBodyKeysAreSorted() {
        Request request = Request.from(Json.parseObject("{\"body\":{\"z\":1,\"a\":{\"y\":2,\"x\":3}}}"));
        Map<String, Object> canonical = Canonicalizer.canonical(request);
        Assert.equal("{\"a\":{\"x\":3,\"y\":2},\"z\":1}", Json.stringify(canonical.get("body")));
    }

    public static void testCanonicalJsonIsStable() {
        String first = Canonicalizer.canonicalJson(Request.from(Json.parseObject(
                "{\"path\":\"/x?b=2&a=1\",\"body\":{\"b\":1,\"a\":2}}")));
        String second = Canonicalizer.canonicalJson(Request.from(Json.parseObject(
                "{\"body\":{\"a\":2,\"b\":1},\"path\":\"/x?a=1&b=2\"}")));
        Assert.equal(first, second);
    }
}
