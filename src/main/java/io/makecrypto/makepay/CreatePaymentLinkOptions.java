package io.makecrypto.makepay;

/**
 * Optional controls for payment-link creation.
 */
public final class CreatePaymentLinkOptions {
    private final String status;
    private final boolean sendPaymentRequestEmail;

    private CreatePaymentLinkOptions(Builder builder) {
        this.status = builder.status;
        this.sendPaymentRequestEmail = builder.sendPaymentRequestEmail;
    }

    public static CreatePaymentLinkOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getStatus() {
        return status;
    }

    public boolean isSendPaymentRequestEmail() {
        return sendPaymentRequestEmail;
    }

    public static final class Builder {
        private String status = "active";
        private boolean sendPaymentRequestEmail;

        private Builder() {
        }

        public Builder status(String status) {
            this.status = status == null || status.trim().isEmpty() ? "active" : status.trim();
            return this;
        }

        public Builder sendPaymentRequestEmail(boolean sendPaymentRequestEmail) {
            this.sendPaymentRequestEmail = sendPaymentRequestEmail;
            return this;
        }

        public CreatePaymentLinkOptions build() {
            return new CreatePaymentLinkOptions(this);
        }
    }
}
