package dev.acciv.apicanon;

import java.util.Map;

public final class CliTest {
    public static void testAnalyzeReturnsCanonicalFields() {
        Map<String, Object> result = Cli.analyze("{\"method\":\"post\",\"path\":\"/v1//a\"}");
        Assert.equal("POST", result.get("method"));
        Assert.equal("/v1/a", result.get("path"));
        Assert.trueValue(result.get("fingerprint").toString().startsWith("sha256:"));
    }

    public static void testIdenticalRequestsHaveNoChanges() {
        String a = "{\"path\":\"/x?b=2&a=1\",\"body\":{\"z\":1,\"a\":2}}";
        String b = "{\"body\":{\"a\":2,\"z\":1},\"path\":\"/x?a=1&b=2\"}";
        Assert.equal("[]", Json.stringify(Cli.diffText(a, b)));
    }

    public static void testDiffIsNotEmptyForDifferentBodies() {
        Assert.trueValue(!Cli.diffText(
                "{\"body\":{\"v\":1}}",
                "{\"body\":{\"v\":2}}").isEmpty());
    }

    public static void testDecimalAndIntegerFormsAreEqual() {
        Assert.equal("[]", Json.stringify(Cli.diffText(
                "{\"body\":{\"v\":2.0}}",
                "{\"body\":{\"v\":2}}")));
    }
}
