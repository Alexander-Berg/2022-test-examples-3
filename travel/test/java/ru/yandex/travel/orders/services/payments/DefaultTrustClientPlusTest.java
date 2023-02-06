package ru.yandex.travel.orders.services.payments;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.opentracing.util.GlobalTracer;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.orders.services.payments.model.TrustCompositeOrderPaymentMarkup;
import ru.yandex.travel.orders.services.payments.model.TrustCreateBasketOrder;
import ru.yandex.travel.orders.services.payments.model.TrustCreateBasketRequest;
import ru.yandex.travel.orders.services.payments.model.TrustResponseStatus;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@Slf4j
public class DefaultTrustClientPlusTest {
    public static final String TOKEN = "08d8373d931af031a18c581e366f3a2d";
    public static final String TYPICAL_SUCCESS_RESPONSE =
            "{\"status\":\"success\",\"status_code\":\"payment_created\",\"purchase_token\":\"" + TOKEN + "\"}";
    public static final String TYPICAL_STATUS_SUCCESS_RESPONSE =
            "{\"status\": \"success\", \"basket_rows\": [{\"order_id\": \"161994502\", \"amount\": \"100000.00\", " +
                    "\"quantity\": \"0.00\"}], \"payment_url\": " +
                    "\"https://trust-test.yandex.ru/web/payment?purchase_token=3159103c93133b80f56fc00ab05ad1d8\", " +
                    "\"payment_status\": \"started\", \"start_ts\": \"1626954405.211\"," +
                    " \"orders\": [{\"product_type\": \"app\", \"uid\": \"4003404817\", \"paid_amount\": \"0.00\", " +
                    "\"current_amount\": [], \"order_ts\": \"1626954230.683\", \"current_qty\": \"0.00\", " +
                    "\"order_id\": \"161994502\", \"orig_amount\": \"100000.00\", " +
                    "\"product_name\": \"TestTestAppProduct\", \"product_id\": \"1185272925398809663\"}], " +
                    "\"currency\": \"RUB\", \"amount\": \"100000.00\", " +
                    "\"payment_timeout\": \"1200.000\", " +
                    "\"purchase_token\": \"" + TOKEN + "\", " +
                    "\"uid\": \"4003404817\"}";
    public static final TrustUserInfo USER_INFO = new TrustUserInfo("uid", "userIp");
    @Rule
    public WireMockRule wireMockRule =
            new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    private AsyncHttpClient httpClient;
    private TrustClient trustClient;

    @Before
    public void prepareClient() {
        httpClient = Dsl.asyncHttpClient();
        var clientWrapper = new AsyncHttpClientWrapper(
                httpClient, log, "testDestination", GlobalTracer.get()
        );
        trustClient = new DefaultTrustClient(
                clientWrapper,
                new TrustConnectionProperties("http://localhost:" + wireMockRule.port(),
                        false,
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1)),
                ""
        );
    }

    @After
    public void tearDown() throws IOException {
        httpClient.close();
    }

    @Test
    public void testCreateTopup() {
        stubFor(post("/topup")
                .willReturn(okJson(TYPICAL_SUCCESS_RESPONSE)));
        var response =
                trustClient.createTopup(
                        null,
                        new TrustUserInfo("uid", "userIp"));
        assertSame(TrustResponseStatus.SUCCESS, response.getStatus());
        assertEquals(TOKEN, response.getPurchaseToken());
    }

    @Test
    public void testStatus() {
        stubFor(get("/topup/" + TOKEN)
                .willReturn(okJson(TYPICAL_STATUS_SUCCESS_RESPONSE)));
        var response =
                trustClient.getTopupStatus(TOKEN, USER_INFO);
        assertSame(TrustResponseStatus.SUCCESS, response.getStatus());
        assertEquals(TOKEN, response.getPurchaseToken());
    }

    @Test
    public void testStart() {
        stubFor(post("/topup/" + TOKEN + "/start")
                .willReturn(okJson(TYPICAL_STATUS_SUCCESS_RESPONSE)));
        var response =
                trustClient.startTopup(TOKEN, USER_INFO);
        assertSame(TrustResponseStatus.SUCCESS, response.getStatus());
        assertEquals(TOKEN, response.getPurchaseToken());
        assertNotNull(response.getOrders().get(0).getCurentQty());
    }

    @Test
    public void testCompositePayment() {
        stubFor(post("/payments")
                .willReturn(okJson("{\n" +
                        "    \"status\": \"success\",\n" +
                        "    \"status_code\": \"payment_created\",\n" +
                        "    \"purchase_token\": \"" + TOKEN + "\"\n" +
                        "}")));

        String orderId = "orderId";
        BigDecimal totalPrice = BigDecimal.valueOf(100.50);
        BigDecimal plusPrice = BigDecimal.valueOf(50);
        TrustCreateBasketRequest request = new TrustCreateBasketRequest();
        request.setOrders(List.of(TrustCreateBasketOrder.builder()
                .orderId(orderId)
                .price(totalPrice).build()));
        request.setPaymethodMarkup(Map.of(orderId, TrustCompositeOrderPaymentMarkup.builder()
                .card(totalPrice.subtract(plusPrice))
                .yandexAccount(plusPrice)
                .build()));

        var response =
                trustClient.createBasket(request, USER_INFO, null);

        assertSame(TrustResponseStatus.SUCCESS, response.getStatus());
        assertEquals(TOKEN, response.getPurchaseToken());
        verify(
                postRequestedFor(urlEqualTo("/payments"))
                        .withRequestBody(matchingJsonPath("paymethod_markup." + orderId +
                                ".yandex_account", equalTo("50")))
                        .withRequestBody(matchingJsonPath("paymethod_markup." + orderId +
                                ".card", equalTo("50.5")))
        );
    }
}
