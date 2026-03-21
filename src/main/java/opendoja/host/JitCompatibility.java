package opendoja.host;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class JitCompatibility {
    private static final String APPLIED_PROPERTY = "opendoja.jitCompatApplied";
    private static final String DEFAULT_ENCODING_PROPERTY = "opendoja.defaultEncoding";
    private static final String[] DEFAULT_ENCODINGS = {"CP932", "Shift_JIS"};

    private JitCompatibility() {
    }

    static void reexecJamLauncherIfNeeded(Path jamPath) throws IOException, InterruptedException {
        if (Boolean.getBoolean(APPLIED_PROPERTY)) {
            return;
        }
        String targetEncoding = targetDefaultEncoding();
        boolean needsEncodingCompat = targetEncoding != null && !defaultCharsetMatches(targetEncoding);
        boolean needsJitCompat = Boolean.parseBoolean(System.getProperty("opendoja.jitCompat", "true"))
                && !alreadyUsingCompileCommands();
        if (!needsEncodingCompat && !needsJitCompat) {
            return;
        }

        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(jamPath)) {
            properties.load(in);
        }
        String appClassName = properties.getProperty("AppClass");
        if (appClassName == null || appClassName.isBlank()) {
            return;
        }

        Set<String> patterns = new LinkedHashSet<>();
        String trimmedAppClass = appClassName.trim();
        patterns.add(trimmedAppClass + ".*");
        Path packagePath = resolvePackagePath(jamPath, properties.getProperty("PackageURL"));
        if (packagePath == null) {
            packagePath = resolveAppCodeSource(trimmedAppClass);
        }
        if (packagePath != null && Files.isRegularFile(packagePath) && packagePath.getFileName().toString().endsWith(".jar")) {
            patterns.addAll(classPatternsFromJar(packagePath));
        }
        if (patterns.isEmpty()) {
            return;
        }

        Path commandFile = null;
        if (needsJitCompat) {
            if (patterns.isEmpty()) {
                needsJitCompat = false;
            } else {
                commandFile = writeCompileCommandFile(patterns);
            }
        }
        if (!needsEncodingCompat && !needsJitCompat) {
            return;
        }

        Process process = new ProcessBuilder(buildJavaCommand(commandFile, targetEncoding, JamLauncher.class.getName(), new String[]{jamPath.toString()}))
                .inheritIO()
                .start();
        int exit = process.waitFor();
        System.exit(exit);
    }

    private static boolean alreadyUsingCompileCommands() {
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (arg.startsWith("-XX:CompileCommandFile=")) {
                return true;
            }
        }
        return false;
    }

    private static List<String> buildJavaCommand(Path commandFile, String targetEncoding, String mainClass, String[] args) {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (arg.startsWith("-XX:CompileCommandFile=")
                    || arg.startsWith("-D" + APPLIED_PROPERTY + "=")
                    || (targetEncoding != null && arg.startsWith("-Dfile.encoding="))) {
                continue;
            }
            command.add(arg);
        }
        command.add("-D" + APPLIED_PROPERTY + "=true");
        if (targetEncoding != null) {
            // Many DoJa-era games decode resource tables through String(byte[], off, len), which
            // follows the VM default charset. Modern Java defaults to UTF-8, but the handset-era
            // blobs here are Shift-JIS/Windows-31J encoded.
            command.add("-Dfile.encoding=" + targetEncoding);
        }
        if (commandFile != null) {
            command.add("-XX:CompileCommandFile=" + commandFile);
        }
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(mainClass);
        for (String arg : args) {
            command.add(arg);
        }
        return command;
    }

    private static Path writeCompileCommandFile(Set<String> patterns) throws IOException {
        Path file = Files.createTempFile("opendoja-compile-commands", ".txt");
        List<String> lines = new ArrayList<>(patterns.size());
        for (String pattern : patterns) {
            lines.add("exclude " + pattern);
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
        return file;
    }

    private static Set<String> classPatternsFromJar(Path jarPath) throws IOException {
        Set<String> patterns = new LinkedHashSet<>();
        try (InputStream in = Files.newInputStream(jarPath); ZipInputStream zip = new ZipInputStream(in)) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null; ) {
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }
                String className = entry.getName().substring(0, entry.getName().length() - ".class".length())
                        .replace('/', '.');
                patterns.add(className + ".*");
            }
        }
        return patterns;
    }

    private static Path resolveAppCodeSource(String appClassName) {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> appClass = Class.forName(appClassName, false, loader);
            if (appClass.getProtectionDomain() == null || appClass.getProtectionDomain().getCodeSource() == null) {
                return null;
            }
            URI location = appClass.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path path = Path.of(location);
            return Files.isDirectory(path) ? null : path;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Path resolvePackagePath(Path jamPath, String packageUrl) {
        if (packageUrl == null || packageUrl.isBlank()) {
            return null;
        }
        String trimmed = packageUrl.trim();
        if (trimmed.contains("://")) {
            if (!trimmed.startsWith("file:")) {
                return null;
            }
            return Path.of(java.net.URI.create(trimmed));
        }
        Path base = jamPath.getParent();
        return (base == null ? Path.of(trimmed) : base.resolve(trimmed)).normalize();
    }

    private static String targetDefaultEncoding() {
        if (explicitFileEncodingArgument() != null) {
            return null;
        }
        String override = System.getProperty(DEFAULT_ENCODING_PROPERTY);
        if (override != null) {
            String value = override.trim();
            return value.isEmpty() ? null : value;
        }
        for (String candidate : DEFAULT_ENCODINGS) {
            try {
                return Charset.forName(candidate).name();
            } catch (RuntimeException ignored) {
                // Prefer handset-faithful CP-932 semantics, but keep a Shift_JIS fallback when the
                // host JVM does not expose that superset codec name directly.
            }
        }
        return null;
    }

    private static String explicitFileEncodingArgument() {
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (arg.startsWith("-Dfile.encoding=")) {
                return arg.substring("-Dfile.encoding=".length());
            }
        }
        return null;
    }

    private static boolean defaultCharsetMatches(String targetEncoding) {
        try {
            return Charset.defaultCharset().name().equalsIgnoreCase(Charset.forName(targetEncoding).name());
        } catch (RuntimeException ignored) {
            return true;
        }
    }
}
