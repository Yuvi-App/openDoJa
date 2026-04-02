package opendoja.host;

import com.nttdocomo.ui.IApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class JamLauncher {
    private JamLauncher() {
    }

    public static IApplication launch(Path jamPath) throws IOException, ClassNotFoundException {
        return launch(jamPath, false);
    }

    public static IApplication launch(Path jamPath, boolean exitOnShutdown) throws IOException, ClassNotFoundException {
        return DesktopLauncher.launch(buildLaunchConfig(jamPath, exitOnShutdown));
    }

    public static LaunchConfig buildLaunchConfig(Path jamPath, boolean exitOnShutdown) throws IOException, ClassNotFoundException {
        Properties properties = loadJamProperties(jamPath);
        return buildLaunchConfig(jamPath, properties, exitOnShutdown);
    }

    private static LaunchConfig buildLaunchConfig(Path jamPath, Properties properties, boolean exitOnShutdown)
            throws IOException, ClassNotFoundException {
        String appClassName = properties.getProperty("AppClass");
        if (appClassName == null || appClassName.isBlank()) {
            throw new IllegalArgumentException("JAM/ADF missing AppClass: " + jamPath);
        }
        Class<?> rawClass = Class.forName(appClassName.trim());
        if (!IApplication.class.isAssignableFrom(rawClass)) {
            throw new IllegalArgumentException("AppClass does not extend IApplication: " + appClassName);
        }
        @SuppressWarnings("unchecked")
        Class<? extends IApplication> applicationClass = (Class<? extends IApplication>) rawClass;
        int[] scratchpadSizes = parseScratchpadSizes(properties.getProperty("SPsize"));
        ResolvedScratchpad scratchpad = resolveScratchpad(jamPath);
        LaunchConfig.Builder builder = LaunchConfig.builder(applicationClass)
                .title(properties.getProperty("AppName", applicationClass.getSimpleName()))
                .sourceUrl(resolvePackageUrl(jamPath, properties.getProperty("PackageURL")))
                .scratchpadPackedFile(scratchpad.path())
                .scratchpadSizes(scratchpadSizes)
                .iAppliType(LaunchConfig.IAppliType.fromJamProperties(properties))
                .exitOnShutdown(exitOnShutdown);
        String drawArea = properties.getProperty("DrawArea");
        if (drawArea != null) {
            String[] parts = drawArea.toLowerCase().split("x");
            if (parts.length == 2) {
                try {
                    builder.viewport(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        String appParam = properties.getProperty("AppParam");
        if (appParam != null && !appParam.isBlank()) {
            builder.args(appParam.trim().split("\\s+"));
        }
        for (String name : properties.stringPropertyNames()) {
            builder.parameter(name, properties.getProperty(name));
        }
        if (!scratchpad.found()) {
            OpenDoJaLog.warn(JamLauncher.class,
                    () -> "No .sp file found for " + jamPath + " next to the JAM or in ../sp; "
                            + "continuing and using " + scratchpad.path() + " if the game creates one at runtime");
        }
        return builder.build();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: JamLauncher <path-to-jam>");
        }
        Path jamPath = Path.of(args[0]);
        LaunchCompatibility.reexecJamLauncherIfNeeded(jamPath);
        try {
            launch(jamPath, true);
        } catch (VerifyError error) {
            // A few handset-era jars contain bytecode that modern HotSpot rejects up front even
            // though the same title otherwise runs once verification is disabled. Retry once from
            // the top launch boundary so the default path stays strict for all normal titles.
            if (!LaunchCompatibility.reexecJamLauncherOnVerifyError(jamPath)) {
                throw error;
            }
            return;
        }
        DoJaRuntime runtime = DoJaRuntime.current();
        if (runtime != null) {
            runtime.awaitShutdown();
        }
        System.exit(0);
    }

    private static String resolvePackageUrl(Path jamPath, String packageUrl) {
        if (packageUrl == null || packageUrl.isBlank()) {
            return jamPath.toUri().toString();
        }
        String trimmed = packageUrl.trim();
        if (trimmed.contains("://")) {
            return normalizePackageUri(URI.create(trimmed)).toString();
        }
        String relativePath = packagePathPart(trimmed);
        if (relativePath.isEmpty()) {
            return jamPath.toUri().toString();
        }
        Path base = jamPath.getParent();
        Path resolved = (base == null ? Path.of(relativePath) : base.resolve(relativePath)).normalize();
        if (isJarName(lastPathSegment(relativePath))) {
            Path absoluteResolved = resolved.toAbsolutePath().normalize();
            Path parent = absoluteResolved.getParent();
            return toDirectoryUri(parent == null ? absoluteResolved : parent);
        }
        return resolved.toUri().toString();
    }

    private static URI normalizePackageUri(URI packageUri) {
        String path = packageUri.getPath();
        if (!isJarName(lastPathSegment(path))) {
            return packageUri;
        }
        return packageUri.resolve(".");
    }

    private static boolean isJarPath(Path path) {
        return path != null && isJarName(path.toString());
    }

    private static boolean isJarName(String name) {
        return name != null && name.length() >= 4 && name.regionMatches(true, name.length() - 4, ".jar", 0, 4);
    }

    private static String lastPathSegment(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static String packagePathPart(String raw) {
        int cut = raw.length();
        int query = raw.indexOf('?');
        if (query >= 0) {
            cut = Math.min(cut, query);
        }
        int fragment = raw.indexOf('#');
        if (fragment >= 0) {
            cut = Math.min(cut, fragment);
        }
        return raw.substring(0, cut).trim();
    }

    private static String toDirectoryUri(Path directory) {
        String uri = directory.toUri().toString();
        return uri.endsWith("/") ? uri : uri + "/";
    }

    private static Properties loadJamProperties(Path jamPath) throws IOException {
        byte[] data = Files.readAllBytes(jamPath);
        try {
            return loadJamProperties(data, DoJaEncoding.DEFAULT_CHARSET);
        } catch (CharacterCodingException ignored) {
            // Most JAM/ADF files follow the handset default charset, but some titles
            // are using UTF-8. Keep the historical decode path first and only fall
            // back when the legacy decoder proves the file is not actually encoded that way.
            return loadJamProperties(data, StandardCharsets.UTF_8);
        }
    }

    private static Properties loadJamProperties(byte[] data, Charset charset) throws IOException {
        Properties properties = new Properties();
        String text = charset.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(data))
                .toString();
        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
            properties.load(reader);
        }
        return properties;
    }

    private static ResolvedScratchpad resolveScratchpad(Path jamPath) {
        String baseName = stripExtension(jamPath.getFileName().toString());
        Path sibling = jamPath.resolveSibling(baseName + ".sp").normalize();
        if (Files.exists(sibling)) {
            return new ResolvedScratchpad(sibling, true);
        }
        Path fallback = jamPath.getParent() == null ? null
                : jamPath.getParent().resolveSibling("sp").resolve(baseName + ".sp").normalize();
        if (fallback != null && Files.exists(fallback)) {
            return new ResolvedScratchpad(fallback, true);
        }
        return new ResolvedScratchpad(sibling, false);
    }

    private static int[] parseScratchpadSizes(String raw) {
        if (raw == null || raw.isBlank()) {
            return new int[0];
        }
        String[] parts = raw.split(",");
        int[] result = new int[parts.length];
        int count = 0;
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            result[count++] = Integer.parseInt(trimmed);
        }
        if (count == result.length) {
            return result;
        }
        int[] compact = new int[count];
        System.arraycopy(result, 0, compact, 0, count);
        return compact;
    }

    private static String stripExtension(String name) {
        int lastDot = name.lastIndexOf('.');
        return lastDot < 0 ? name : name.substring(0, lastDot);
    }

    private record ResolvedScratchpad(Path path, boolean found) {
    }
}
