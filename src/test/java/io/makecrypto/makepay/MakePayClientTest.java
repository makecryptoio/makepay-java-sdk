package io.makecrypto.makepay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MakePayClientTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void createsPaymentLinksAndSendsAuthenticationHeaders() throws Exception {
        List<CapturedRequest> capturedRequests = Collections.synchronizedList(new ArrayList<>());
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> handleJson(exchange, capturedRequests));
        server.start();

        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            MakePayClient client = new MakePayClient(
                MakePayClientOptions.builder()
                    .baseUrl(baseUrl)
                    .keyId("mk_test")
                    .keySecret("mksec_test")
                    .build()
            );

            JsonNode response = client.createPaymentLink(Map.of(
                "amount", "12.50",
                "currency", "USDT"
            ));

            assertTrue(response.path("ok").asBoolean());
            CapturedRequest request = capturedRequests.get(0);
            assertEquals("POST", request.method);
            assertEquals("/api/partner/v1/makepay/payment-links", request.path);
            assertEquals("mk_test", request.header("X-MakeCrypto-Key-Id"));
            assertEquals("mksec_test", request.header("X-MakeCrypto-Key-Secret"));
            assertTrue(request.header("User-Agent").startsWith("MakePayJava/0.3.0"));
            assertEquals("12.50", MAPPER.readTree(request.body).path("payload").path("amount").asText());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void coversMakePay030ApiRoutes() throws Exception {
        List<CapturedRequest> capturedRequests = Collections.synchronizedList(new ArrayList<>());
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> handleJson(exchange, capturedRequests));
        server.start();

        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            MakePayClient client = new MakePayClient(
                MakePayClientOptions.builder()
                    .baseUrl(baseUrl)
                    .keyId("mk_test")
                    .keySecret("mksec_test")
                    .build()
            );

            client.createDonationLink(Map.of(
                "title", "Spring campaign",
                "defaultAmountUsd", "25",
                "donationSlug", "spring-campaign"
            ));
            client.listDonationLinks();
            client.getDonationLink("don_123");
            client.updateDonationLink("don_123", Map.of("status", "paused"));
            client.listCustomers();
            client.upsertCustomer(Map.of("email", "buyer@example.com", "name", "Buyer"));
            client.createCustomerPortal("cus_123", Map.of("returnUrl", "https://merchant.example"));
            client.listSubscriptions();
            client.createSubscription(Map.of("amountUsd", "20", "customerEmail", "buyer@example.com"));
            client.listDestinationAssets();

            Map<String, Object> webhookQuery = new LinkedHashMap<>();
            webhookQuery.put("limit", 10);
            client.listWebhookRequests(webhookQuery);

            client.listPosTerminals();
            client.createPosTerminal(Map.of("name", "Front counter", "pin", "1234"));
            client.getPosTerminal("pos_123");
            client.updatePosTerminal("pos_123", Map.of("name", "Front counter", "pin", "1234"));
            client.listProducts();
            client.createProduct(Map.of("name", "Sticker", "basePriceUsd", "10"));
            client.getProduct("prod_123");
            client.updateProduct("prod_123", Map.of("name", "Sticker", "status", "active"));
            client.listProductDownloads("prod_123");
            client.createProductDownload("prod_123", Map.of("fileName", "guide.pdf"));
            client.getShop();
            client.updateShop(Map.of("slug", "demo-shop", "displayCurrency", "USD"));
            client.getShopBuilder();
            client.updateShopBuilder(Map.of("blocks", List.of()));
            client.getShopDomain();
            client.updateShopDomain("shop.example.com");
            client.refreshShopDomain();
            client.listShopCoupons();
            client.createShopCoupon(Map.of("code", "SPRING10", "discountType", "percent", "value", "10"));
            client.updateShopCoupon("coupon_123", Map.of("status", "archived"));
            client.archiveShopCoupon("coupon_123");

            Map<String, Object> shopOrdersQuery = new LinkedHashMap<>();
            shopOrdersQuery.put("limit", 25);
            shopOrdersQuery.put("status", "paid");
            client.listShopOrders(shopOrdersQuery);

            client.getBranding();
            client.updateBranding(Map.of(
                "brandName", "Merchant",
                "brandingBrandColor", "#101010",
                "paymentLinkDomain", "pay.example.com"
            ));
            client.refreshBrandingDomains("payment-link");
            client.getBookkeepingSummary();
            client.listBookkeepingInvoices();
            client.createBookkeepingInvoice(Map.of(
                "title", "Invoice #1042",
                "currency", "USD",
                "counterparty", Map.of("email", "buyer@example.com", "name", "Buyer")
            ));
            client.getBookkeepingInvoice("inv_123");
            client.updateBookkeepingInvoice("inv_123", Map.of("status", "open"));
            client.createBookkeepingInvoicePaymentLink("inv_123", Map.of("sendPaymentRequestEmail", true));
            client.listBookkeepingExpenses();
            client.createBookkeepingExpense(Map.of("title", "Hosting", "amount", "49", "currency", "USD"));
            client.createBookkeepingExpenseFromActivity(Map.of("walletActivityEventKey", "chain_event_123"));
            client.getBookkeepingExpense("exp_123");
            client.updateBookkeepingExpense("exp_123", Map.of("status", "approved"));
            client.listBookkeepingDocuments();
            client.uploadBookkeepingDocument(
                "receipt".getBytes(StandardCharsets.UTF_8),
                "receipt.pdf",
                "application/pdf",
                "receipt",
                null,
                "exp_123"
            );
            client.getBookkeepingDocumentDownloadUrl("doc_123");
            client.runBookkeepingDocumentOcr("doc_123");
            client.createBookkeepingReconciliation(Map.of(
                "invoiceId", "inv_123",
                "paymentSessionId", "session_123",
                "linkType", "payment"
            ));

            int anonymousRequestIndex = capturedRequests.size();
            MakePayClient.createAnonymousPaymentLink(
                Map.of("amount", "5"),
                baseUrl
            );

            List<String> requestRoutes = capturedRequests.stream()
                .map(CapturedRequest::route)
                .collect(Collectors.toList());
            assertTrue(requestRoutes.contains("POST /api/partner/v1/makepay/donations"));
            assertTrue(requestRoutes.contains("GET /api/partner/v1/makepay/donations"));
            assertTrue(requestRoutes.contains("GET /api/partner/v1/makepay/customers"));
            assertTrue(requestRoutes.contains("POST /api/partner/v1/makepay/subscriptions"));
            assertTrue(requestRoutes.contains("GET /api/partner/v1/makepay/destination-assets"));
            assertTrue(requestRoutes.contains("GET /api/partner/v1/makepay/webhook-requests?limit=10"));
            assertTrue(requestRoutes.contains("POST /api/partner/v1/makepay/pos-terminals"));
            assertTrue(requestRoutes.contains("PATCH /api/partner/v1/makepay/pos-terminals/pos_123"));
            assertTrue(requestRoutes.contains("POST /api/partner/v1/makepay/products"));
            assertTrue(requestRoutes.contains("PATCH /api/partner/v1/makepay/products/prod_123"));
            assertTrue(requestRoutes.contains("POST /api/partner/v1/makepay/shop/products/prod_123/downloads"));
            assertTrue(requestRoutes.contains("PATCH /api/partner/v1/makepay/shop"));
            assertTrue(requestRoutes.contains("PUT /api/partner/v1/makepay/shop/builder"));
            assertTrue(requestRoutes.contains("PUT /api/partner/v1/makepay/shop/domains"));
            assertTrue(requestRoutes.contains("POST /api/partner/v1/makepay/shop/domains"));
            assertTrue(requestRoutes.contains("POST /api/partner/v1/makepay/shop/coupons"));
            assertTrue(requestRoutes.contains("DELETE /api/partner/v1/makepay/shop/coupons/coupon_123"));
            assertTrue(requestRoutes.contains("GET /api/partner/v1/makepay/shop/orders?limit=25&status=paid"));
            assertTrue(requestRoutes.contains("GET /api/partner/v1/makepay/branding"));
            assertTrue(requestRoutes.contains("PATCH /api/partner/v1/makepay/branding"));
            assertTrue(requestRoutes.contains("POST /api/partner/v1/makepay/branding/domains/refresh"));
            assertTrue(requestRoutes.contains("GET /api/partner/v1/makepay/bookkeeping"));
            assertTrue(requestRoutes.contains("POST /api/partner/v1/makepay/bookkeeping/invoices"));
            assertTrue(requestRoutes.contains("PATCH /api/partner/v1/makepay/bookkeeping/invoices/inv_123"));
            assertTrue(requestRoutes.contains("POST /api/partner/v1/makepay/bookkeeping/invoices/inv_123/payment-link"));
            assertTrue(requestRoutes.contains("POST /api/partner/v1/makepay/bookkeeping/expenses"));
            assertTrue(requestRoutes.contains("POST /api/partner/v1/makepay/bookkeeping/expenses/from-activity"));
            assertTrue(requestRoutes.contains("PATCH /api/partner/v1/makepay/bookkeeping/expenses/exp_123"));
            assertTrue(requestRoutes.contains("POST /api/partner/v1/makepay/bookkeeping/documents"));
            assertTrue(requestRoutes.contains("GET /api/partner/v1/makepay/bookkeeping/documents/doc_123/download"));
            assertTrue(requestRoutes.contains("POST /api/partner/v1/makepay/bookkeeping/documents/doc_123/ocr"));
            assertTrue(requestRoutes.contains("POST /api/partner/v1/makepay/bookkeeping/reconciliation"));

            CapturedRequest documentUploadRequest = capturedRequests.stream()
                .filter(request -> request.route().equals("POST /api/partner/v1/makepay/bookkeeping/documents"))
                .filter(request -> request.header("Content-Type").startsWith("multipart/form-data; boundary="))
                .findFirst()
                .orElseThrow();
            assertTrue(documentUploadRequest.body.contains("name=\"documentType\""));
            assertTrue(documentUploadRequest.body.contains("receipt"));

            CapturedRequest anonymousRequest = capturedRequests.get(anonymousRequestIndex);
            assertEquals("POST /api/partner/v1/makepay/payment-links", anonymousRequest.route());
            assertEquals("", anonymousRequest.header("X-MakeCrypto-Key-Id"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void buildsCheckoutUrlsAndHtml() {
        MakePayClient client = new MakePayClient(
            MakePayClientOptions.builder()
                .keyId("mk_test")
                .keySecret("mksec_test")
                .checkoutBaseUrl("https://checkout.example/")
                .build()
        );

        assertEquals("https://checkout.example/payment/pay_123", client.hostedCheckoutUrl("pay_123"));
        assertEquals("https://checkout.example/donations/spring-campaign", client.hostedDonationUrl("spring-campaign"));
        assertEquals(
            "https://checkout.example/embed/payment/pay_123?parentOrigin=https%3A%2F%2Fmerchant.example",
            client.embeddedCheckoutUrl("pay_123", "https://merchant.example")
        );
        assertEquals(
            "https://checkout.example/embed/donations/spring-campaign?parentOrigin=https%3A%2F%2Fmerchant.example",
            client.embeddedDonationUrl("spring-campaign", "https://merchant.example")
        );
        assertEquals("https://checkout.example/modal/makepay.js", client.modalScriptUrl());
        assertTrue(client.embedButtonHtml("pay_\"<&", "Pay <now>")
            .contains("data-makepay-payment-link=\"pay_&quot;&lt;&amp;\""));
        assertTrue(client.iframeHtml("pay_123", "Secure checkout")
            .contains("src=\"https://checkout.example/embed/payment/pay_123\""));
    }

    @Test
    void throwsMakePayErrorWhenCredentialsAreMissing() {
        RuntimeException error = assertThrows(
            RuntimeException.class,
            () -> new MakePayClient(MakePayClientOptions.builder().keyId("").keySecret("").build())
        );

        assertInstanceOf(MakePayError.class, error);
    }

    private static void handleJson(
        HttpExchange exchange,
        List<CapturedRequest> capturedRequests
    ) throws IOException {
        byte[] requestBody = exchange.getRequestBody().readAllBytes();
        capturedRequests.add(new CapturedRequest(
            exchange.getRequestMethod(),
            exchange.getRequestURI().getPath(),
            exchange.getRequestURI().getRawQuery(),
            new LinkedHashMap<>(exchange.getRequestHeaders()),
            new String(requestBody, StandardCharsets.UTF_8)
        ));

        byte[] responseBody = (
            "{\"ok\":true,"
                + "\"paymentLink\":{\"uid\":\"pay_123\",\"publicUrl\":\"https://makepay.io/payment/pay_123\"},"
                + "\"terminal\":{\"uid\":\"pos_123\"}}"
        ).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(201, responseBody.length);
        exchange.getResponseBody().write(responseBody);
        exchange.close();
    }

    private static final class CapturedRequest {
        private final String method;
        private final String path;
        private final String query;
        private final Map<String, List<String>> headers;
        private final String body;

        private CapturedRequest(
            String method,
            String path,
            String query,
            Map<String, List<String>> headers,
            String body
        ) {
            this.method = method;
            this.path = path;
            this.query = query;
            this.headers = headers;
            this.body = body;
        }

        private String route() {
            return method + " " + path + (query == null || query.isEmpty() ? "" : "?" + query);
        }

        private String header(String name) {
            return headers.entrySet()
                .stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .flatMap(entry -> entry.getValue().stream())
                .findFirst()
                .orElse("");
        }
    }
}
