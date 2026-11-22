package dev.acciv.apicanon;

public final class DiffTest {
    public static void testBodyValueChange() {
        String changes = Json.stringify(Cli.diffText(
                "{\"path\":\"/x\",\"body\":{\"v\":1}}",
                "{\"path\":\"/x\",\"body\":{\"v\":2}}"));
        Assert.contains(changes, "$.body.v");
        Assert.contains(changes, "changed");
    }

    public static void testQueryValueChange() {
        String changes = Json.stringify(Cli.diffText(
                "{\"path\":\"/x?b=1\"}",
                "{\"path\":\"/x?b=2\"}"));
        Assert.contains(changes, "$.query[0][1]");
    }

    public static void testHeaderValueChange() {
        String changes = Json.stringify(Cli.diffText(
                "{\"headers\":{\"X-Trace\":\"one\"}}",
                "{\"headers\":{\"X-Trace\":\"two\"}}"));
        Assert.contains(changes, "$.headers[0][1]");
    }

    public static void testAddedKeyAndMethodChange() {
        String changes = Json.stringify(Cli.diffText(
                "{\"method\":\"GET\",\"body\":{\"v\":1}}",
                "{\"method\":\"POST\",\"body\":{\"v\":1,\"extra\":true}}"));
        Assert.contains(changes, "$.method");
        Assert.contains(changes, "$.body.extra");
        Assert.contains(changes, "added");
    }
}
