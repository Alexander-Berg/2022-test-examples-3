package ru.yandex.market.checkout.checkouter.balance.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.assertj.core.util.Lists;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.cashier.model.PassParams;
import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.balance.trust.model.BalanceOrderParams;
import ru.yandex.market.checkout.checkouter.balance.trust.model.BasketLineMarkup;
import ru.yandex.market.checkout.checkouter.balance.trust.model.BasketMarkup;
import ru.yandex.market.checkout.checkouter.balance.trust.model.CreateBasketLine;
import ru.yandex.market.checkout.checkouter.balance.trust.model.DeliveredBasketLine;
import ru.yandex.market.checkout.checkouter.balance.trust.model.YandexCashbackInfo;
import ru.yandex.market.checkout.checkouter.balance.xmlrpc.TrustException;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.TrustBasketKey;
import ru.yandex.market.checkout.checkouter.pay.builders.CashPaymentBuilder;
import ru.yandex.market.checkout.checkouter.pay.builders.SupplierPaymentBuilder;
import ru.yandex.market.checkout.checkouter.pay.strategies.CashPaymentStrategyImpl;
import ru.yandex.market.checkout.checkouter.shop.PaymentArticle;
import ru.yandex.market.checkout.checkouter.shop.PaymentClass;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.common.util.UrlBuilder;
import ru.yandex.market.checkout.common.web.CheckoutHttpParameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.balance.OneElementBackIterator;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CLEAR_BASKET_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_DELIVERY_RECEIPT_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.generateRandomTrustId;
import static ru.yandex.market.checkout.util.balance.checkers.CreateBasketParams.createBasket;
import static ru.yandex.market.checkout.util.balance.checkers.CreateProductParams.product;
import static ru.yandex.market.checkout.util.balance.checkers.MarkupBasketLineParams.line;
import static ru.yandex.market.checkout.util.balance.checkers.MarkupBasketParams.markup;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkBatchServiceOrderCreationCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkCreateAccountMethodCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkCreateBasketCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkCreateCreditDeliveryReceiptCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkCreateDeliveryReceiptCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkListPaymentMethodsCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkListWalletBalanceMethodCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkLoadPartnerCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkMarkupBasketCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkOptionalCreateServiceProductCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkUnbindCardCall;

public class TrustServiceTest extends AbstractServicesTestBase {

    private static final long CAMPAIGN_ID = 123L;
    private static final long CLIENT_ID = 456L;
    private static final String YA_MONEY_ID = "asdasd";
    private static final int DELIVERY_SERVICE_CLIENT_ID = 13432546;
    private static final int AGENCY_COMMISSION = 0;

    @Autowired
    private TrustService trustService;
    @Autowired
    private CashPaymentStrategyImpl cashPaymentStrategy;

    private ShopMetaData shopMetaData;

    @BeforeEach
    public void setUp() throws Exception {
        shopMetaData = ShopMetaDataBuilder.createTestDefault()
                .withCampaiginId(CAMPAIGN_ID)
                .withClientId(CLIENT_ID)
                .withSandboxClass(PaymentClass.YANDEX)
                .withProdClass(PaymentClass.YANDEX)
                .withYaMoneyId(YA_MONEY_ID)
                .withArticles(new PaymentArticle[0])
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .withInn("asda")
                .withPhone("def")
                .withAgencyCommission(AGENCY_COMMISSION)
                .build();

        trustMockConfigurer.mockWholeTrust();
    }

    @AfterEach
    public void tearDown() {
        trustMockConfigurer.resetAll();
    }

    @Test
    public void stupidTest() {
        trustService.createServiceProduct(shopMetaData, PaymentGoal.ORDER_POSTPAY, Color.GREEN);
    }

    @Test
    public void tvmHeaderTest() {
        trustService.createServiceProduct(shopMetaData, PaymentGoal.ORDER_POSTPAY, Color.GREEN);
        trustMockConfigurer.servedEvents().forEach(e ->
                Assertions.assertNotNull(e.getRequest().getHeader(CheckoutHttpParameters.SERVICE_TICKET_HEADER)));
    }

    @Test
    public void testMarkupBasket() {
        TrustBasketKey basketKey = new TrustBasketKey(generateRandomTrustId(), generateRandomTrustId());

        Payment payment = new Payment();
        payment.setBasketKey(basketKey);
        payment.setBalanceServiceId(610L);

        BasketMarkup markup = new BasketMarkup();

        markup
                .addBasketLineMarkup("123", new BasketLineMarkup()
                        .addPaymentMethod("card", BigDecimal.TEN)
                        .addPaymentMethod("spasibo", new BigDecimal(100)))
                .addBasketLineMarkup("234", new BasketLineMarkup()
                        .addPaymentMethod("spasibo", new BigDecimal(200))
                        .addPaymentMethod("card", new BigDecimal(300)));


        trustService.markupBasket(payment, Set.of(Color.BLUE), markup);


        checkMarkupBasketCall(trustMockConfigurer.eventsIterator(),
                markup(equalTo(basketKey.getPurchaseToken()), payment.getUid())
                        .withLine("234", line()
                                .withPaymethod("card", new BigDecimal(300))
                                .withPaymethod("spasibo", new BigDecimal(200)))
                        .withLine("123", line()
                                .withPaymethod("spasibo", new BigDecimal(100))
                                .withPaymethod("card", BigDecimal.TEN)));
    }

    @Test
    public void testPrintOffsetAdvanceReceipt() {
        TrustBasketKey basketKey = new TrustBasketKey(generateRandomTrustId(), generateRandomTrustId());

        Payment payment = new Payment();
        payment.setBasketKey(basketKey);
        payment.setBalanceServiceId(610L);

        List<DeliveredBasketLine> lines = new ArrayList<DeliveredBasketLine>() {{
            add(new DeliveredBasketLine("111", VatType.VAT_20.name()));
            add(new DeliveredBasketLine("222", VatType.VAT_10.name()));
        }};

        trustService.printOffsetAdvanceReceipt(payment, lines, Color.BLUE);

        ServeEvent event = trustMockConfigurer.eventsIterator().next();
        checkCreateDeliveryReceiptCall(event, payment, body -> {
            JsonArray orders = body.getAsJsonArray("orders");
            assertEquals(lines.size(), orders.size());

            orders.forEach(e -> {
                JsonObject jsonObject = e.getAsJsonObject();

                assertTrue(lines.stream().anyMatch(
                        l -> l.getOrderId().equals(jsonObject.get("order_id").getAsString())
                                && l.getFiscalVat().equals(jsonObject.get("fiscal_nds").getAsString())));
            });
        });
    }

    @Test
    public void testPrintCreditOffsetAdvanceReceipt() {
        TrustBasketKey basketKey = new TrustBasketKey(generateRandomTrustId(), generateRandomTrustId());

        Payment payment = new Payment();
        payment.setBasketKey(basketKey);
        payment.setBalanceServiceId(610L);
        payment.setType(PaymentGoal.TINKOFF_CREDIT);

        List<DeliveredBasketLine> lines = new ArrayList<DeliveredBasketLine>() {{
            add(new DeliveredBasketLine("111", VatType.VAT_20.name()));
            add(new DeliveredBasketLine("222", VatType.VAT_10.name()));
        }};

        trustService.printOffsetAdvanceReceipt(payment, lines, Color.BLUE);

        ServeEvent event = trustMockConfigurer.eventsIterator().next();
        checkCreateCreditDeliveryReceiptCall(event, payment, body -> {
            JsonArray orders = body.getAsJsonArray("orders");
            assertEquals(lines.size(), orders.size());

            orders.forEach(e -> {
                JsonObject jsonObject = e.getAsJsonObject();

                assertTrue(lines.stream().anyMatch(
                        l -> l.getOrderId().equals(jsonObject.get("order_id").getAsString())
                                && l.getFiscalVat().equals(jsonObject.get("fiscal_nds").getAsString())));
            });
        });
    }

    @Test
    public void testPrintOffsetAdvanceReceiptWithNotPostauthorizedError() throws IOException {
        TrustBasketKey basketKey = new TrustBasketKey(generateRandomTrustId(), generateRandomTrustId());

        Payment payment = new Payment();
        payment.setBasketKey(basketKey);
        payment.setBalanceServiceId(610L);

        List<DeliveredBasketLine> lines = new ArrayList<DeliveredBasketLine>() {{
            add(new DeliveredBasketLine("111", VatType.VAT_20.name()));
            add(new DeliveredBasketLine("222", VatType.VAT_10.name()));
        }};

        trustMockConfigurer.mockCreateDeliveryReceiptWithNotPostauthorizedError();

        trustService.printOffsetAdvanceReceipt(payment, lines, Color.BLUE);

        OneElementBackIterator<ServeEvent> iterator = trustMockConfigurer.eventsIterator();
        ServeEvent deliverCall = iterator.next();
        assertEquals(CREATE_DELIVERY_RECEIPT_STUB, deliverCall.getStubMapping().getName());
        ServeEvent clearCall = iterator.next();
        assertEquals(CLEAR_BASKET_STUB, clearCall.getStubMapping().getName());
        trustMockConfigurer.resetAll();
    }

    @Test
    public void testCreateCashBalanceOrder() {
        Order order = OrderProvider.getBlueOrder();

        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final long passportUidTopBound = 1L << 60; // see https://wiki.yandex-team.ru/passport/uids/
        order.setCreationDate(new Date(1522432507966L));
        order.setId(random.nextLong(passportUidTopBound));

        BalanceOrderParams params = cashPaymentStrategy.prepareBalanceOrderParams(BuyerProvider.UID, order,
                shopMetaData,
                PaymentGoal.ORDER_POSTPAY);
        assertEquals(params.getAgencyCommission(), Integer.valueOf(0));
        assertEquals(params.getDeveloperPayload(), order.getDisplayOrderId());
        assertEquals(params.getPassParams(), new PassParams());
        assertEquals(params.getOrderCreateDate(), order.getCreationDate());
        assertEquals(params.getRgb(), order.getRgb());
        assertEquals(params.getServiceOrderId(), order.getId() + "-" + PaymentGoal.ORDER_POSTPAY.name().toLowerCase());
        assertEquals(params.getServiceProductId(), YA_MONEY_ID);
        assertEquals(BuyerProvider.UID, params.getUid().longValue());

        OneElementBackIterator<ServeEvent> serveEvents = trustMockConfigurer.eventsIterator();
        checkLoadPartnerCall(serveEvents, CLIENT_ID);
        checkOptionalCreateServiceProductCall(serveEvents, product(CLIENT_ID, "" + CLIENT_ID + "-" + YA_MONEY_ID,
                YA_MONEY_ID));

        assertFalse(serveEvents.hasNext());
    }

    @Test
    public void testListPaymentMethods() {
        final Long uid = 666333L;
        final String userIp = "127.127.127.4";

        trustService.listPaymentMethods(uid, userIp, null, Color.WHITE);

        Iterator<ServeEvent> serveEvents = trustMockConfigurer.eventsIterator();
        checkListPaymentMethodsCall(serveEvents, uid.toString(), userIp, null);
    }

    @Test
    public void testGetCashbackBalance() {
        final Long uid = 666333L;

        YandexCashbackInfo result =
                trustService.getYandexCashbackBalance(uid, Color.BLUE);
        assertNotNull(result.getBalance());
        assertTrue(result.isAccountCreated());
        final Iterator<ServeEvent> serveEvents = trustMockConfigurer.eventsGatewayIterator();
        checkListWalletBalanceMethodCall(serveEvents);
    }

    @Test
    public void createCashbackAccount() {
        final long uid = 666333L;
        String response = trustService.createCashbackAccount(uid, Color.BLUE);
        assertEquals("yandex_account-w/30b153cc-8e30-58e2-8d1a-1095bc49b915", response);
        Iterator<ServeEvent> serveEvents = trustMockConfigurer.eventsIterator();
        checkCreateAccountMethodCall(serveEvents, String.valueOf(uid));
    }

    @Test
    public void testGetCashbackBalanceNotFoundAccount() {
        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockEmptyListWalletBalanceResponse();

        final Long uid = 666123L;
        YandexCashbackInfo result = trustService.getYandexCashbackBalance(uid, Color.BLUE);
        assertEquals(BigDecimal.ZERO, result.getBalance());
        assertFalse(result.isAccountCreated());
    }

    @Test
    public void testUnbindCard() {
        final String sessionId = "PASSPORT_OAUTH_TOKEN";
        final String userIp = "127.127.127.4";
        final String cardId = "card_id_string";

        trustService.unbindCard(0L, sessionId, userIp, cardId, Color.WHITE);

        Iterator<ServeEvent> serveEvents = trustMockConfigurer.eventsIterator();
        checkUnbindCardCall(serveEvents, sessionId, userIp, cardId);
    }

    @Test
    public void testUnbindCardRetrySuccess() {
        trustMockConfigurer.mockUnbindCard404();

        final String sessionId = "PASSPORT_OAUTH_TOKEN";
        final String userIp = "127.127.127.4";
        final String cardId = "card_id_string";

        trustService.unbindCard(22L, sessionId, userIp, cardId, Color.WHITE);

        Iterator<ServeEvent> serveEvents = trustMockConfigurer.eventsIterator();
        checkUnbindCardCall(serveEvents, sessionId, userIp, cardId);
    }

    @Test
    public void testUnbindCardRetryFail() {
        trustMockConfigurer.mockUnbindCard404();
        trustMockConfigurer.mockUnbindCard404All();

        final String sessionId = "PASSPORT_OAUTH_TOKEN";
        final String userIp = "127.127.127.4";
        final String cardId = "card_id_string";

        assertThrows(ErrorCodeException.class, () -> {
            trustService.unbindCard(22L, sessionId, userIp, cardId, Color.WHITE);
        });
    }

    @Test
    public void testCreateCashBasket() {
        Order order = OrderProvider.getBlueOrder(o -> o.setId(1L));
        CashPaymentBuilder builder = new CashPaymentBuilder(order);
        builder.setUid(1L);
        builder.setPaymentId(2L);
        builder.setYandexUid("yandex-uid");
        builder.setDeliveryServiceClientId(DELIVERY_SERVICE_CLIENT_ID);
        builder.setCurrency(Currency.RUR);
        builder.setServiceUrl(UrlBuilder.fromString("http://gravicapa01ht.market.yandex.net:39001"));
        builder.setSendPassParams(true);
        builder.setLines(Collections.singletonList(new CreateBasketLine(
                "3039885",
                BigDecimal.ONE,
                new BigDecimal("3132")
        )));

        trustService.createBasket(builder, shopMetaData.getPrepayType(), Color.GREEN);

        Iterator<ServeEvent> serveEvents = trustMockConfigurer.eventsIterator();
        checkCreateBasketCall(serveEvents, createBasket()
                .withPayMethodId("cash-" + DELIVERY_SERVICE_CLIENT_ID)
                .withBackUrl(equalTo("http://gravicapa01ht.market.yandex.net:39001/payments/2/notify-basket"))
                .withUid(1L)
                .withCurrency(Currency.RUR)
                .withPassParams(notNullValue(String.class))
                .withYandexUid("yandex-uid")
                .withOrder("3039885", BigDecimal.ONE, new BigDecimal(3132))
                .withDeveloperPayload("{\"call_preview_payment\":\"card_info\"}")
        );
    }

    @Test
    public void testCreateCashBasketForMultiOrderWithSameServiceToken() {
        Order blueOrder = OrderProvider.getBlueOrder(o -> o.setId(1L));
        Order whiteOrder = OrderProvider.getColorOrder(Color.WHITE);
        whiteOrder.setId(2L);
        ArrayList<Order> orders = Lists.newArrayList(blueOrder, whiteOrder);
        SupplierPaymentBuilder builder = new SupplierPaymentBuilder(orders, 123L);
        builder.setUid(1L);
        builder.setPaymentId(2L);
        builder.setYandexUid("yandex-uid");
        builder.setCurrency(Currency.RUR);
        builder.setServiceUrl(UrlBuilder.fromString("http://gravicapa01ht.market.yandex.net:39001"));
        builder.setSendPassParams(true);
        builder.setLines(Stream.of("123", "456")
                .map(balanceOrderId -> new CreateBasketLine(
                        balanceOrderId,
                        BigDecimal.ONE,
                        BigDecimal.valueOf(3132)
                )).collect(Collectors.toList()));

        Set<Color> colors = new HashSet<>();
        colors.add(Color.BLUE);
        colors.add(Color.WHITE);
        trustService.createBasket(builder, shopMetaData.getPrepayType(), colors);

        Iterator<ServeEvent> serveEvents = trustMockConfigurer.eventsIterator();
        checkCreateBasketCall(serveEvents, createBasket()
                .withUserIp("127.0.0.1")
                .withPayMethodId("cash-" + DELIVERY_SERVICE_CLIENT_ID)
                .withBackUrl(equalTo("http://gravicapa01ht.market.yandex.net:39001/payments/2/notify-basket"))
                .withUid(1L)
                .withCurrency(Currency.RUR)
                .withPassParams(notNullValue(String.class))
                .withYandexUid("yandex-uid")
                .withPayMethodId("sberbank_credit")
                .withFiscalForce(1)
                .withPaymentTimeout(Matchers.equalTo("0"))
                .withDeveloperPayload("{\"origin_payment_id\":123,\"call_preview_payment\":\"card_info\"}")
                .withOrder("123", BigDecimal.ONE, new BigDecimal(3132))
                .withOrder("456", BigDecimal.ONE, new BigDecimal(3132))
        );
    }

    @Test
    public void testBatchCreateBalanceOrdersWithSameServiceTokens() {
        List<BalanceOrderParams> balanceOrderParams = new ArrayList<>();
        balanceOrderParams.add(new BalanceOrderParams(
                shopMetaData,
                "123",
                "",
                new PassParams(),
                "345",
                1L,
                Color.BLUE,
                new Date()));
        balanceOrderParams.add(new BalanceOrderParams(
                shopMetaData,
                "123",
                "",
                new PassParams(),
                "456",
                1L,
                Color.WHITE,
                new Date()));

        trustService.batchCreateBalanceOrders(shopMetaData.getPrepayType(), PaymentGoal.ORDER_POSTPAY,
                balanceOrderParams);

        Iterator<ServeEvent> serveEvents = trustMockConfigurer.eventsIterator();
        checkBatchServiceOrderCreationCall(serveEvents, 1L);
    }

    @Test
    public void testGetClientCreditInfoOnError() {
        TrustBasketKey basketKey = new TrustBasketKey(generateRandomTrustId(), generateRandomTrustId());
        Payment payment = new Payment();
        payment.setBasketKey(basketKey);
        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockRateLimitHit();

        assertThrows(TrustException.class, () -> trustService.getClientCreditInfo(payment, Color.BLUE));
    }

}
