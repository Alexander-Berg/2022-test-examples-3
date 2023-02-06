package ru.yandex.market.cashier.mocks.trust;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yandex.market.cashier.mocks.OneElementBackIterator;
import ru.yandex.market.cashier.mocks.trust.checkers.CheckBasketParams;
import ru.yandex.market.cashier.trust.api.TrustResponseStatus;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.request;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.google.common.base.Throwables.propagate;
import static java.lang.Integer.toHexString;
import static java.util.Arrays.stream;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.yandex.market.cashier.mocks.trust.BasketResponseTransformer.BASKET_STATE_RESPONSE;
import static ru.yandex.market.cashier.mocks.trust.BasketResponseTransformer.CONFIG;
import static ru.yandex.market.cashier.mocks.trust.BasketResponseTransformer.RESPONSE_STATUS;
import static ru.yandex.market.cashier.mocks.trust.ResponseVariable.BASKET_STATUS;
import static ru.yandex.market.cashier.mocks.trust.ResponseVariable.PURCHASE_TOKEN;
import static ru.yandex.market.cashier.mocks.trust.ResponseVariable.TRUST_REFUND_ID;

@Component
public class TrustMockConfigurer {

    public static final String CHECK_BASKET_STATUS_CODE = "payment_timeout";
    public static final String CHECK_BASKET_STATUS_DESC = "timeout while waiting for success";


    public static final String LOAD_PARTNER_STUB = "LoadPartner";
    public static final String CREATE_PRODUCT_STUB = "CreateServiceProduct";
    public static final String CREATE_ORDERS_STUB = "CreateOrdersBatch";
    public static final String CREATE_BASKET_STUB = "CreateBasket";
    public static final String CHECK_BASKET_STUB = "CheckBasket";
    public static final String PAY_BASKET_STUB = "PayBasket";
    public static final String CLEAR_BASKET_STUB = "ClearBasket";
    public static final String UNHOLD_BASKET_STUB = "UnholdBasket";
    public static final String UPDATE_BASKET_LINE_STUB = "UpdateBasketLine";
    public static final String CANCEL_BASKET_LINE_STUB = "CancelBasketLine";
    public static final String CREATE_REFUND_STUB = "CreateRefund";
    public static final String DO_REFUND_STUB = "DoRefund";
    public static final String MARKUP_BASKET_STUB = "MarkupBasket";

    @Autowired
    protected WireMockServer trustMock;

    private static final String TRUST_URL = "/trust-payments/v2";
    private static final String TRUST_PAYMENTS_URL = "/trust-payments/v2/payments";
    private static final String RECEIPT_PDF = "receipt.pdf";

    public static String generateRandomTrustId() {
        Random random = new Random(System.currentTimeMillis());

        return toHexString(random.nextInt()) +
                toHexString(random.nextInt()) +
                toHexString(random.nextInt());
    }

    public static InputStream getMockReceiptPDF() {
        return TrustMockConfigurer.class.getResourceAsStream(RECEIPT_PDF);
    }

    public void mockWholeTrust() {
        mockWholeTrust(null);
    }

    public void mockWholeTrust(String purchaseToken) {
        CheckBasketParams mockConfig = new CheckBasketParams().withPurchaseToken(purchaseToken);
        try {
            mockLoadPartner();
            mockCreateProduct();
            mockCreateOrdersBatch();
            mockCreateBasket(mockConfig);
            mockCheckBasket(mockConfig);
            mockPayBasket(mockConfig);
            mockClearBasket();
            mockCancelBasket();
            mockUpdateBasket();
            mockCreateRefund(null);
            mockDoRefund();
            mockMarkupBasket();

            mockTrustPayments();
            mockCheckRenderer();
//            balanceMockHelper.mockWholeBalance();
        } catch (IOException io) {
            throw propagate(io);
        }
    }

    public void mockTrustPayments() {
        mockTrustHttpMethod("GET", TRUST_PAYMENTS_URL + "/.*/receipts/.*",
                "payments_receipts.json", false);
    }

    public void mockCheckRenderer() {
        mockTrustHttpMethod("POST", "/check/pdf", RECEIPT_PDF, true);
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
            params.put(PURCHASE_TOKEN, config.getPurchaseToken() == null ? PURCHASE_TOKEN.defaultValue() : config.getPurchaseToken());
        }

        MappingBuilder builder = post(urlEqualTo(TRUST_URL + "/payments"))
                .withName(CREATE_BASKET_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("createBasket.json", params.build())));
        trustMock.stubFor(builder);
    }

    public void mockCreateBasket(ResponseDefinitionBuilder response) {
        MappingBuilder builder = post(urlEqualTo(TRUST_URL + "/payments"))
                .withName(CREATE_BASKET_STUB)
                .willReturn(response.withHeader("Content-Type", "application/json"));
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

    private void mockPayBasket(CheckBasketParams config) {
        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/payments/([0123456789abcdef]*)/start"))
                .withName(PAY_BASKET_STUB)
                .willReturn(ok()
                        .withTransformer(BASKET_STATE_RESPONSE, CONFIG, config)
                        .withTransformerParameter(RESPONSE_STATUS, "started"));
        trustMock.stubFor(builder);
    }

    private void mockClearBasket() throws IOException {
        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/payments/([0123456789abcdef]*)/clear"))
                .withName(CLEAR_BASKET_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("emptySuccess.json")));
        trustMock.stubFor(builder);
    }

    private void mockCancelBasket() throws IOException {
        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/payments/([0123456789abcdef]*)/unhold"))
                .withName(UNHOLD_BASKET_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("emptySuccess.json")));
        trustMock.stubFor(builder);
    }

    private void mockUpdateBasket() throws IOException {
        //for unhold
        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/payments/([0123456789abcdef]*)/orders/([a-z0-9\\-_A-Z]*)/unhold"))
                .withName(CANCEL_BASKET_LINE_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("emptySuccess.json")));
        trustMock.stubFor(builder);

        // for resize
        builder = post(urlPathMatching(TRUST_URL + "/payments/([0123456789abcdef]*)/orders/([a-z0-9\\-_A-Z]*)/resize"))
                .withName(UPDATE_BASKET_LINE_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("emptySuccess.json")));
        trustMock.stubFor(builder);
    }

    public void mockDoRefund() throws IOException {
        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/refunds/([0123456789abcdef]*)/start"))
                .withName(DO_REFUND_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("emptySuccess.json")));
        trustMock.stubFor(builder);
    }

    public void mockCreateRefund(TrustResponseStatus status) throws IOException {
        ImmutableMap.Builder<ResponseVariable, Object> params = ImmutableMap.builder();
        params.put(TRUST_REFUND_ID, TRUST_REFUND_ID.defaultValue());
        params.put(BASKET_STATUS, status != null ? status.name() : BASKET_STATUS.defaultValue.toString());

        MappingBuilder builder = post(urlEqualTo(TRUST_URL + "/refunds"))
                .withName(CREATE_REFUND_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("createRefund.json", params.build())));
        trustMock.stubFor(builder);
    }

    private void mockMarkupBasket() throws IOException {
        MappingBuilder builder = post(urlPathMatching(TRUST_URL + "/payments/([0123456789abcdef]*)/markup"))
                .withName(MARKUP_BASKET_STUB)
                .willReturn(ok().withBody(getStringBodyFromFile("emptySuccess.json")));
        trustMock.stubFor(builder);
    }

    public void resetRequests() {
        trustMock.resetRequests();
    }

    public void resetAll() {
        trustMock.resetAll();
    }

    public WireMockServer trustMock() {
        return trustMock;
    }

    public List<ServeEvent> servedEvents() {
        return Lists.reverse(trustMock.getAllServeEvents());
    }

    public OneElementBackIterator<ServeEvent> eventsIterator() {
        return new OneElementBackIterator<>(servedEvents().iterator());
    }

    private void mockTrustHttpMethod(String httpMethod, String pathRegex,
                                     String bodyFileName, boolean binary) {
        try {
            trustMock.stubFor(request(httpMethod, urlPathMatching(pathRegex))
                    .willReturn(responseWithBody(bodyFileName, binary)));
        } catch (IOException io) {
            throw new RuntimeException(io);
        }
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

    private static byte[] getBinaryBodyFromFile(String fileName) throws IOException {
        return IOUtils.toByteArray(TrustMockConfigurer.class.getResourceAsStream(fileName));
    }

    private static String getStringBodyFromFile(String fileName) throws
            IOException {
        return getStringBodyFromFile(fileName, null);
    }

    private static String getStringBodyFromFile(String fileName, Map<ResponseVariable, Object> vars) throws
            IOException {
        final String[] template = {IOUtils.toString(TrustMockConfigurer.class.getResourceAsStream(fileName))};
        stream(ResponseVariable.values()).forEach(
                var -> template[0] = template[0].replace("{{" + var.name() + "}}",
                        Objects.toString((vars != null) ? vars.getOrDefault(var, var.defaultValue) : var.defaultValue)));
        return template[0];
    }


}
