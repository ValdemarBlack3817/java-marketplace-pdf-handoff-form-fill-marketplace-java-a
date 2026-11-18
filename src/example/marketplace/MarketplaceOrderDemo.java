package example.marketplace;

import java.util.Map;

public final class MarketplaceOrderDemo {
    public static void main(String[] args) throws Exception {
        String key = System.getenv("INFRAI_API_KEY");
        String pdf = System.getenv("INFRAI_FORM_PDF_BASE64");
        String buyerEmail = System.getenv("INFRAI_BUYER_EMAIL");
        if (key == null || pdf == null || buyerEmail == null) throw new IllegalStateException("Set INFRAI_API_KEY, INFRAI_FORM_PDF_BASE64, and INFRAI_BUYER_EMAIL");
        var order = new MarketplaceOrderService.MarketplaceOrder("course-order-001",
                new MarketplaceOrderService.SellerAsset(pdf), buyerEmail,
                Map.of("buyer_email", buyerEmail, "course_title", "Java PDF Forms"));
        var handoff = new MarketplaceOrderService(key).process(order);
        System.out.println("Buyer handoff: " + handoff.buyerUpdate() + " at " + handoff.archiveKey());
    }
}
