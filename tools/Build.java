import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Java-only build and test driver. Run: javac -d out/tools tools/Build.java && java -cp out/tools Build */
public final class Build {
    private Build() {
    }

    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.err.println("A JDK is required (javac not found)");
            System.exit(1);
            return;
        }

        Path classes = Path.of("out", "classes");
        Files.createDirectories(classes);

        List<String> command = new ArrayList<>();
        command.add("-d");
        command.add(classes.toString());
        for (Path source : sources()) {
            command.add(source.toString());
        }

        int status = compiler.run(null, null, null, command.toArray(new String[0]));
        if (status != 0) {
            System.err.println("compilation failed");
            System.exit(status);
            return;
        }

        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder(
                java, "-cp", classes.toString(), "dev.acciv.apicanon.TestRunner")
                .inheritIO()
                .start();
        System.exit(process.waitFor());
    }

    private static List<Path> sources() throws Exception {
        List<Path> result = new ArrayList<>();
        for (String root : List.of("src/main/java", "src/test/java")) {
            try (Stream<Path> stream = Files.walk(Path.of(root))) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .sorted(Comparator.comparing(Path::toString))
                        .forEach(result::add);
            }
        }
        if (result.isEmpty()) {
            throw new IllegalStateException("no Java sources found");
        }
        return result;
    }
}
