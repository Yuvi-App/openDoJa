package opendoja.host;

import java.nio.charset.Charset;
import java.util.List;

/**
 * Shared DoJa text-encoding resolution.
 */
public final class DoJaEncoding {
    private static final List<String> DEFAULT_ENCODING_CANDIDATES = List.of("MS932", "windows-31j", "Shift_JIS");

    // Probe these in order and use the first charset the host JVM exposes. The goal is
    // CP-932 semantics, but some JVMs resolve the raw "CP932" alias to x-IBM942C instead of
    // the Windows/MS932 mapping the game data expects, so that alias is intentionally omitted.
    public static final Charset DEFAULT_CHARSET = resolveDefaultCharset();

    private DoJaEncoding() {
    }

    public static Charset defaultCharset() {
        return DEFAULT_CHARSET;
    }

    public static String defaultCharsetName() {
        return DEFAULT_CHARSET.name();
    }

    private static Charset resolveDefaultCharset() {
        for (String candidate : DEFAULT_ENCODING_CANDIDATES) {
            try {
                return Charset.forName(candidate);
            } catch (RuntimeException ignored) {
            }
        }
        return Charset.defaultCharset();
    }
}
