package opendoja.host;

import java.lang.reflect.Method;
import java.util.List;

public final class LaunchCompatibilityProbe {
    private LaunchCompatibilityProbe() {
    }

    public static void main(String[] args) throws Exception {
        String expectedEncoding = args.length == 0 ? DoJaEncoding.defaultCharsetName() : args[0];
        verifyCompatibilityCommand(expectedEncoding);
        verifyVerifyFallbackCommand(expectedEncoding);
    }

    private static void verifyCompatibilityCommand(String expectedEncoding) throws Exception {
        Method method = LaunchCompatibility.class.getDeclaredMethod("buildCompatibilityCommand",
                boolean.class, boolean.class, boolean.class, boolean.class, String.class, String[].class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> command = (List<String>) method.invoke(null,
                false, false, false, false, "opendoja.host.JamLauncher", new String[]{"sample.jam"});
        verifyFileEncoding(command, expectedEncoding, "compatibility");
    }

    private static void verifyVerifyFallbackCommand(String expectedEncoding) throws Exception {
        Method method = LaunchCompatibility.class.getDeclaredMethod("buildVerifyFallbackCommand",
                String.class, String[].class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> command = (List<String>) method.invoke(null,
                "opendoja.host.JamLauncher", new String[]{"sample.jam"});
        verifyFileEncoding(command, expectedEncoding, "verify fallback");
    }

    private static void verifyFileEncoding(List<String> command, String expectedEncoding, String label) {
        String expectedArgument = "-Dfile.encoding=" + expectedEncoding;
        long matches = command.stream().filter(arg -> arg.startsWith("-Dfile.encoding=")).count();
        check(matches == 1L, label + " command should contain exactly one file.encoding argument: " + command);
        check(command.contains(expectedArgument),
                label + " command should contain " + expectedArgument + " but was " + command);
        System.out.println(label + " command OK: " + expectedArgument);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
