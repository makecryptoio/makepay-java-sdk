package io.makecrypto.makepay;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;

/**
 * Configuration for {@link MakePayClient}.
 */
public final class MakePayClientOptions {
    private final String baseUrl;
    private final String checkoutBaseUrl;
    private final String keyId;
    private final String keySecret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private MakePayClientOptions(Builder builder) {
        this.baseUrl = builder.baseUrl;
        this.checkoutBaseUrl = builder.checkoutBaseUrl;
        this.keyId = builder.keyId;
        this.keySecret = builder.keySecret;
        this.httpClient = builder.httpClient;
        this.objectMapper = builder.objectMapper;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getCheckoutBaseUrl() {
        return checkoutBaseUrl;
    }

    public String getKeyId() {
        return keyId;
    }

    public String getKeySecret() {
        return keySecret;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public static final class Builder {
        private String baseUrl = MakePayClient.DEFAULT_BASE_URL;
        private String checkoutBaseUrl = MakePayClient.DEFAULT_CHECKOUT_BASE_URL;
        private String keyId = "";
        private String keySecret = "";
        private HttpClient httpClient;
        private ObjectMapper objectMapper;

        private Builder() {
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder checkoutBaseUrl(String checkoutBaseUrl) {
            this.checkoutBaseUrl = checkoutBaseUrl;
            return this;
        }

        public Builder keyId(String keyId) {
            this.keyId = keyId;
            return this;
        }

        public Builder keySecret(String keySecret) {
            this.keySecret = keySecret;
            return this;
        }

        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public MakePayClientOptions build() {
            return new MakePayClientOptions(this);
        }
    }
}
