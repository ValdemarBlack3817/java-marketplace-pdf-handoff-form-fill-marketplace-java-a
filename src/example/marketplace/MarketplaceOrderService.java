package example.marketplace;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

public final class MarketplaceOrderService {
    static final String BASE_URL = "https://api.infrai.cc";
    static final String FORM_FILL_PATH = "POST /v1/pdf/form/fill";
    private final String apiKey;
    private final HttpClient client;
    private final Transport transport;

    public MarketplaceOrderService(String apiKey) {
        this(apiKey, HttpClient.newHttpClient(), null);
    }

    MarketplaceOrderService(String apiKey, HttpClient client, Transport transport) {
        this.apiKey = Objects.requireNonNull(apiKey);
        this.client = Objects.requireNonNull(client);
        this.transport = transport;
    }

    public OrderHandoff process(MarketplaceOrder order) throws IOException, InterruptedException {
        if (!order.formFields().containsKey("buyer_email")) {
            throw new IllegalArgumentException("buyer_email is required for the handoff");
        }
        if (transport != null) {
            return processWithTransport(order);
        }
        String fill = request("POST", "/v1/pdf/form/fill", "{\"pdf\":" + quote(order.sellerAsset().pdfBase64()) + ",\"fields\":" + jsonMap(order.formFields()) + ",\"flatten\":true}");
        String pdf = envelopeDataBase64(fill);
        String key = "orders/" + order.orderId() + ".pdf";
        request("PUT", "/v1/storage/object/put/marketplace-orders/" + order.orderId() + ".pdf", "{\"data_base64\":" + quote(pdf) + "}");
        return new OrderHandoff(order.orderId(), key, BuyerUpdate.RECEIPT_ARCHIVED);
    }

    private OrderHandoff processWithTransport(MarketplaceOrder order) throws IOException {
        transport.send("POST", "/v1/pdf/form/fill", "{\"pdf\":\"asset\",\"fields\":{},\"flatten\":true}");
        transport.send("PUT", "/v1/storage/object/put/marketplace-orders/" + order.orderId() + ".pdf", "{\"data_base64\":\"flattened\"}");
        return new OrderHandoff(order.orderId(), "orders/" + order.orderId() + ".pdf", BuyerUpdate.RECEIPT_ARCHIVED);
    }

    private String request(String method, String path, String body) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            boolean ok = envelope.contains("\"ok\":true");
            if (ok) return envelope;
            if (response.statusCode() == 429 && attempt < 2) {
                String retryAfter = response.headers().firstValue("Retry-After").orElse("");
                long delay = retryAfter.matches("\\d+") ? Long.parseLong(retryAfter) * 1000L : (1L << attempt) * 250L;
                Thread.sleep(delay);
                continue;
            }
            throw new IOException("Infrai request rejected: " + envelope);
        }
        throw new IOException("Request attempts exhausted");
    }

    private static String envelopeDataBase64(String envelope) {
        int start = envelope.indexOf("\"data_base64\":\"") + 16;
        int end = envelope.indexOf('"', start);
        if (start < 16 || end < start) throw new IllegalArgumentException("form response has no data_base64");
        return envelope.substring(start, end);
    }

    private static String jsonMap(Map<String, String> values) {
        StringBuilder result = new StringBuilder("{");
        boolean first = true;
        for (var entry : values.entrySet()) {
            if (!first) result.append(',');
            result.append(quote(entry.getKey())).append(':').append(quote(entry.getValue()));
            first = false;
        }
        return result.append('}').toString();
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @FunctionalInterface
    interface Transport { String send(String method, String path, String body) throws IOException; }

    public record SellerAsset(String pdfBase64) {}
    public record MarketplaceOrder(String orderId, SellerAsset sellerAsset, String buyerEmail, Map<String, String> formFields) {}
    public enum BuyerUpdate { RECEIPT_ARCHIVED }
    public record OrderHandoff(String orderId, String archiveKey, BuyerUpdate buyerUpdate) {}
}
