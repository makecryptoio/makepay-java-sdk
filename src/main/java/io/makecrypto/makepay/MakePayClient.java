package io.makecrypto.makepay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Server-side client for the MakePay partner API.
 */
public final class MakePayClient {
    public static final String DEFAULT_BASE_URL = "https://www.makecrypto.io";
    public static final String DEFAULT_CHECKOUT_BASE_URL = "https://makepay.io";
    public static final String VERSION = "0.3.0";

    private final String baseUrl;
    private final String checkoutBaseUrl;
    private final String keyId;
    private final String keySecret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MakePayClient(String keyId, String keySecret) {
        this(MakePayClientOptions.builder().keyId(keyId).keySecret(keySecret).build());
    }

    public MakePayClient(MakePayClientOptions options) {
        Objects.requireNonNull(options, "MakePay client options are required.");
        this.baseUrl = normalizeBaseUrl(orDefault(options.getBaseUrl(), DEFAULT_BASE_URL));
        this.checkoutBaseUrl = normalizeBaseUrl(orDefault(options.getCheckoutBaseUrl(), DEFAULT_CHECKOUT_BASE_URL));
        this.keyId = trimToEmpty(options.getKeyId());
        this.keySecret = trimToEmpty(options.getKeySecret());
        this.httpClient = options.getHttpClient() == null ? HttpClient.newHttpClient() : options.getHttpClient();
        this.objectMapper = options.getObjectMapper() == null ? new ObjectMapper() : options.getObjectMapper();

        if (this.keyId.isEmpty() || this.keySecret.isEmpty()) {
            throw new MakePayError("MakePay keyId and keySecret are required.", 400);
        }
    }

    public JsonNode createPaymentLink(Map<String, ?> payload) {
        return createPaymentLink(payload, CreatePaymentLinkOptions.defaults());
    }

    public JsonNode createPaymentLink(Map<String, ?> payload, CreatePaymentLinkOptions options) {
        return createPaymentLikeLink("/api/partner/v1/makepay/payment-links", payload, options, null);
    }

    public JsonNode listPaymentLinks() {
        return listPaymentLinks(Collections.emptyMap());
    }

    public JsonNode listPaymentLinks(Map<String, ?> query) {
        return request("GET", "/api/partner/v1/makepay/payment-links", null, query);
    }

    public JsonNode getPaymentLink(String uid) {
        assertNonEmpty(uid, "Payment link UID is required.");
        return request("GET", "/api/partner/v1/makepay/payment-links/" + urlEncode(uid), null);
    }

    public JsonNode updatePaymentLink(String uid, Map<String, ?> updates) {
        assertNonEmpty(uid, "Payment link UID is required.");
        return request(
            "PATCH",
            "/api/partner/v1/makepay/payment-links/" + urlEncode(uid),
            updates == null ? Collections.emptyMap() : updates
        );
    }

    public JsonNode sendPaymentRequestEmail(String uid) {
        return sendPaymentRequestEmail(uid, null);
    }

    public JsonNode sendPaymentRequestEmail(String uid, String email) {
        assertNonEmpty(uid, "Payment link UID is required.");
        Map<String, Object> body = new LinkedHashMap<>();
        if (email != null && !email.trim().isEmpty()) {
            body.put("email", email.trim());
        }

        return request(
            "POST",
            "/api/partner/v1/makepay/payment-links/" + urlEncode(uid) + "/send-request-email",
            body
        );
    }

    public JsonNode createDonationLink(Map<String, ?> payload) {
        return createDonationLink(payload, CreatePaymentLinkOptions.defaults());
    }

    public JsonNode createDonationLink(Map<String, ?> payload, CreatePaymentLinkOptions options) {
        return createPaymentLikeLink("/api/partner/v1/makepay/donations", payload, options, "donation");
    }

    public JsonNode listDonationLinks() {
        return request("GET", "/api/partner/v1/makepay/donations", null);
    }

    public JsonNode getDonationLink(String uid) {
        assertNonEmpty(uid, "Donation link UID is required.");
        return request("GET", "/api/partner/v1/makepay/donations/" + urlEncode(uid), null);
    }

    public JsonNode updateDonationLink(String uid, Map<String, ?> updates) {
        assertNonEmpty(uid, "Donation link UID is required.");
        return request(
            "PATCH",
            "/api/partner/v1/makepay/donations/" + urlEncode(uid),
            updates == null ? Collections.emptyMap() : updates
        );
    }

    public JsonNode listCustomers() {
        return request("GET", "/api/partner/v1/makepay/customers", null);
    }

    public JsonNode upsertCustomer(Map<String, ?> payload) {
        return request("POST", "/api/partner/v1/makepay/customers", nullToEmpty(payload));
    }

    public JsonNode createCustomerPortal(String customerId) {
        return createCustomerPortal(customerId, Collections.emptyMap());
    }

    public JsonNode createCustomerPortal(String customerId, Map<String, ?> payload) {
        assertNonEmpty(customerId, "Customer ID is required.");
        return request(
            "POST",
            "/api/partner/v1/makepay/customers/" + urlEncode(customerId) + "/portal",
            nullToEmpty(payload)
        );
    }

    public JsonNode listSubscriptions() {
        return request("GET", "/api/partner/v1/makepay/subscriptions", null);
    }

    public JsonNode createSubscription(Map<String, ?> payload) {
        return request("POST", "/api/partner/v1/makepay/subscriptions", nullToEmpty(payload));
    }

    public JsonNode listDestinationAssets() {
        return request("GET", "/api/partner/v1/makepay/destination-assets", null);
    }

    public JsonNode listWebhookRequests() {
        return listWebhookRequests(Collections.emptyMap());
    }

    public JsonNode listWebhookRequests(Map<String, ?> query) {
        return request("GET", "/api/partner/v1/makepay/webhook-requests", null, query);
    }

    public JsonNode listPosTerminals() {
        return request("GET", "/api/partner/v1/makepay/pos-terminals", null);
    }

    public JsonNode createPosTerminal(Map<String, ?> payload) {
        return request("POST", "/api/partner/v1/makepay/pos-terminals", nullToEmpty(payload));
    }

    public JsonNode getPosTerminal(String terminalId) {
        assertNonEmpty(terminalId, "POS terminal ID is required.");
        return request("GET", "/api/partner/v1/makepay/pos-terminals/" + urlEncode(terminalId), null);
    }

    public JsonNode updatePosTerminal(String terminalId, Map<String, ?> payload) {
        assertNonEmpty(terminalId, "POS terminal ID is required.");
        return request(
            "PATCH",
            "/api/partner/v1/makepay/pos-terminals/" + urlEncode(terminalId),
            nullToEmpty(payload)
        );
    }

    public JsonNode listProducts() {
        return request("GET", "/api/partner/v1/makepay/products", null);
    }

    public JsonNode createProduct(Map<String, ?> payload) {
        return request("POST", "/api/partner/v1/makepay/products", nullToEmpty(payload));
    }

    public JsonNode getProduct(String productId) {
        assertNonEmpty(productId, "Product ID is required.");
        return request("GET", "/api/partner/v1/makepay/products/" + urlEncode(productId), null);
    }

    public JsonNode updateProduct(String productId, Map<String, ?> payload) {
        assertNonEmpty(productId, "Product ID is required.");
        return request(
            "PATCH",
            "/api/partner/v1/makepay/products/" + urlEncode(productId),
            nullToEmpty(payload)
        );
    }

    public JsonNode listProductDownloads(String productId) {
        assertNonEmpty(productId, "Product ID is required.");
        return request("GET", "/api/partner/v1/makepay/shop/products/" + urlEncode(productId) + "/downloads", null);
    }

    public JsonNode createProductDownload(String productId, Map<String, ?> payload) {
        assertNonEmpty(productId, "Product ID is required.");
        return request(
            "POST",
            "/api/partner/v1/makepay/shop/products/" + urlEncode(productId) + "/downloads",
            nullToEmpty(payload)
        );
    }

    public JsonNode getShop() {
        return request("GET", "/api/partner/v1/makepay/shop", null);
    }

    public JsonNode updateShop(Map<String, ?> payload) {
        return request("PATCH", "/api/partner/v1/makepay/shop", nullToEmpty(payload));
    }

    public JsonNode getShopBuilder() {
        return request("GET", "/api/partner/v1/makepay/shop/builder", null);
    }

    public JsonNode updateShopBuilder(Map<String, ?> payload) {
        return request("PUT", "/api/partner/v1/makepay/shop/builder", nullToEmpty(payload));
    }

    public JsonNode getShopDomain() {
        return request("GET", "/api/partner/v1/makepay/shop/domains", null);
    }

    public JsonNode updateShopDomain(String domain) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("domain", domain);
        return updateShopDomain(body);
    }

    public JsonNode updateShopDomain(Map<String, ?> payload) {
        return request("PUT", "/api/partner/v1/makepay/shop/domains", nullToEmpty(payload));
    }

    public JsonNode refreshShopDomain() {
        return refreshShopDomain(Collections.emptyMap());
    }

    public JsonNode refreshShopDomain(String domain) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("domain", domain);
        return refreshShopDomain(body);
    }

    public JsonNode refreshShopDomain(Map<String, ?> payload) {
        return request("POST", "/api/partner/v1/makepay/shop/domains", nullToEmpty(payload));
    }

    public JsonNode listShopCoupons() {
        return request("GET", "/api/partner/v1/makepay/shop/coupons", null);
    }

    public JsonNode createShopCoupon(Map<String, ?> payload) {
        return request("POST", "/api/partner/v1/makepay/shop/coupons", nullToEmpty(payload));
    }

    public JsonNode updateShopCoupon(String couponUid, Map<String, ?> payload) {
        assertNonEmpty(couponUid, "Shop coupon UID is required.");
        return request(
            "PATCH",
            "/api/partner/v1/makepay/shop/coupons/" + urlEncode(couponUid),
            nullToEmpty(payload)
        );
    }

    public JsonNode archiveShopCoupon(String couponUid) {
        assertNonEmpty(couponUid, "Shop coupon UID is required.");
        return request("DELETE", "/api/partner/v1/makepay/shop/coupons/" + urlEncode(couponUid), null);
    }

    public JsonNode listShopOrders() {
        return listShopOrders(Collections.emptyMap());
    }

    public JsonNode listShopOrders(Map<String, ?> query) {
        return request("GET", "/api/partner/v1/makepay/shop/orders", null, query);
    }

    public JsonNode getBranding() {
        return request("GET", "/api/partner/v1/makepay/branding", null);
    }

    public JsonNode updateBranding(Map<String, ?> payload) {
        return request("PATCH", "/api/partner/v1/makepay/branding", nullToEmpty(payload));
    }

    public JsonNode refreshBrandingDomains() {
        return refreshBrandingDomains("all");
    }

    public JsonNode refreshBrandingDomains(String kind) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("kind", kind == null || kind.trim().isEmpty() ? "all" : kind.trim());
        return request("POST", "/api/partner/v1/makepay/branding/domains/refresh", body);
    }

    public JsonNode getBookkeepingSummary() {
        return request("GET", "/api/partner/v1/makepay/bookkeeping", null);
    }

    public JsonNode listBookkeepingInvoices() {
        return request("GET", "/api/partner/v1/makepay/bookkeeping/invoices", null);
    }

    public JsonNode createBookkeepingInvoice(Map<String, ?> payload) {
        return request("POST", "/api/partner/v1/makepay/bookkeeping/invoices", nullToEmpty(payload));
    }

    public JsonNode getBookkeepingInvoice(String invoiceId) {
        assertNonEmpty(invoiceId, "Bookkeeping invoice ID is required.");
        return request("GET", "/api/partner/v1/makepay/bookkeeping/invoices/" + urlEncode(invoiceId), null);
    }

    public JsonNode updateBookkeepingInvoice(String invoiceId, Map<String, ?> payload) {
        assertNonEmpty(invoiceId, "Bookkeeping invoice ID is required.");
        return request(
            "PATCH",
            "/api/partner/v1/makepay/bookkeeping/invoices/" + urlEncode(invoiceId),
            nullToEmpty(payload)
        );
    }

    public JsonNode createBookkeepingInvoicePaymentLink(String invoiceId) {
        return createBookkeepingInvoicePaymentLink(invoiceId, Collections.emptyMap());
    }

    public JsonNode createBookkeepingInvoicePaymentLink(String invoiceId, Map<String, ?> options) {
        assertNonEmpty(invoiceId, "Bookkeeping invoice ID is required.");
        return request(
            "POST",
            "/api/partner/v1/makepay/bookkeeping/invoices/" + urlEncode(invoiceId) + "/payment-link",
            nullToEmpty(options)
        );
    }

    public JsonNode listBookkeepingExpenses() {
        return request("GET", "/api/partner/v1/makepay/bookkeeping/expenses", null);
    }

    public JsonNode createBookkeepingExpense(Map<String, ?> payload) {
        return request("POST", "/api/partner/v1/makepay/bookkeeping/expenses", nullToEmpty(payload));
    }

    public JsonNode createBookkeepingExpenseFromActivity(Map<String, ?> payload) {
        return request("POST", "/api/partner/v1/makepay/bookkeeping/expenses/from-activity", nullToEmpty(payload));
    }

    public JsonNode getBookkeepingExpense(String expenseId) {
        assertNonEmpty(expenseId, "Bookkeeping expense ID is required.");
        return request("GET", "/api/partner/v1/makepay/bookkeeping/expenses/" + urlEncode(expenseId), null);
    }

    public JsonNode updateBookkeepingExpense(String expenseId, Map<String, ?> payload) {
        assertNonEmpty(expenseId, "Bookkeeping expense ID is required.");
        return request(
            "PATCH",
            "/api/partner/v1/makepay/bookkeeping/expenses/" + urlEncode(expenseId),
            nullToEmpty(payload)
        );
    }

    public JsonNode listBookkeepingDocuments() {
        return request("GET", "/api/partner/v1/makepay/bookkeeping/documents", null);
    }

    public JsonNode uploadBookkeepingDocument(Path file) {
        String fileName = file == null || file.getFileName() == null ? "document" : file.getFileName().toString();
        return uploadBookkeepingDocument(file, fileName, null, null, null);
    }

    public JsonNode uploadBookkeepingDocument(
        Path file,
        String fileName,
        String documentType,
        String invoiceId,
        String expenseId
    ) {
        if (file == null) {
            throw new MakePayError("Bookkeeping document file is required.", 400);
        }
        try {
            String contentType = Files.probeContentType(file);
            return uploadBookkeepingDocument(
                Files.readAllBytes(file),
                fileName,
                contentType == null ? "application/octet-stream" : contentType,
                documentType,
                invoiceId,
                expenseId
            );
        } catch (IOException error) {
            throw new MakePayError("Unable to read bookkeeping document file.", error);
        }
    }

    public JsonNode uploadBookkeepingDocument(
        byte[] file,
        String fileName,
        String contentType,
        String documentType,
        String invoiceId,
        String expenseId
    ) {
        if (file == null || file.length == 0) {
            throw new MakePayError("Bookkeeping document file is required.", 400);
        }

        String boundary = "----MakePayJava" + System.nanoTime();
        byte[] body = buildMultipartBody(
            boundary,
            file,
            orDefault(fileName, "document"),
            orDefault(contentType, "application/octet-stream"),
            documentType,
            invoiceId,
            expenseId
        );
        return requestMultipart("POST", "/api/partner/v1/makepay/bookkeeping/documents", body, boundary);
    }

    public JsonNode getBookkeepingDocumentDownloadUrl(String documentId) {
        assertNonEmpty(documentId, "Bookkeeping document ID is required.");
        return request(
            "GET",
            "/api/partner/v1/makepay/bookkeeping/documents/" + urlEncode(documentId) + "/download",
            null
        );
    }

    public JsonNode runBookkeepingDocumentOcr(String documentId) {
        assertNonEmpty(documentId, "Bookkeeping document ID is required.");
        return request(
            "POST",
            "/api/partner/v1/makepay/bookkeeping/documents/" + urlEncode(documentId) + "/ocr",
            Collections.emptyMap()
        );
    }

    public JsonNode createBookkeepingReconciliation(Map<String, ?> payload) {
        return request("POST", "/api/partner/v1/makepay/bookkeeping/reconciliation", nullToEmpty(payload));
    }

    public JsonNode getSettings() {
        return request("GET", "/api/partner/v1/makepay/settings", null);
    }

    public JsonNode updateSettings(Map<String, ?> settings) {
        return request("PUT", "/api/partner/v1/makepay/settings", nullToEmpty(settings));
    }

    public String hostedCheckoutUrl(String paymentUid) {
        return buildHostedCheckoutUrl(paymentUid, checkoutBaseUrl);
    }

    public String hostedDonationUrl(String donationSlug) {
        return buildHostedDonationUrl(donationSlug, checkoutBaseUrl);
    }

    public String embeddedCheckoutUrl(String paymentUid) {
        return buildEmbeddedCheckoutUrl(paymentUid, checkoutBaseUrl, null);
    }

    public String embeddedCheckoutUrl(String paymentUid, String parentOrigin) {
        return buildEmbeddedCheckoutUrl(paymentUid, checkoutBaseUrl, parentOrigin);
    }

    public String embeddedDonationUrl(String donationSlug) {
        return buildEmbeddedDonationUrl(donationSlug, checkoutBaseUrl, null);
    }

    public String embeddedDonationUrl(String donationSlug, String parentOrigin) {
        return buildEmbeddedDonationUrl(donationSlug, checkoutBaseUrl, parentOrigin);
    }

    public String modalScriptUrl() {
        return buildModalScriptUrl(checkoutBaseUrl);
    }

    public String embedButtonHtml(String paymentUid) {
        return embedButtonHtml(paymentUid, "Pay with crypto");
    }

    public String embedButtonHtml(String paymentUid, String buttonLabel) {
        return buildEmbedButtonHtml(paymentUid, checkoutBaseUrl, buttonLabel);
    }

    public String iframeHtml(String paymentUid) {
        return iframeHtml(paymentUid, "MakePay checkout");
    }

    public String iframeHtml(String paymentUid, String iframeTitle) {
        return buildIframeHtml(paymentUid, checkoutBaseUrl, iframeTitle, null);
    }

    public JsonNode request(String method, String path, Object body) {
        return request(method, path, body, Collections.emptyMap());
    }

    public JsonNode request(String method, String path, Object body, Map<String, ?> query) {
        URI uri = URI.create(baseUrl + path + buildQueryString(query));
        HttpRequest.Builder builder = authenticatedRequestBuilder(uri);

        String normalizedMethod = method.toUpperCase(Locale.ROOT);
        if (body != null && !"GET".equals(normalizedMethod)) {
            builder.header("Content-Type", "application/json");
            builder.method(normalizedMethod, HttpRequest.BodyPublishers.ofString(writeJson(body)));
        } else {
            builder.method(normalizedMethod, HttpRequest.BodyPublishers.noBody());
        }

        return send(builder.build());
    }

    public JsonNode requestMultipart(String method, String path, byte[] body, String boundary) {
        URI uri = URI.create(baseUrl + path);
        HttpRequest request = authenticatedRequestBuilder(uri)
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .method(method.toUpperCase(Locale.ROOT), HttpRequest.BodyPublishers.ofByteArray(body))
            .build();
        return send(request);
    }

    public static JsonNode createAnonymousPaymentLink(Map<String, ?> payload) {
        return createAnonymousPaymentLink(payload, DEFAULT_BASE_URL);
    }

    public static JsonNode createAnonymousPaymentLink(Map<String, ?> payload, String baseUrl) {
        return createAnonymousPaymentLink(payload, baseUrl, HttpClient.newHttpClient(), new ObjectMapper());
    }

    public static JsonNode createAnonymousPaymentLink(
        Map<String, ?> payload,
        String baseUrl,
        HttpClient httpClient,
        ObjectMapper objectMapper
    ) {
        ObjectMapper mapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        HttpClient client = httpClient == null ? HttpClient.newHttpClient() : httpClient;
        URI uri = URI.create(
            normalizeBaseUrl(orDefault(baseUrl, DEFAULT_BASE_URL)) + "/api/partner/v1/makepay/payment-links"
        );

        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .method("POST", HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(nullToEmpty(payload))))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return decodeResponse(response, mapper);
        } catch (IOException error) {
            throw new MakePayError("MakePay API request failed.", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new MakePayError("MakePay API request was interrupted.", error);
        }
    }

    public static JsonNode createAnonymousMakePayPaymentLink(Map<String, ?> payload) {
        return createAnonymousPaymentLink(payload);
    }

    public static JsonNode createAnonymousMakePayPaymentLink(Map<String, ?> payload, String baseUrl) {
        return createAnonymousPaymentLink(payload, baseUrl);
    }

    public static JsonNode createAnonymousMakePayPaymentLink(
        Map<String, ?> payload,
        String baseUrl,
        HttpClient httpClient,
        ObjectMapper objectMapper
    ) {
        return createAnonymousPaymentLink(payload, baseUrl, httpClient, objectMapper);
    }

    public static String buildHostedCheckoutUrl(String paymentUid) {
        return buildHostedCheckoutUrl(paymentUid, DEFAULT_CHECKOUT_BASE_URL);
    }

    public static String buildHostedCheckoutUrl(String paymentUid, String baseUrl) {
        assertNonEmpty(paymentUid, "Payment link UID is required.");
        return normalizeBaseUrl(orDefault(baseUrl, DEFAULT_CHECKOUT_BASE_URL)) + "/payment/" + urlEncode(paymentUid);
    }

    public static String buildHostedDonationUrl(String donationSlug) {
        return buildHostedDonationUrl(donationSlug, DEFAULT_CHECKOUT_BASE_URL);
    }

    public static String buildHostedDonationUrl(String donationSlug, String baseUrl) {
        assertNonEmpty(donationSlug, "Donation slug is required.");
        return normalizeBaseUrl(orDefault(baseUrl, DEFAULT_CHECKOUT_BASE_URL)) + "/donations/" + urlEncode(donationSlug);
    }

    public static String buildEmbeddedCheckoutUrl(String paymentUid) {
        return buildEmbeddedCheckoutUrl(paymentUid, DEFAULT_CHECKOUT_BASE_URL, null);
    }

    public static String buildEmbeddedCheckoutUrl(String paymentUid, String baseUrl, String parentOrigin) {
        assertNonEmpty(paymentUid, "Payment link UID is required.");
        return buildEmbeddedUrl("/embed/payment/", paymentUid, baseUrl, parentOrigin);
    }

    public static String buildEmbeddedDonationUrl(String donationSlug) {
        return buildEmbeddedDonationUrl(donationSlug, DEFAULT_CHECKOUT_BASE_URL, null);
    }

    public static String buildEmbeddedDonationUrl(String donationSlug, String baseUrl, String parentOrigin) {
        assertNonEmpty(donationSlug, "Donation slug is required.");
        return buildEmbeddedUrl("/embed/donations/", donationSlug, baseUrl, parentOrigin);
    }

    public static String buildModalScriptUrl() {
        return buildModalScriptUrl(DEFAULT_CHECKOUT_BASE_URL);
    }

    public static String buildModalScriptUrl(String baseUrl) {
        return normalizeBaseUrl(orDefault(baseUrl, DEFAULT_CHECKOUT_BASE_URL)) + "/modal/makepay.js";
    }

    public static String buildEmbedButtonHtml(String paymentUid, String baseUrl, String buttonLabel) {
        assertNonEmpty(paymentUid, "Payment link UID is required.");
        String resolvedLabel = buttonLabel == null ? "Pay with crypto" : buttonLabel;
        return "<script src=\"" + escapeHtmlAttribute(buildModalScriptUrl(baseUrl)) + "\"></script>\n"
            + "<button type=\"button\" data-makepay-payment-link=\"" + escapeHtmlAttribute(paymentUid) + "\">\n"
            + "  " + escapeHtmlText(resolvedLabel) + "\n"
            + "</button>";
    }

    public static String buildIframeHtml(
        String paymentUid,
        String baseUrl,
        String iframeTitle,
        String parentOrigin
    ) {
        String title = iframeTitle == null || iframeTitle.trim().isEmpty() ? "MakePay checkout" : iframeTitle;
        return "<iframe\n"
            + "  title=\"" + escapeHtmlAttribute(title) + "\"\n"
            + "  src=\"" + escapeHtmlAttribute(buildEmbeddedCheckoutUrl(paymentUid, baseUrl, parentOrigin)) + "\"\n"
            + "  style=\"width:100%;min-height:720px;border:0;border-radius:12px;\"\n"
            + "  allow=\"clipboard-read; clipboard-write\"\n"
            + "></iframe>";
    }

    private JsonNode createPaymentLikeLink(
        String path,
        Map<String, ?> payload,
        CreatePaymentLinkOptions options,
        String type
    ) {
        CreatePaymentLinkOptions resolvedOptions = options == null ? CreatePaymentLinkOptions.defaults() : options;
        Map<String, Object> resolvedPayload = new LinkedHashMap<>();
        if (payload != null) {
            resolvedPayload.putAll(payload);
        }
        if (type != null) {
            resolvedPayload.put("type", type);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", resolvedOptions.getStatus());
        body.put("sendPaymentRequestEmail", resolvedOptions.isSendPaymentRequestEmail());
        body.put("payload", resolvedPayload);
        return request("POST", path, body);
    }

    private HttpRequest.Builder authenticatedRequestBuilder(URI uri) {
        return HttpRequest.newBuilder(uri)
            .header("Accept", "application/json")
            .header("User-Agent", "MakePayJava/" + VERSION)
            .header("X-MakeCrypto-Key-Id", keyId)
            .header("X-MakeCrypto-Key-Secret", keySecret);
    }

    private JsonNode send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            return decodeResponse(response, objectMapper);
        } catch (IOException error) {
            throw new MakePayError("MakePay API request failed.", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new MakePayError("MakePay API request was interrupted.", error);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception error) {
            throw new MakePayError("Unable to encode MakePay request body as JSON.", 400, null, error);
        }
    }

    private static JsonNode decodeResponse(HttpResponse<String> response, ObjectMapper objectMapper) {
        JsonNode decoded = parseJsonOrEmpty(response.body(), objectMapper);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new MakePayError(readErrorMessage(decoded, response.statusCode()), response.statusCode(), decoded);
        }
        return decoded;
    }

    private static JsonNode parseJsonOrEmpty(String text, ObjectMapper objectMapper) {
        try {
            if (text == null || text.trim().isEmpty()) {
                return objectMapper.createObjectNode();
            }
            JsonNode decoded = objectMapper.readTree(text);
            return decoded == null ? objectMapper.createObjectNode() : decoded;
        } catch (Exception error) {
            return objectMapper.createObjectNode();
        }
    }

    private static String readErrorMessage(JsonNode decoded, int status) {
        if (decoded != null && decoded.hasNonNull("error")) {
            return decoded.get("error").asText();
        }
        if (decoded != null && decoded.hasNonNull("message")) {
            return decoded.get("message").asText();
        }
        return "MakePay API request failed with HTTP " + status + ".";
    }

    private static String buildEmbeddedUrl(String prefix, String uid, String baseUrl, String parentOrigin) {
        String url = normalizeBaseUrl(orDefault(baseUrl, DEFAULT_CHECKOUT_BASE_URL)) + prefix + urlEncode(uid);
        if (parentOrigin != null && !parentOrigin.trim().isEmpty()) {
            url += "?parentOrigin=" + urlEncode(parentOrigin.trim());
        }
        return url;
    }

    private static String buildQueryString(Map<String, ?> query) {
        if (query == null || query.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, ?> entry : query.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            builder.append(builder.length() == 0 ? "?" : "&");
            builder.append(urlEncode(entry.getKey())).append("=").append(urlEncode(String.valueOf(value)));
        }
        return builder.toString();
    }

    private static byte[] buildMultipartBody(
        String boundary,
        byte[] file,
        String fileName,
        String contentType,
        String documentType,
        String invoiceId,
        String expenseId
    ) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeFileField(out, boundary, "file", fileName, contentType, file);
        writeOptionalFormField(out, boundary, "documentType", documentType);
        writeOptionalFormField(out, boundary, "invoiceId", invoiceId);
        writeOptionalFormField(out, boundary, "expenseId", expenseId);
        writeText(out, "--" + boundary + "--\r\n");
        return out.toByteArray();
    }

    private static void writeOptionalFormField(ByteArrayOutputStream out, String boundary, String name, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        writeText(out, "--" + boundary + "\r\n");
        writeText(out, "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        writeText(out, value.trim() + "\r\n");
    }

    private static void writeFileField(
        ByteArrayOutputStream out,
        String boundary,
        String name,
        String fileName,
        String contentType,
        byte[] file
    ) {
        writeText(out, "--" + boundary + "\r\n");
        writeText(
            out,
            "Content-Disposition: form-data; name=\"" + name + "\"; filename=\""
                + escapeMultipartQuotedString(fileName) + "\"\r\n"
        );
        writeText(out, "Content-Type: " + contentType + "\r\n\r\n");
        out.write(file, 0, file.length);
        writeText(out, "\r\n");
    }

    private static void writeText(ByteArrayOutputStream out, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.write(bytes, 0, bytes.length);
    }

    private static String escapeMultipartQuotedString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void assertNonEmpty(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new MakePayError(message, 400);
        }
    }

    private static Map<String, ?> nullToEmpty(Map<String, ?> value) {
        return value == null ? Collections.emptyMap() : value;
    }

    private static String normalizeBaseUrl(String baseUrl) {
        return baseUrl.replaceAll("/+$", "");
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String escapeHtmlAttribute(String value) {
        return escapeHtmlText(value).replace("\"", "&quot;");
    }

    private static String escapeHtmlText(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }
}
