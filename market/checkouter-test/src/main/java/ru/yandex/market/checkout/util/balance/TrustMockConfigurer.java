package ru.yandex.market.checkout.util.balance;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import ru.yandex.market.checkout.checkouter.balance.BasketStatus;
import ru.yandex.market.checkout.checkouter.balance.trust.rest.AbstractTrustRestApi;
import ru.yandex.market.checkout.checkouter.pay.TrustBasketKey;
import ru.yandex.market.checkout.util.GenericMockHelper;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.request;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.google.common.base.Throwables.propagate;
import static java.lang.Integer.toHexString;
import static java.util.Arrays.stream;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.yandex.market.checkout.util.balance.BasketResponseTransformer.BASKET_STATE_RESPONSE;
import static ru.yandex.market.checkout.util.balance.BasketResponseTransformer.CONFIG;
import static ru.yandex.market.checkout.util.balance.BasketResponseTransformer.RESPONSE_STATUS;
import static ru.yandex.market.checkout.util.balance.BasketStatusResponseTransformer.BASKET_STATUS_STATE_RESPONSE;
import static ru.yandex.market.checkout.util.balance.ResponseVariable.BASKET_ID;
import static ru.yandex.market.checkout.util.balance.ResponseVariable.BASKET_STATUS;
import static ru.yandex.market.checkout.util.balance.ResponseVariable.PURCHASE_TOKEN;
import static ru.yandex.market.checkout.util.balance.ResponseVariable.TRUST_REFUND_ID;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildWithBasketKeyConfig;

@TestComponent
public class TrustMockConfigurer {

    public static final String CHECK_BASKET_STATUS_CODE = "payment_timeout";
    public static final String CHECK_BASKET_STATUS_DESC = "timeout while waiting for success";

    public static final String TRUST_URL = "/trust-payments/v2";
    public static final String TRUST_PAYMENTS_CREATE_BASKET_URL = "/trust-payments/v2/payments?show_trust_payment_id" +
            "=true";
    public static final String LOAD_PARTNER_STUB = "LoadPartner";
    public static final String CREATE_PRODUCT_STUB = "CreateServiceProduct";
    public static final String CREATE_ORDERS_STUB = "CreateOrdersBatch";
    public static final String CREATE_BASKET_STUB = "CreateBasket";
    public static final String CHECK_BASKET_STUB = "CheckBasket";
    public static final String GET_BASKET_STUB = "GetBasket";
    public static final String PAY_BASKET_STUB = "PayBasket";
    public static final String CLEAR_BASKET_STUB = "ClearBasket";
    public static final String UNHOLD_BASKET_STUB = "UnholdBasket";
    public static final String UPDATE_BASKET_LINE_STUB = "UpdateBasketLine";
    public static final String CANCEL_BASKET_LINE_STUB = "CancelBasketLine";
    public static final String CREATE_REFUND_STUB = "CreateRefund";
    public static final String DO_REFUND_STUB = "DoRefund";
    public static final String UNBIND_CARD_STUB = "UnbindCard";
    public static final String PAYMENT_METHODS_STUB = "ListPaymentMethods";
    public static final String BINDINGS_STUB = "Bindings";
    public static final String MARKUP_BASKET_STUB = "MarkupBasket";
    public static final String CREATE_DELIVERY_RECEIPT_STUB = "CreateDeliveryReceipt";
    public static final String CREATE_DELIVERY_CREDIT_RECEIPT_STUB = "CreateDeliveryCreditReceipt";
    public static final String LOAD_RECEIPT_PAYLOAD_STUB = "LoadReceiptPdf";
    public static final String CHECK_RECEIPT_PDF_STUB = "CheckReceiptPdf";
    public static final String RATE_LIMIT_STUB = "RateLimitStub";
    public static final String CREATE_ACCOUNT_STUB = "CreateAccount";
    public static final String TOPUP_STUB = "Topup";
    public static final String CREATE_CREDIT = "CreateCredit";
    public static final String CREDIT_START = "StartCredit";
    public static final String GATEWAY_INFO = "GatewayInfo";
    public static final String LIST_WALLET_BALANCE = "ListWalletBalance";
    private static final String TRUST_PAYMENTS_URL = "/trust-payments/v2/payments";
    private static final String RECEIPT_PDF = "receipt.pdf";
    @Autowired
    protected WireMockServer trustMock;
    @Autowired
    protected WireMockServer trustGatewayMock;
    @Autowired
    private BalanceMockHelper balanceMockHelper;

    public static String generateRandomTrustId() {
        Random random = new Random(System.currentTimeMillis());

        return toHexString(random.nextInt()) +
                toHexString(random.nextInt()) +
                toHexString(random.nextInt());
    }

    public static InputStream getMockReceiptPDF() {
        return BalanceMockHelper.class.getResourceAsStream(RECEIPT_PDF);
    }

    private static ResponseDefinitionBuilder responseWithBody(String bodyFileName, boolean binary) throws
            IOException {
        return (binary) ? aResponse().withBody(getBinaryBodyFromFile(bodyFileName)) :
                aResponse().withBody(getStringBodyFromFile(bodyFileName));
    }

    private static ResponseDefinitionBuilder ok() {
        return aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(SC_OK);
    }

    private static ResponseDefinitionBuilder badRequest() {
        return aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(SC_BAD_REQUEST);
    }

    private static byte[] getBinaryBodyFromFile(String fileName) throws IOException {
        return IOUtils.toByteArray(BalanceMockHelper.class.getResourceAsStream(fileName));
    }

    private static String getStringBodyFromFile(String fileName) throws
            IOException {
        return getStringBodyFromFile(fileName, null);
    }

    private static String getStringBodyFromFile(String fileName, Map<ResponseVariable, Object> vars) throws
            IOException {
        final String[] template = {IOUtils.toString(BalanceMockHelper.class.getResourceAsStream(fileName))};
        stream(ResponseVariable.values()).forEach(
                var -> template[0] = template[0].replace("{{" + var.name() + "}}",
                        Objects.toString((vars != null) ? vars.getOrDefault(var, var.defaultValue) :
                                var.defaultValue)));
        return template[0];
    }

    public void mockWholeTrust() {
        mockWholeTrust(null);
    }

    public void mockWholeTrust(TrustBasketKey basketKey) {
        CheckBasketParams mockConfig = buildWithBasketKeyConfig(basketKey);
        try {
            mockLoadPartner();
            mockCreateProduct();
            mockCreateOrdersBatch();
            mockCreateBasket(mockConfig);
            mockCreateCredit(mockConfig);
            mockTopup(mockConfig);
            mockCheckBasket(mockConfig);
            mockStatusBasket(mockConfig, null);
            mockPayBasket(mockConfig);
            mockStartCredit(mockConfig);
            mockClearBasket();
            mockCancelBasket();
            mockCancelCreditBasket();
            mockUpdateBasket();
            mockListPaymentMethods();
            mockUnbindCard();
            mockCreateRefund(null);
            mockDoRefund();
            mockMarkupBasket();
            mockCreateDeliveryReceipt();
            mockCreateCreditDeliveryReceipt();
            mockCreateAccount();

            mockTrustPaymentsReceipts();
            mockCheckRenderer();
            mockGatewayInfo();
            mockListWalletBalanceResponse();
            balanceMockHelper.mockWholeBalance();
        } catch (IOException io) {
            throw propagate(io);
        }
    }

    public void mockTrustPaymentsReceipts() {
        mockBalanceHttpMethod("GET", TRUST_PAYMENTS_URL + "/.*/receipts/.*",
                LOAD_RECEIPT_PAYLOAD_STUB, "payments_receipts.json", false);
    }

    public void mockEmptyTrustPaymentsReceipts() {
        mockBalanceHttpMethod("GET", TRUST_PAYMENTS_URL + "/.*/receipts/.*",
                LOAD_RECEIPT_PAYLOAD_STUB, "empty_payments_receipts.json", false);
    }

    public void mockNotFoundReceipts() {
        MappingBuilder builder = get(urlPathMatching(TRUST_PAYMENTS_URL + "/.*/receipts/.*"))
                .withName(LOAD_RECEIPT_PAYLOAD_STUB)
                .willReturn(notFound());
        trustMock.stubFor(builder);
    }

    public void mockCheckRenderer() {
        mockBalanceHttpMethod("POST", "/check/pdf", CHECK_RECEIPT_PDF_STUB, RECEIPT_PDF, true);
    }

    private void mockLoadPartner() throws IOException {
        MappingBuilder builder = get(urlPathMatching(TRUST_URL + "/partners/([0-9]*)"))
                .withName(LOAD_PARTNER_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("emptySuccess.json")));
        trustMock.stubFor(builder);
    }

    private void mockCreateProduct() throws IOException {
        MappingBuilder builder = post(urlEqualTo(TRUST_URL + "/products"))
                .withName(CREATE_PRODUCT_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("emptySuccess.json")));
        trustMock.stubFor(builder);
    }

    private void mockCreateOrdersBatch() throws IOException {
        MappingBuilder builder = post(urlEqualTo(TRUST_URL + "/orders_batch"))
                .withName(CREATE_ORDERS_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("emptySuccess.json")));
        trustMock.stubFor(builder);
    }

    public void mockCreateBasket() throws IOException {
        mockCreateBasket((CheckBasketParams) null);
    }

    public void mockCreateBasket(CheckBasketParams config) throws IOException {
        ImmutableMap.Builder<ResponseVariable, Object> params = ImmutableMap.builder();
        if (config != null) {
            params
                    .put(PURCHASE_TOKEN, config.getPurchaseToken() == null ? PURCHASE_TOKEN.defaultValue() :
                            config.getPurchaseToken())
                    .put(BASKET_ID, config.getPurchaseToken() == null ? BASKET_ID.defaultValue() :
                            config.getTrustPaymentId());
        }

        MappingBuilder builder = post(urlEqualTo(TRUST_PAYMENTS_CREATE_BASKET_URL))
                .withName(CREATE_BASKET_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("createBasket.json", params.build())));
        trustMock.stubFor(builder);
    }

    private void mockCreateCredit(CheckBasketParams config) throws IOException {
        ImmutableMap.Builder<ResponseVariable, Object> params = ImmutableMap.builder();
        if (config != null) {
            params
                    .put(PURCHASE_TOKEN, config.getPurchaseToken() == null ? PURCHASE_TOKEN.defaultValue() :
                            config.getPurchaseToken())
                    .put(BASKET_ID, config.getPurchaseToken() == null ? BASKET_ID.defaultValue() :
                            config.getTrustPaymentId());
        }

        MappingBuilder builder = post(urlEqualTo(TRUST_URL + "/credit?show_trust_payment_id=true"))
                .withName(CREATE_CREDIT)
                .willReturn(ok().withBody(getStringBodyFromFile("createBasket.json", params.build())));
        trustMock.stubFor(builder);
    }

    private void mockGatewayInfo() throws IOException {
        MappingBuilder builder = get(urlPathMatching(TRUST_URL + "/credit/([0123456789abcdef]*)/gateway_info"))
                .withName(GATEWAY_INFO)
                .willReturn(ok().withBody(getStringBodyFromFile("gatewayInfo.json")));
        trustMock.stubFor(builder);
    }

    public void mockCreateBasket(ResponseDefinitionBuilder response) {
        MappingBuilder builder = post(urlEqualTo(TRUST_PAYMENTS_CREATE_BASKET_URL))
                .withName(CREATE_BASKET_STUB)
                .willReturn(response.withHeader("Content-Type", "application/json"));
        trustMock.stubFor(builder);
    }

    private void mockTopup(CheckBasketParams config) throws IOException {
        ImmutableMap.Builder<ResponseVariable, Object> params = ImmutableMap.builder();
        if (config != null) {
            params
                    .put(PURCHASE_TOKEN, config.getPurchaseToken() == null ? PURCHASE_TOKEN.defaultValue() :
                            config.getPurchaseToken())
                    .put(BASKET_ID, config.getPurchaseToken() == null ? BASKET_ID.defaultValue() :
                            config.getTrustPaymentId());
        }

        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/topup"))
                .withName(TOPUP_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("createBasket.json", params.build())));
        trustMock.stubFor(builder);
    }

    public void mockCheckBasket(CheckBasketParams config) {
        mockCheckBasket(config, null);
    }

    public void mockCheckBasket(CheckBasketParams config, Consumer<MappingBuilder> mappingConfigurer) {
        MappingBuilder builder = get(urlPathMatching(TRUST_URL + "/payments/([0123456789abcdef]*)"))
                .withName(CHECK_BASKET_STUB)
                .willReturn(ok().withTransformer(BASKET_STATE_RESPONSE, CONFIG, config)
                        .withTransformerParameter(RESPONSE_STATUS, "authorized"));

        if (mappingConfigurer != null) {
            mappingConfigurer.accept(builder);
        }

        trustMock.stubFor(builder);
    }

    public void mockStatusBasket(CheckBasketParams config, Consumer<MappingBuilder> mappingConfigurer) {
        MappingBuilder builder = get(urlPathMatching(TRUST_URL + "/payment_status/([0123456789abcdef]*)"))
                .withName(GET_BASKET_STUB)
                .willReturn(ok().withTransformer(BASKET_STATUS_STATE_RESPONSE, CONFIG, config)
                        .withTransformerParameter(RESPONSE_STATUS, "authorized"));

        if (mappingConfigurer != null) {
            mappingConfigurer.accept(builder);
        }
        trustMock.stubFor(builder);
    }

    public void mockNotFoundStatusBasket() {
        MappingBuilder builder = get(urlPathMatching(TRUST_URL + "/payment_status/([0123456789abcdef]*)"))
                .withName(GET_BASKET_STUB)
                .willReturn(
                        notFound()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"status\": \"error\", \"status_code\": \"payment_not_found\"}")
                );
        trustMock.stubFor(builder);
    }

    public void mockNotFoundCheckBasket() {
        MappingBuilder builder = get(urlPathMatching(TRUST_URL + "/payments/([0123456789abcdef]*)"))
                .withName(CHECK_BASKET_STUB)
                .willReturn(
                        notFound()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"status\": \"error\", \"status_code\": \"payment_not_found\"}")
                );
        trustMock.stubFor(builder);
    }

    public void mockPayBasket(CheckBasketParams config) {
        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/payments/([0123456789abcdef]*)/start"))
                .withName(PAY_BASKET_STUB)
                .willReturn(ok()
                        .withTransformer(BASKET_STATE_RESPONSE, CONFIG, config)
                        .withTransformerParameter(RESPONSE_STATUS, "started"));
        trustMock.stubFor(builder);
    }

    public void mockPayBasketBadRequest() {
        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/payments/([0123456789abcdef]*)/start"))
                .withName(PAY_BASKET_STUB)
                .willReturn(badRequest()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"error\", \"status_code\": \"not_enough_funds\"}")
                );
        trustMock.stubFor(builder);
    }

    private void mockStartCredit(CheckBasketParams config) {
        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/credit/([0123456789abcdef]*)/start"))
                .withName(CREDIT_START)
                .willReturn(ok()
                        .withTransformer(BASKET_STATE_RESPONSE, CONFIG, config)
                        .withTransformerParameter(RESPONSE_STATUS, "started"));
        trustMock.stubFor(builder);
    }

    public void mockClearBasket() throws IOException {
        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/payments/([0123456789abcdef]*)/clear"))
                .withName(CLEAR_BASKET_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("emptySuccess.json")));
        trustMock.stubFor(builder);
    }

    public void mockClearBasketAlreadyCancelled() throws IOException {
        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/payments/([0123456789abcdef]*)/clear"))
                .withName(CLEAR_BASKET_STUB)
                .willReturn(badRequest()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"error\", \"status_code\": \"invalid_state\"}")
                );
        trustMock.stubFor(builder);
    }

    public void mockAlreadyClearedBasket() throws IOException {
        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/payments/([0123456789abcdef]*)/clear"))
                .withName(CLEAR_BASKET_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("alreadyCleared.json")));
        trustMock.stubFor(builder);
    }

    public void mockBadRequest() throws IOException {
        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/payments/([0123456789abcdef]*)/clear"))
                .withName(CLEAR_BASKET_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("invalid_state.json")));
        trustMock.stubFor(builder);
    }

    private void mockCancelBasket() throws IOException {
        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/payments/([0123456789abcdef]*)/unhold"))
                .withName(UNHOLD_BASKET_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("emptySuccess.json")));
        trustMock.stubFor(builder);
    }

    private void mockCancelCreditBasket() throws IOException {
        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/credit/([0123456789abcdef]*)/unhold"))
                .withName(UNHOLD_BASKET_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("emptySuccess.json")));
        trustMock.stubFor(builder);
    }

    public void mockUpdateBasket() throws IOException {
        //for unhold
        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/payments/([0123456789abcdef]*)/orders/" +
                "([a-z0-9\\-_A-Z]*)/unhold"))
                .withName(CANCEL_BASKET_LINE_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("emptySuccess.json")));
        trustMock.stubFor(builder);

        // for resize
        builder = post(urlPathMatching(TRUST_URL +
                "/payments/([0123456789abcdef]*)/orders/([a-z0-9\\-_A-Z]*)/resize"))
                .withName(UPDATE_BASKET_LINE_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("emptySuccess.json")));
        trustMock.stubFor(builder);
    }

    public void mockBindings() throws IOException {
        mockBindings("bindings_response.json");
    }

    public void mockListPaymentMethods() throws IOException {
        mockListPaymentMethods("listPaymentMethods.json");
    }

    public void mockListPaymentMethodsWithoutCashbackAccount() throws IOException {
        mockListPaymentMethods("listPaymentMethods_no_account.json");
    }

    public void mockBindings(String filename) throws IOException {
        MappingBuilder builder = get(urlPathMatching("/bindings-external/v2.0/bindings/"))
                .withQueryParam("with_binding_ts", equalTo("true"))
                .withQueryParam("with_expired", equalTo("true"))
                .withName(BINDINGS_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile(filename)));
        trustGatewayMock.stubFor(builder);
    }

    public void mockListPaymentMethods(String filename) throws IOException {
        MappingBuilder builder = get(urlPathMatching(TRUST_URL + "/payment-methods"))
                .withName(PAYMENT_METHODS_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile(filename)));
        trustMock.stubFor(builder);
    }

    public void mockUnbindCard() throws IOException {
        MappingBuilder builder = delete(urlEqualTo(TRUST_URL + "/payment_methods"))
                .withName(UNBIND_CARD_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("emptySuccess.json")));
        trustMock.stubFor(builder);
    }

    public void mockUnbindCard404() {
        MappingBuilder builder = delete(urlEqualTo(TRUST_URL + "/payment_methods"))
                .withName(UNBIND_CARD_STUB)
                .withHeader(AbstractTrustRestApi.UID_HEADER, new EqualToPattern("22"))
                .willReturn(notFound());
        trustMock.stubFor(builder);
    }

    public void mockUnbindCard404All() {
        MappingBuilder builder = delete(urlEqualTo(TRUST_URL + "/payment_methods"))
                .withName(UNBIND_CARD_STUB)
                .withHeader(AbstractTrustRestApi.UID_HEADER, StringValuePattern.ABSENT)
                .willReturn(notFound());
        trustMock.stubFor(builder);
    }

    public void mockDoRefund() throws IOException {
        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/refunds/([0123456789abcdef]*)/start"))
                .withName(DO_REFUND_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("emptySuccess.json")));
        trustMock.stubFor(builder);
    }

    public void mockCreateRefund(BasketStatus status) throws IOException {
        ImmutableMap.Builder<ResponseVariable, Object> params = ImmutableMap.builder();
        params.put(TRUST_REFUND_ID, TRUST_REFUND_ID.defaultValue());
        params.put(BASKET_STATUS, status != null ? status.name() : BASKET_STATUS.defaultValue.toString());

        MappingBuilder builder = post(urlEqualTo(TRUST_URL + "/refunds"))
                .withName(CREATE_REFUND_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("createRefund.json", params.build())));
        trustMock.stubFor(builder);
    }

    public void mockBadRequestRefund() {
        ImmutableMap.Builder<ResponseVariable, Object> params = ImmutableMap.builder();

        MappingBuilder builder = post(urlEqualTo(TRUST_URL + "/refunds"))
                .withName(CREATE_REFUND_STUB)
                .willReturn(badRequest()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"error\", \"status_code\": \"bad_request\"}")
                );
        trustMock.stubFor(builder);
    }

    public void mockBadRequestDoRefund() {
        ImmutableMap.Builder<ResponseVariable, Object> params = ImmutableMap.builder();

        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/refunds/([0123456789abcdef]*)/start"))
                .withName(DO_REFUND_STUB)
                .willReturn(badRequest()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"error\", \"status_code\": \"bad_request\"}")
                );
        trustMock.stubFor(builder);
    }

    private void mockMarkupBasket() throws IOException {
        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/payments/([0123456789abcdef]*)/markup"))
                .withName(MARKUP_BASKET_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("emptySuccess.json")));
        trustMock.stubFor(builder);
    }

    private void mockCreateDeliveryReceipt() throws IOException {
        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/payments/([0123456789abcdef]*)/deliver"))
                .withName(CREATE_DELIVERY_RECEIPT_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("emptySuccess.json")));
        trustMock.stubFor(builder);
    }

    private void mockCreateCreditDeliveryReceipt() throws IOException {
        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/credit/([0123456789abcdef]*)/deliver"))
                .withName(CREATE_DELIVERY_CREDIT_RECEIPT_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("emptySuccess.json")));
        trustMock.stubFor(builder);
    }

    public void mockCreateDeliveryReceiptWithNotPostauthorizedError() throws IOException {
        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/payments/([0123456789abcdef]*)/deliver"))
                .withName(CREATE_DELIVERY_RECEIPT_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("not_authorized.json")));
        trustMock.stubFor(builder);
    }

    public void mockRateLimitHit() {
        MappingBuilder builder = any(urlPathMatching(TRUST_URL + ".*"))
                .withName(RATE_LIMIT_STUB)
                .willReturn(aResponse().withStatus(429)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"error\", \"status_code\": \"unknown_error\", \"status_desc\": " +
                                "\"http 429 Too Many Requests\"}"));
        trustMock.stubFor(builder);
    }

    public void mockListWalletBalanceResponse() throws IOException {
        MappingBuilder builder = get(urlPathMatching("/legacy/wallet-balance*"))
                .withName(LIST_WALLET_BALANCE)
                .willReturn(ok().withBody(getStringBodyFromFile("wallet_balance.json")));
        trustGatewayMock.stubFor(builder);
    }

    public void mockListWalletBalanceResponse(Double cashbackAmount) {
        MappingBuilder builder = get(urlPathMatching("/legacy/wallet-balance*"))
                .withName(LIST_WALLET_BALANCE)
                .willReturn(ok().withBody("{\n" +
                        "    \"balances\": [\n" +
                        "        {\n" +
                        "            \"wallet_id\": \"w/30b153cc-8e30-58e2-8d1a-1095bc49b915\",\n" +
                        "            \"amount\": \"" + cashbackAmount + "\",\n" +
                        "            \"currency\": \"RUB\"\n" +
                        "        }\n" +
                        "    ]\n" +
                        "}\n"));
        trustGatewayMock.stubFor(builder);
    }

    public void mockNegativeListWalletBalanceResponse() throws IOException {
        MappingBuilder builder = get(urlPathMatching("/legacy/wallet-balance*"))
                .withName(LIST_WALLET_BALANCE)
                .willReturn(ok().withBody(getStringBodyFromFile("negative_wallet_balance.json")));
        trustGatewayMock.stubFor(builder);
    }

    public void mockEmptyListWalletBalanceResponse() {
        MappingBuilder builder = get(urlPathMatching("/legacy/wallet-balance*"))
                .withName(LIST_WALLET_BALANCE)
                .willReturn(ok().withBody("{}"));
        trustGatewayMock.stubFor(builder);
    }

    private void mockCreateAccount() throws IOException {
        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/account"))
                .withName(CREATE_ACCOUNT_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("create_account.json")));
        trustMock.stubFor(builder);
    }

    public BalanceMockHelper balanceHelper() {
        return balanceMockHelper;
    }

    public WireMockServer balanceMock() {
        return balanceHelper().balanceMock();
    }

    public void resetRequests() {
        trustMock.resetRequests();
        trustGatewayMock.resetRequests();
        balanceMockHelper.resetRequests();
    }

    public void resetAll() {
        trustMock.resetAll();
        trustGatewayMock.resetAll();
        balanceMockHelper.resetAll();
    }

    public WireMockServer trustMock() {
        return trustMock;
    }

    public WireMockServer trustGatewayMock() {
        return trustGatewayMock;
    }

    public List<ServeEvent> servedEvents() {
        return GenericMockHelper.servedEvents(trustMock);
    }

    public List<ServeEvent> servedGatewayEvents() {
        return GenericMockHelper.servedEvents(trustGatewayMock);
    }

    public OneElementBackIterator<ServeEvent> eventsIterator() {
        return new OneElementBackIterator<>(servedEvents());
    }

    public OneElementBackIterator<ServeEvent> eventsGatewayIterator() {
        return new OneElementBackIterator<>(servedGatewayEvents().iterator());
    }

    private StubMapping mockBalanceHttpMethod(String httpMethod, String pathRegex, String name,
                                              String bodyFileName, boolean binary) {
        try {
            return trustMock.stubFor(request(httpMethod, urlPathMatching(pathRegex)).withName(name)
                    .willReturn(responseWithBody(bodyFileName, binary)));
        } catch (IOException io) {
            throw new RuntimeException(io);
        }
    }


}
