package ru.yandex.travel.orders.services.payments;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.money.CurrencyUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.opentracing.mock.MockTracer;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.services.payments.model.PaymentStatusEnum;
import ru.yandex.travel.orders.services.payments.model.TrustBasketStatusResponse;
import ru.yandex.travel.orders.services.payments.model.TrustBoundPaymentMethod;
import ru.yandex.travel.orders.services.payments.model.TrustCompositeOrderPaymentMarkup;
import ru.yandex.travel.orders.services.payments.model.TrustCreateBasketOrder;
import ru.yandex.travel.orders.services.payments.model.TrustCreateBasketPassParams;
import ru.yandex.travel.orders.services.payments.model.TrustCreateBasketRequest;
import ru.yandex.travel.orders.services.payments.model.TrustCreateBasketResponse;
import ru.yandex.travel.orders.services.payments.model.TrustCreateOrderResponse;
import ru.yandex.travel.orders.services.payments.model.TrustCreateRefundRequest;
import ru.yandex.travel.orders.services.payments.model.TrustCreateRefundResponse;
import ru.yandex.travel.orders.services.payments.model.TrustEnabledPaymentMethod;
import ru.yandex.travel.orders.services.payments.model.TrustPaymentMethodsRequest;
import ru.yandex.travel.orders.services.payments.model.TrustPaymentMethodsResponse;
import ru.yandex.travel.orders.services.payments.model.TrustRefundState;
import ru.yandex.travel.orders.services.payments.model.TrustResizeRequest;
import ru.yandex.travel.orders.services.payments.model.TrustResponseStatus;
import ru.yandex.travel.orders.services.payments.model.TrustStartPaymentResponse;
import ru.yandex.travel.orders.services.payments.model.plus.TrustAccount;
import ru.yandex.travel.orders.services.payments.model.plus.TrustCreateAccountRequest;
import ru.yandex.travel.orders.services.payments.model.plus.TrustCreateAccountResponse;
import ru.yandex.travel.orders.services.payments.model.plus.TrustCreateTopupResponse;
import ru.yandex.travel.orders.services.payments.model.plus.TrustGetAccountsResponse;
import ru.yandex.travel.orders.services.payments.model.plus.TrustTopupCashbackType;
import ru.yandex.travel.orders.services.payments.model.plus.TrustTopupPassParams;
import ru.yandex.travel.orders.services.payments.model.plus.TrustTopupPayload;
import ru.yandex.travel.orders.services.payments.model.plus.TrustTopupRequest;
import ru.yandex.travel.testing.TestUtils;
import ru.yandex.travel.testing.local.LocalTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plus API documentation and samples can be found here:
 * https://wiki.yandex-team.ru/trust/yandexaccount/
 */
@Ignore
@Slf4j
public class DefaultTrustClientLocalTest {
    public static final String TEST_PASSPORT_ID = "4003404817"; // test user from docs
    //public static final String TEST_PASSPORT_ID = "4021320124"; // tlg-13

    private DefaultTrustClient client;

    @Before
    public void init() {
        AsyncHttpClient ahc = Dsl.asyncHttpClient(Dsl.config()
                .setThreadFactory(new ThreadFactoryBuilder().setDaemon(true).build())
                .build());
        AsyncHttpClientWrapper ahcWrapper = new AsyncHttpClientWrapper(
                ahc, log, "trust", new MockTracer(),
                Arrays.stream(DefaultTrustClient.Method.values()).map(Enum::name).collect(Collectors.toSet())
        );
        TrustConnectionProperties properties = TrustConnectionProperties.builder()
                .baseUrl("https://trust-payments-test.paysys.yandex.net:8028/trust-payments/v2")
                // ssh -L 8028:trust-payments.paysys.yandex.net:8028 __ANY_PROD_HOST__
                // (hosts: https://nanny.yandex-team.ru/ui/#/services/catalog/travel_orders_app_prod/)
                // Then add "127.0.0.1       trust-payments.paysys.yandex.net" to your /etc/hosts
                //.baseUrl("https://trust-payments.paysys.yandex.net:8028/trust-payments/v2")
                .httpReadTimeout(Duration.ofSeconds(30))
                .httpRequestTimeout(Duration.ofSeconds(30))
                .build();

        String trustToken = LocalTestUtils.readYavSecret("ver-01f5fyskj1171vyngxyhvjys4d", "value");
        client = new DefaultTrustClient(ahcWrapper, properties, trustToken);
    }

    @Test
    public void testPaymentMethods() {
        var req = TrustPaymentMethodsRequest.builder()
                .showEnabled(true)
                .build();
        TrustPaymentMethodsResponse res = client.getPaymentMethods(
                req, new TrustUserInfo(TEST_PASSPORT_ID, null));

        //log.info("Response: {}", res);
        log.info("Bound payment methods:");
        for (TrustBoundPaymentMethod method : res.getBoundPaymentMethods()) {
            log.info("    {}", method);
        }
        log.info("Enabled payment methods:");
        for (TrustEnabledPaymentMethod method : res.getEnabledPaymentMethods()) {
            log.info("    {}", method);
        }
    }

    @Test
    public void createYandexAccountIfNone() {
        TrustUserInfo userInfo = new TrustUserInfo(TEST_PASSPORT_ID, null);
        TrustGetAccountsResponse accountsRsp = client.getAccounts(userInfo);
        if (!accountsRsp.getAccounts().isEmpty()) {
            log.info("Existing accounts:");
            for (TrustAccount account : accountsRsp.getAccounts()) {
                log.info("    {}", account);
            }
        } else {
            log.info("No Yandex Account, creating a new one");
            TrustCreateAccountRequest createReq = TrustCreateAccountRequest.builder()
                    .currency("RUB")
                    .build();
            TrustCreateAccountResponse createRsp = client.createAccount(createReq, userInfo);
            log.info("New account response: {}", createRsp);
        }
    }

    // use this method ONLY IN TESTING, topup in production requires additional account data (payload)
    @Test
    public void getBalance() {
        CurrencyUnit currency = ProtoCurrencyUnit.RUB;

        TrustBoundPaymentMethod method = client.getPaymentMethods(testUserInfo()).getBoundYandexAccountMethod(currency);
        log.info("Balance: {} {}", method.getBalance(), method.getCurrency());
        log.info("Account: {}", method.getAccount());
    }

    @Test
    public void addYandexAccountPoints() {
        TrustUserInfo userInfo = new TrustUserInfo(TEST_PASSPORT_ID, null);
        CurrencyUnit currency = ProtoCurrencyUnit.RUB;

        TrustBoundPaymentMethod method = client.getPaymentMethods(userInfo).getBoundYandexAccountMethod(currency);
        log.info("Initial Yandex Account balance: {} {}", method.getBalance(), method.getCurrency());

        TrustTopupRequest topupReq = TrustTopupRequest.builder()
                .paymethodId(method.getId())
                // FiscalItemType.HOTELS_YANDEX_PLUS_CASHBACK.getTrustId()
                .productId("HOTELS_PLUS_CASHBACK")
                .amount((long) 1_000)
                .currency(currency.getCurrencyCode())
                .passParams(TrustTopupPassParams.builder()
                        .payload(testPayload())
                        // For manual tests in PROD use the proper payload
                        //.payload(prodPayload())
                        .build())
                .build();
        TrustCreateTopupResponse topupRsp = client.createTopup(topupReq, userInfo);
        log.info("Topup response: {}", topupRsp);

        String token = topupRsp.getPurchaseToken();
        TrustStartPaymentResponse startRsp = client.startPayment(token, userInfo);
        log.info("Start response: {}", startRsp);

        while (client.getBasketStatus(token, userInfo).getPaymentStatus() != PaymentStatusEnum.CLEARED) {
            log.info("The topup hasn't completed, waiting a bit more");
            TestUtils.sleep(Duration.ofSeconds(3));
        }
        log.info("The topup operation has successfully finished:\n\t{}", testScroogePaymentUrl(token));
        log.info("Check the updated balance here:\n\t{}", plusHomeUrl());

        // then it takes some time for the balance to be updated in the payment methods API
    }

    // don't forget to specify your own passport id in the TEST_PASSPORT_ID class field above
    @Test
    public void paymentWithPaymentMarkups() {
        TrustUserInfo userInfo = testUserInfo();

        // FiscalItemType.EXPEDIA_HOTEL.getTrustId()
        TrustCreateOrderResponse orderRsp = client.createOrder("PLACEMENT_EXPEDIA", userInfo);
        assertThat(orderRsp.getStatus()).isEqualTo(TrustResponseStatus.SUCCESS);

        BigDecimal totalPrice = BigDecimal.valueOf(1000);
        BigDecimal plusPrice = BigDecimal.valueOf(200);

        TrustCreateBasketResponse basketRsp = client.createBasket(TrustCreateBasketRequest.builder()
                //.paymethodId("composite") // doesn't work for some reason
                .userEmail("test-email@example.com")
                .orders(List.of(TrustCreateBasketOrder.builder()
                        .orderId(orderRsp.getOrderId())
                        .price(totalPrice)
                        .qty(1)
                        .fiscalNds("nds_20"/*VatType.getTrustValue()*/)
                        .fiscalTitle("DefaultTrustClientLocalTest")
                        .build()))
                .paymethodMarkup(Map.of(orderRsp.getOrderId(), TrustCompositeOrderPaymentMarkup.builder()
                        .card(totalPrice.subtract(plusPrice))
                        .yandexAccount(plusPrice)
                        .build()))
                .passParams(TrustCreateBasketPassParams.builder()
                        .payload(TrustTopupPayload.builder()
                                .cashbackService("travel")
                                .build())
                        .build())
                .build(), userInfo, null);
        assertThat(basketRsp.getStatus()).isEqualTo(TrustResponseStatus.SUCCESS);

        TrustStartPaymentResponse startRsp = client.startPayment(basketRsp.getPurchaseToken(), userInfo);
        assertThat(startRsp.getStatus()).isEqualTo(TrustResponseStatus.SUCCESS);

        log.info("Payment URL:\n\t{}", startRsp.getPaymentUrl());

        while (!Set.of(PaymentStatusEnum.AUTHORIZED, PaymentStatusEnum.NOT_AUTHORIZED)
                .contains(client.getBasketStatus(basketRsp.getPurchaseToken(), userInfo).getPaymentStatus())) {
            log.info("The payment hasn't completed, waiting a bit more");
            TestUtils.sleep(Duration.ofSeconds(5));
        }

        TrustBasketStatusResponse basket = client.getBasketStatus(basketRsp.getPurchaseToken(), userInfo);
        assertThat(basket.getPaymentMethod()).isEqualTo("composite");
        assertThat(basket.getPaymethodId()).isEqualTo("composite");

        log.info("Scrooge URL:\n\t{}", testScroogePaymentUrl(basketRsp.getPurchaseToken()));
        log.info("Check the updated balance here:\n\t{}", plusHomeUrl());
        log.info("Payment data:\n\tpurchase token: {}\n\torder id: {}",
                basketRsp.getPurchaseToken(), orderRsp.getOrderId());
    }

    @Test
    public void resizeWithPaymentMarkups() {
        String purchaseToken = "___SPECIFY_ME___";
        String orderId = "___SPECIFY_ME___";

        TrustResizeRequest resizeReq = TrustResizeRequest.builder()
                .amount(BigDecimal.valueOf(650))
                .qty(1)
                .paymethodMarkup(Map.of(orderId, TrustCompositeOrderPaymentMarkup.builder()
                        .card(BigDecimal.valueOf(650))
                        .yandexAccount(BigDecimal.valueOf(0))
                        .build()))
                .build();
        client.resize(purchaseToken, orderId, resizeReq, testUserInfo());

        TrustBasketStatusResponse basket = client.getBasketStatus(purchaseToken, testUserInfo());
        // for some reason it doesn't get updated
        log.info("Updated markup: {}", basket.getPaymentMarkup());

        log.info("Order: {}", client.getOrder(orderId, null));
    }

    @Test
    public void clear() {
        String purchaseToken = "___SPECIFY_ME___";

        client.clear(purchaseToken, testUserInfo());

        log.info("Waiting a bit for status change...");
        TestUtils.sleep(Duration.ofSeconds(10));

        TrustBasketStatusResponse basket = client.getBasketStatus(purchaseToken, testUserInfo());
        log.info("Updated status: {}, markup: {}", basket.getPaymentStatus(), basket.getPaymentMarkup());
    }

    @Test
    public void refundWithMarkup() {
        String purchaseToken = "___SPECIFY_ME___";
        String orderId = "___SPECIFY_ME___";

        TrustCreateRefundResponse refundRsp = client.createRefund(TrustCreateRefundRequest.builder()
                .purchaseToken(purchaseToken)
                .reasonDesc("test")
                .orders(List.of(TrustCreateRefundRequest.Order.builder()
                        .orderId(orderId)
                        .deltaAmount(BigDecimal.valueOf(150))
                        .build()))
                .paymethodMarkup(Map.of(orderId, TrustCompositeOrderPaymentMarkup.builder()
                        //.card(BigDecimal.valueOf(0))
                        .yandexAccount(BigDecimal.valueOf(150))
                        .build()))
                .build(), testUserInfo());
        assertThat(refundRsp.getStatus()).isEqualTo(TrustResponseStatus.SUCCESS);

        client.startRefund(refundRsp.getTrustRefundId(), testUserInfo());

        while (!Set.of(TrustRefundState.SUCCESS, TrustRefundState.FAILED, TrustRefundState.ERROR)
                .contains(client.getRefundStatus(refundRsp.getTrustRefundId(), testUserInfo()).getStatus())) {
            log.info("The refund hasn't completed, waiting a bit more");
            TestUtils.sleep(Duration.ofSeconds(5));
        }

        log.info("Refund: {}", client.getRefundStatus(refundRsp.getTrustRefundId(), testUserInfo()));

        TrustBasketStatusResponse basket = client.getBasketStatus(purchaseToken, testUserInfo());
        log.info("Basket status: {}, markup: {}", basket.getPaymentStatus(), basket.getPaymentMarkup());

        log.info("Scrooge refund URL:\n\t{}", testScroogeRefundUrl(refundRsp.getTrustRefundId()));
        log.info("Check the updated balance here:\n\t{}", plusHomeUrl());
    }

    private TrustUserInfo testUserInfo() {
        return new TrustUserInfo(TEST_PASSPORT_ID, null);
    }

    private TrustTopupPayload testPayload() {
        return TrustTopupPayload.builder()
                .cashbackService("travel")
                .cashbackType(TrustTopupCashbackType.TRANSACTION)
                .hasPlus(true)
                .baseAmount(new BigDecimal("3000.0000"))
                .commissionAmount(new BigDecimal("300.00"))
                .vatCommissionAmount(new BigDecimal("20.00"))
                .serviceId(-123L)
                .issuer("marketing")
                .campaignName("manual topup for tests")
                .ticket("NEWSERVICE-XXX")
                .build();
    }

    private TrustTopupPayload prodPayload() {
        return TrustTopupPayload.builder()
                .cashbackService("travel")
                .cashbackType(TrustTopupCashbackType.TRANSACTION)
                .hasPlus(true)
                .baseAmount(new BigDecimal("0"))
                .commissionAmount(new BigDecimal("0"))
                .vatCommissionAmount(new BigDecimal("0"))
                .serviceId((long) 641)
                .issuer("hotels_marketing")
                .campaignName("hotel orders cashback (for testers)")
                .ticket("NEWSERVICE-1330")
                .build();
    }

    private static String testScroogePaymentUrl(String purchaseToken) {
        return "https://scrooge-test.paysys.yandex-team.ru/payments/?purchase_token=" + purchaseToken;
    }

    private static String testScroogeRefundUrl(String trustRefundId) {
        return "https://scrooge-test.paysys.yandex-team.ru/refunds/?trust_refund_id=" + trustRefundId;
    }

    private static String plusHomeUrl() {
        return "https://plus.tst.yandex.ru/";
    }
}
