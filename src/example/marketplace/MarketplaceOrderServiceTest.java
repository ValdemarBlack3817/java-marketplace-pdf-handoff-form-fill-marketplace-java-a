package example.marketplace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MarketplaceOrderServiceTest {
    public static void main(String[] args) throws Exception {
        List<String> calls = new ArrayList<>();
        MarketplaceOrderService service = new MarketplaceOrderService("test-key", java.net.http.HttpClient.newHttpClient(),
                (method, path, body) -> { calls.add(method + " " + path + " " + body); return "{\"ok\":true}"; });
        var order = new MarketplaceOrderService.MarketplaceOrder("order-42",
                new MarketplaceOrderService.SellerAsset("seller-pdf"), "buyer@example.com", Map.of("buyer_email", "buyer@example.com"));
        var handoff = service.process(order);
        if (handoff.buyerUpdate() != MarketplaceOrderService.BuyerUpdate.RECEIPT_ARCHIVED) throw new AssertionError("handoff was not archived");
        if (!calls.get(0).startsWith("POST /v1/pdf/form/fill")) throw new AssertionError("form fill was skipped");
        if (!calls.get(0).contains("\"flatten\":true")) throw new AssertionError("form was not flattened");
        if (!calls.get(1).startsWith("PUT /v1/storage/object/put/marketplace-orders/order-42.pdf")) throw new AssertionError("archive was skipped");
        System.out.println("PASS: flattened order is archived before buyer handoff");
    }
}
