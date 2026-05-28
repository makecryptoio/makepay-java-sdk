package io.makecrypto.makepay;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MakePayWebhookTest {
    @Test
    void verifiesAndParsesSignedPayloads() throws Exception {
        String body = "{\"event\":{\"type\":\"status_changed\"}}";
        String secret = "whsec_test";
        long timestamp = Instant.now().getEpochSecond();
        String signature = hmacSha256(secret, timestamp + "." + body);
        String header = "t=" + timestamp + ",v1=" + signature;

        assertTrue(MakePayWebhook.verify(body, header, secret));
        assertFalse(MakePayWebhook.verify(body, header, "wrong"));

        JsonNode parsed = MakePayWebhook.parse(body, header, secret);
        assertEquals("status_changed", parsed.path("event").path("type").asText());
    }

    @Test
    void rejectsInvalidPayloads() {
        assertFalse(MakePayWebhook.verify("{}", "t=bad,v1=abc", "whsec_test"));
        assertThrows(
            MakePayError.class,
            () -> MakePayWebhook.parse("{}", "t=1,v1=abc", "whsec_test")
        );
    }

    private static String hmacSha256(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
