# MakePay Java SDK

Official Java SDK for MakePay server-side integrations. Use it to create crypto
payment links, donation pages, invoices, bookkeeping records, subscriptions,
POS terminals, products, Simple Shop storefronts, customer portals, branded
domains, and signed webhook handlers.

Public source: `https://github.com/makecryptoio/makepay-java-sdk`

## Install

```kotlin
dependencies {
    implementation("io.makecrypto:makepay:0.3.0")
}
```

```xml
<dependency>
  <groupId>io.makecrypto</groupId>
  <artifactId>makepay</artifactId>
  <version>0.3.0</version>
</dependency>
```

The SDK targets Java 11 or newer and uses the standard JDK HTTP client.

## Configure

Create a MakePay API key in MakeCrypto and keep the secret on your server only.

```java
import io.makecrypto.makepay.MakePayClient;
import io.makecrypto.makepay.MakePayClientOptions;

MakePayClient makepay = new MakePayClient(
    MakePayClientOptions.builder()
        .keyId(System.getenv("MAKEPAY_KEY_ID"))
        .keySecret(System.getenv("MAKEPAY_KEY_SECRET"))
        .build()
);
```

The client sends `X-MakeCrypto-Key-Id` and `X-MakeCrypto-Key-Secret` headers to
the MakePay partner API.

## Payment Links

```java
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

JsonNode response = makepay.createPaymentLink(Map.of(
    "title", "Order #1042",
    "description", "Checkout for order #1042",
    "amount", "129.99",
    "currency", "USDT",
    "orderId", "order_1042",
    "customerEmail", "buyer@example.com",
    "returnUrl", "https://merchant.example/orders/1042",
    "successUrl", "https://merchant.example/orders/1042/success",
    "failureUrl", "https://merchant.example/orders/1042/pay",
    "expirationTime", "12h"
));

String publicUrl = response.path("paymentLink").path("publicUrl").asText();
```

Read, update, and email existing links:

```java
makepay.listPaymentLinks();
makepay.getPaymentLink("PAYMENT_LINK_UID");
makepay.updatePaymentLink("PAYMENT_LINK_UID", Map.of("status", "paused"));
makepay.sendPaymentRequestEmail("PAYMENT_LINK_UID", "buyer@example.com");
```

## Donations

Donation pages are flexible-amount payment links with a public donation slug.

```java
JsonNode donation = makepay.createDonationLink(Map.of(
    "title", "Spring campaign",
    "description", "Support the 2026 spring fundraiser.",
    "defaultAmountUsd", "25",
    "minimumAmountUsd", "5",
    "donationSlug", "spring-campaign"
));

makepay.listDonationLinks();
makepay.getDonationLink("DONATION_UID");
makepay.updateDonationLink("DONATION_UID", Map.of("status", "paused"));
```

## Anonymous Payment Links

Anonymous links do not use a MakePay API key. They require an explicit
settlement route because MakePay cannot read merchant wallet settings.

```java
import java.util.List;

JsonNode anonymous = MakePayClient.createAnonymousPaymentLink(Map.of(
    "amount", "25",
    "settlement", Map.of(
        "currency", "USDT",
        "priorities", List.of(Map.of(
            "chain", "ETH",
            "address", "0xYourSettlementWallet",
            "asset", "ETH.USDT-0xdAC17F958D2ee523a2206206994597C13D831ec7"
        ))
    ),
    "title", "Invoice #1042",
    "webhookUrl", "https://merchant.example/webhooks/makepay"
));
```

## Checkout URLs And Embeds

Use hosted checkout for redirects, or the embed helpers when your frontend keeps
the shopper on the merchant site.

```java
String paymentUid = response.path("paymentLink").path("uid").asText();

String hostedUrl = makepay.hostedCheckoutUrl(paymentUid);
String embedUrl = makepay.embeddedCheckoutUrl(
    paymentUid,
    "https://merchant.example"
);
String buttonHtml = makepay.embedButtonHtml(paymentUid, "Pay with crypto");
String iframeHtml = makepay.iframeHtml(paymentUid, "Secure MakePay checkout");

String donationHostedUrl = makepay.hostedDonationUrl("spring-campaign");
String donationEmbedUrl = makepay.embeddedDonationUrl(
    "spring-campaign",
    "https://merchant.example"
);
```

## Customers And Subscriptions

```java
makepay.upsertCustomer(Map.of(
    "email", "buyer@example.com",
    "name", "Buyer Example",
    "clientId", "crm_123"
));

makepay.createCustomerPortal("CUSTOMER_ID", Map.of(
    "returnUrl", "https://merchant.example/account"
));

makepay.createSubscription(Map.of(
    "amountUsd", "29",
    "customerEmail", "buyer@example.com",
    "label", "Monthly plan",
    "billingIntervalUnit", "month",
    "billingIntervalCount", 1
));
```

## POS, Products, And Simple Shop

```java
JsonNode terminal = makepay.createPosTerminal(Map.of(
    "name", "Front counter",
    "pin", "1234",
    "catalogEnabled", true
));

makepay.createProduct(Map.of(
    "name", "Digital guide",
    "productType", "digital",
    "basePriceUsd", "19",
    "shopSlug", "digital-guide",
    "images", List.of(Map.of(
        "url", "https://merchant.example/guide.png",
        "alt", "Guide cover"
    )),
    "variants", List.of(Map.of("name", "PDF", "priceUsd", "19"))
));

makepay.createProductDownload("PRODUCT_UID", Map.of(
    "fileName", "guide.pdf",
    "contentType", "application/pdf",
    "url", "https://merchant.example/downloads/guide.pdf"
));

makepay.updateShop(Map.of(
    "slug", "merchant-shop",
    "displayCurrency", "USD",
    "checkoutMode", "hosted"
));

makepay.updateShopDomain("shop.merchant.example");
makepay.refreshShopDomain();
makepay.createShopCoupon(Map.of(
    "code", "SPRING10",
    "discountType", "percent",
    "value", "10"
));
makepay.listShopOrders(Map.of("status", "paid", "limit", 25));
```

## Invoices And Bookkeeping

```java
JsonNode invoice = makepay.createBookkeepingInvoice(Map.of(
    "title", "Invoice #1042",
    "currency", "USD",
    "issueDate", "2026-05-15",
    "dueDate", "2026-05-30",
    "counterparty", Map.of(
        "name", "Buyer Example",
        "email", "buyer@example.com"
    ),
    "lineItems", List.of(Map.of(
        "description", "Implementation services",
        "quantity", "1",
        "unitAmount", "500"
    )),
    "metadata", Map.of("orderId", "order_1042")
));

makepay.createBookkeepingInvoicePaymentLink("INVOICE_UID", Map.of(
    "sendPaymentRequestEmail", true
));

makepay.createBookkeepingExpense(Map.of(
    "title", "Hosting",
    "amount", "49",
    "currency", "USD",
    "incurredOn", "2026-05-15"
));

makepay.uploadBookkeepingDocument(
    Path.of("receipt.pdf"),
    "receipt.pdf",
    "receipt",
    null,
    "EXPENSE_UID"
);

makepay.getBookkeepingSummary();
```

## Branding, Settings, And Operations

```java
makepay.updateBranding(Map.of(
    "brandName", "Merchant",
    "websiteUrl", "https://merchant.example",
    "supportEmail", "support@merchant.example",
    "brandingAccentColor", "#14b8a6",
    "paymentLinkDomain", "pay.merchant.example",
    "emailSendingDomain", "mail.merchant.example"
));

makepay.getBranding();
makepay.refreshBrandingDomains("all");

makepay.getSettings();
makepay.updateSettings(Map.of(
    "callbackUrl", "https://merchant.example/webhooks/makepay"
));

makepay.listDestinationAssets();
makepay.listWebhookRequests(Map.of("limit", 25));
```

## Verify Webhooks

Read the exact raw body before parsing JSON.

```java
import io.makecrypto.makepay.MakePayWebhook;

String rawBody = requestBodyString;
String signature = request.getHeader("x-makepay-signature");

JsonNode event = MakePayWebhook.parse(
    rawBody,
    signature,
    System.getenv("MAKEPAY_WEBHOOK_SECRET")
);

if ("status_changed".equals(event.path("event").path("type").asText())) {
    // Update your local order.
}
```

Use `MakePayWebhook.verify(...)` when you only need a boolean result.

## Release

The package is published to Maven Central as `io.makecrypto:makepay`. The
GitHub workflow `Publish MakePay Java SDK` builds, signs, creates a Central
Portal deployment bundle, uploads it to Central Portal, and waits for the
deployment to finish.

The canonical monorepo source lives in `apps/plugins/java-sdk`. The public
repository at `https://github.com/makecryptoio/makepay-java-sdk` mirrors only
the SDK files so developers can install or inspect it without the full
MakeCrypto workspace.

Required GitHub secrets:

- `CENTRAL_PORTAL_USERNAME`
- `CENTRAL_PORTAL_PASSWORD`
- `MAVEN_SIGNING_KEY`
- `MAVEN_SIGNING_PASSWORD`
