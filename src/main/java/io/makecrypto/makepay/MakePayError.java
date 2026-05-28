package io.makecrypto.makepay;

/**
 * Runtime exception thrown for MakePay API, configuration, and webhook errors.
 */
public final class MakePayError extends RuntimeException {
    private final int status;
    private final Object responseBody;

    public MakePayError(String message) {
        this(message, 0, null, null);
    }

    public MakePayError(String message, int status) {
        this(message, status, null, null);
    }

    public MakePayError(String message, int status, Object responseBody) {
        this(message, status, responseBody, null);
    }

    public MakePayError(String message, Throwable cause) {
        this(message, 0, null, cause);
    }

    public MakePayError(String message, int status, Object responseBody, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.responseBody = responseBody;
    }

    public int getStatus() {
        return status;
    }

    public Object getResponseBody() {
        return responseBody;
    }
}
