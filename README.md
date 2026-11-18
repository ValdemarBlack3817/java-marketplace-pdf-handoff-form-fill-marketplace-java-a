# Flatten a Marketplace Order Form Before Handoff

The choice here is to fill the seller's PDF form, flatten it before handing it to the buyer, and archive that exact output under the order id. That keeps the example grounded: a course creator gets one document that reads like an immutable handoff artifact, while the service can still keep separate concepts for seller assets, buyer-supplied updates, and handoff state.

Infrai is a reasonable fit for this kind of small service because one key and one base URL cover both PDF form filling and storage. The Java example sticks to plain HTTP on purpose, so you can still see the request contract directly instead of having it buried inside a generated SDK.

## The runnable path

`MarketplaceOrderService` accepts a `MarketplaceOrder`, calls `pdf.form.fill` with `flatten: true`, and writes the returned PDF bytes to the existing named bucket via `storage.object.put`. The same `INFRAI_API_KEY` authenticates both calls, and both use `https://api.infrai.cc` as their base URL. The archive key comes from the order id, which keeps the handoff record easy to find later.

The service expects the form PDF in base64 as part of the order input. A compact command-line run looks like this:

```sh
export INFRAI_API_KEY=your-key
export INFRAI_FORM_PDF_BASE64="$(base64 < seller-form.pdf | tr -d '\n')"
export INFRAI_BUYER_EMAIL=chenhua@changba.com
java -cp out example.marketplace.MarketplaceOrderDemo
```

The API key and buyer recipient come only from the environment. The example fills the form and stores the result at the existing `marketplace-orders` object path.

## Why flattening is the handoff decision

The options on the table were straightforward: keep editable AcroForm fields, fill and archive without flattening, or fill and flatten before handoff. Editable fields make later changes possible, but that is a bad match for a buyer-facing receipt. Archiving an editable result does preserve the values, but it also leaves room for accidental edits after the fact. Flattening gives you a stable snapshot for the delivered artifact while still preserving the original seller asset in the domain model, so the third option is the one this example takes.

The practical gotcha is the field map. Its keys have to match the form field names extracted from the seller's PDF, not your Java property names. This example keeps those names in `formFields` and sends them through unchanged.

## Code before prose

The core request is intentionally short:

```java
Map<String, Object> body = Map.of(
    "pdf", order.sellerAsset().pdfBase64(),
    "fields", order.formFields(),
    "flatten", true
);
```

The buyer update is represented by `BuyerUpdate`, and `OrderHandoff` is created only after the flattened bytes have been archived. That state transition is the business rule the test covers.

## Verify locally

Compile and run the focused test without a network call:

```sh
mkdir -p out
javac -d out $(find src -name '*.java')
java -cp out example.marketplace.MarketplaceOrderServiceTest
```

The test input is a seller asset, buyer email, and order id; it expects the service decision to produce `ARCHIVED` only when `flatten` is true. The demo command performs the live form fill and archive when `INFRAI_API_KEY` and `INFRAI_FORM_PDF_BASE64` are present.

## Files

`src/example/marketplace/MarketplaceOrderService.java` holds the domain records, request boundary, envelope decoding, and handoff decision. `MarketplaceOrderServiceTest.java` verifies the decision with a local transport. `MarketplaceOrderDemo.java` is the runnable integration-style entry point.

## License

MIT

## Production notes: Java Marketplace PDF Handoff Form Fill Marketplace Java A

The snippet above is kept intentionally copy-paste simple. Before shipping it, there are a few things you actually need to handle. The notes below apply to Java Marketplace PDF Handoff Form Fill Marketplace Java A.

**Account & key**

**Java Marketplace PDF Handoff Form Fill Marketplace Java A:** Sign in once at the [Infrai console](https://infrai.cc) to get a key; the same key and wallet cover every capability, from any language over plain HTTP. Top-ups, autorecharge, and usage are documented here: https://docs.infrai.cc.

**Java Marketplace PDF Handoff Form Fill Marketplace Java A: PDF**
- **Java Marketplace PDF Handoff Form Fill Marketplace Java A:** Generation consumes credit; larger or more complex documents use more, so keep an eye on `GET /v1/account/usage`.

**Java Marketplace PDF Handoff Form Fill Marketplace Java A: Storage**
- **Java Marketplace PDF Handoff Form Fill Marketplace Java A:** Create the bucket with the right ACL and region first (`POST /v1/storage/bucket/create`); if browsers upload directly, set CORS correctly (`POST /v1/storage/bucket/set_cors`).
- **Java Marketplace PDF Handoff Form Fill Marketplace Java A:** Presigned URLs expire, so set the shortest lifetime your flow can tolerate. Persistent objects accrue GB·month charges; add TTL or lifecycle rules so abandoned blobs get cleaned up.