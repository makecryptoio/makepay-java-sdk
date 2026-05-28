package io.makecrypto.makepay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Helpers for verifying and parsing MakePay signed webhook payloads.
 */
public final class MakePayWebhook {
    private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();

    private MakePayWebhook() {
    }

    public static boolean verify(String rawBody, String signatureHeader, String secret) {
        return verify(rawBody, signatureHeader, secret, 300);
    }

    public static boolean verify(
        String rawBody,
        String signatureHeader,
        String secret,
        int toleranceSeconds
    ) {
        return verify(rawBody == null ? new byte[0] : rawBody.getBytes(StandardCharsets.UTF_8),
            signatureHeader,
            secret,
            toleranceSeconds);
    }

    public static boolean verify(byte[] rawBody, String signatureHeader, String secret) {
        return verify(rawBody, signatureHeader, secret, 300);
    }

    public static boolean verify(
        byte[] rawBody,
        String signatureHeader,
        String secret,
        int toleranceSeconds
    ) {
        if (signatureHeader == null || signatureHeader.trim().isEmpty() || secret == null || secret.isEmpty()) {
            return false;
        }

        Map<String, String> parts = parseSignatureHeader(signatureHeader);
        long timestamp = parseLong(parts.get("t"));
        String signature = parts.get("v1");
        if (
            timestamp <= 0 ||
            signature == null ||
            signature.length() % 2 != 0 ||
            !signature.matches("(?i)^[a-f0-9]+$")
        ) {
            return false;
        }

        if (toleranceSeconds > 0) {
            long now = Instant.now().getEpochSecond();
            if (Math.abs(now - timestamp) > toleranceSeconds) {
                return false;
            }
        }

        String expected = hmacSha256Hex(secret, timestamp + ".", rawBody == null ? new byte[0] : rawBody);
        return MessageDigest.isEqual(
            hexToBytes(expected),
            hexToBytes(signature.toLowerCase(Locale.ROOT))
        );
    }

    public static JsonNode parse(String rawBody, String signatureHeader, String secret) {
        return parse(rawBody, signatureHeader, secret, 300, DEFAULT_MAPPER);
    }

    public static JsonNode parse(
        String rawBody,
        String signatureHeader,
        String secret,
        int toleranceSeconds
    ) {
        return parse(rawBody, signatureHeader, secret, toleranceSeconds, DEFAULT_MAPPER);
    }

    public static JsonNode parse(
        String rawBody,
        String signatureHeader,
        String secret,
        int toleranceSeconds,
        ObjectMapper objectMapper
    ) {
        if (!verify(rawBody, signatureHeader, secret, toleranceSeconds)) {
            throw new MakePayError("Invalid MakePay webhook signature.", 401);
        }

        try {
            JsonNode parsed = objectMapper.readTree(rawBody == null ? "" : rawBody);
            if (parsed == null || !parsed.isObject()) {
                throw new MakePayError("Invalid MakePay webhook JSON body.", 400);
            }
            return parsed;
        } catch (MakePayError error) {
            throw error;
        } catch (Exception error) {
            throw new MakePayError("Invalid MakePay webhook JSON body.", 400, null, error);
        }
    }

    private static Map<String, String> parseSignatureHeader(String header) {
        Map<String, String> parts = new HashMap<>();
        for (String part : header.split(",")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length == 2 && !pair[0].isEmpty()) {
                parts.put(pair[0], pair[1]);
            }
        }
        return parts;
    }

    private static long parseLong(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    private static String hmacSha256Hex(String secret, String prefix, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update(prefix.getBytes(StandardCharsets.UTF_8));
            mac.update(body);
            return bytesToHex(mac.doFinal());
        } catch (Exception error) {
            throw new MakePayError("Unable to verify MakePay webhook signature.", error);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int length = hex.length();
        byte[] bytes = new byte[length / 2];
        for (int index = 0; index < length; index += 2) {
            bytes[index / 2] = (byte) Integer.parseInt(hex.substring(index, index + 2), 16);
        }
        return bytes;
    }
}
