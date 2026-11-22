package dev.acciv.apicanon;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/** Tiny reflection-based test runner. No JUnit dependency. */
public final class TestRunner {
    private static final Class<?>[] SUITES = {
            JsonTest.class,
            CanonicalizerTest.class,
            FingerprintTest.class,
            DiffTest.class,
            CliTest.class
    };

    private TestRunner() {
    }

    public static void main(String[] args) {
        int passed = 0;
        int failed = 0;
        for (Class<?> suite : SUITES) {
            System.out.println("== " + suite.getSimpleName() + " ==");
            Method[] methods = Arrays.stream(suite.getDeclaredMethods())
                    .filter(m -> m.getName().startsWith("test"))
                    .filter(m -> m.getParameterCount() == 0)
                    .sorted((a, b) -> a.getName().compareTo(b.getName()))
                    .toArray(Method[]::new);
            for (Method method : methods) {
                try {
                    method.invoke(null);
                    System.out.println("  ok   " + method.getName());
                    passed++;
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    System.out.println("  FAIL " + method.getName() + ": " + cause);
                    failed++;
                } catch (ReflectiveOperationException e) {
                    System.out.println("  FAIL " + method.getName() + ": " + e);
                    failed++;
                }
            }
        }
        System.out.println();
        System.out.println("passed=" + passed + " failed=" + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }
}
