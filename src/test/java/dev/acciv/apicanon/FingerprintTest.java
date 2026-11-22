package dev.acciv.apicanon;

import java.util.Map;

public final class FingerprintTest {
    public static void testFingerprintShape() {
        Map<String, Object> canonical = Canonicalizer.canonical(Request.from(Json.parseObject(
                "{\"path\":\"/v1/items\",\"method\":\"GET\"}")));
        String fingerprint = Fingerprint.of(canonical);
        Assert.trueValue(fingerprint.startsWith("sha256:"));
        Assert.equal(71, fingerprint.length());
    }

    public static void testFingerprintIgnoresVolatileHeaders() {
        String a = Cli.analyze("{\"path\":\"/x\",\"headers\":{\"X-Request-Id\":\"1\"}}").get("fingerprint").toString();
        String b = Cli.analyze("{\"path\":\"/x\",\"headers\":{\"X-Request-Id\":\"2\"}}").get("fingerprint").toString();
        Assert.equal(a, b);
    }

    public static void testFingerprintChangesWithBody() {
        String a = Cli.analyze("{\"path\":\"/x\",\"body\":{\"v\":1}}").get("fingerprint").toString();
        String b = Cli.analyze("{\"path\":\"/x\",\"body\":{\"v\":2}}").get("fingerprint").toString();
        Assert.trueValue(!a.equals(b));
    }
}
