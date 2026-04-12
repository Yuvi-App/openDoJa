package opendoja.host;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JamMetadataResolver {
    private static final Pattern DEVICE_HINT_PATTERN = Pattern.compile(
            "(?i)(?:^|[^A-Za-z0-9])((?:FOMA\\s+)?[A-Z]?[0-9]{3,4}i?[A-Z]?(?:S|V|C)?)(?:[^A-Za-z0-9]|$)");

    private JamMetadataResolver() {
    }

    static Properties loadJamProperties(Path jamPath) throws IOException {
        byte[] data = Files.readAllBytes(jamPath);
        CharacterCodingException lastCodingFailure = null;
        for (String charsetName : DoJaEncoding.defaultEncodingCandidates()) {
            try {
                return loadJamProperties(data, Charset.forName(charsetName));
            } catch (CharacterCodingException exception) {
                lastCodingFailure = exception;
            } catch (RuntimeException ignored) {
            }
        }
        if (lastCodingFailure != null) {
            throw lastCodingFailure;
        }
        throw new IllegalStateException("No JAM property charsets configured");
    }

    static Map<String, String> resolveEffectiveParameters(Path jamPath, Properties properties) {
        Map<String, String> parameters = new HashMap<>();
        for (String name : properties.stringPropertyNames()) {
            parameters.put(name, properties.getProperty(name));
        }

        // Apply the same metadata fallbacks used for normal JAM launches so every caller
        // sees one consistent ProfileVer/TargetDevice view of the JAM.
        String inferredProfileVersion = inferProfileVersion(properties);
        if (inferredProfileVersion != null) {
            parameters.put("ProfileVer", inferredProfileVersion);
        }

        String inferredTargetDevice = inferTargetDevice(jamPath, properties);
        if (inferredTargetDevice != null && !parameters.containsKey("TargetDevice")) {
            parameters.put("TargetDevice", inferredTargetDevice);
        }
        return parameters;
    }

    static DoJaProfile resolveProfile(Path jamPath) throws IOException {
        Properties properties = loadJamProperties(jamPath);
        return DoJaProfile.fromParametersOrDocumentedDeviceIdentity(resolveEffectiveParameters(jamPath, properties));
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

    private static String inferTargetDevice(Path jamPath, Properties properties) {
        String inferred = metadataDeviceIdentity(properties);
        if (inferred != null) {
            return inferred;
        }
        return firstDeviceHint(jamPath.toString());
    }

    private static String inferProfileVersion(Properties properties) {
        String configured = properties.getProperty("ProfileVer");
        if (configured != null && !configured.isBlank()) {
            return null;
        }
        if (metadataDeviceIdentity(properties) != null) {
            return null;
        }
        int[] drawArea = parseDrawArea(properties.getProperty("DrawArea"));
        if (drawArea == null) {
            return null;
        }
        DoJaProfile inferred = DoJaProfile.fromDocumentedLegacyDisplayResolution(drawArea[0], drawArea[1]);
        return inferred.isKnown() ? inferred.toString() : null;
    }

    private static String metadataDeviceIdentity(Properties properties) {
        String configured = properties.getProperty("TargetDevice");
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        String packageUrl = properties.getProperty("PackageURL");
        if (packageUrl != null && !packageUrl.isBlank()) {
            return firstDeviceHint(packageUrl);
        }
        return null;
    }

    private static String firstDeviceHint(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        Matcher matcher = DEVICE_HINT_PATTERN.matcher(candidate);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).trim();
    }

    private static int[] parseDrawArea(String rawDrawArea) {
        if (rawDrawArea == null || rawDrawArea.isBlank()) {
            return null;
        }
        String[] parts = rawDrawArea.trim().split("[xX]");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
