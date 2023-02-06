package ru.yandex.market.checkout.checkouter.pay;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Iterables;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.cashier.CashierService;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.storage.payment.PaymentReadingDao;
import ru.yandex.market.checkout.checkouter.storage.payment.SurchargeInfoDao;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.ChangeOrderItemsHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.ClientHelper;
import ru.yandex.market.common.report.model.FoodtechType;
import ru.yandex.market.queuedcalls.QueuedCall;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.pay.PaymentStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.pay.PaymentStatus.HOLD;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_BASKET_STUB;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.getRequestBodyAsJson;

/**
 * @author zagidullinri
 * @date 28.03.2022
 */
public class SurchargePaymentTest extends AbstractWebTestBase {

    private static final String CARD_ID = "card-123f";
    private static final String LOGIN_ID = "login_id_123";

    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private CashierService cashierService;
    @Autowired
    private SurchargeInfoService surchargeInfoService;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private SurchargeInfoDao surchargeInfoDao;
    @Autowired
    private ChangeOrderItemsHelper changeOrderItemsHelper;
    @Autowired
    private OrderPayHelper paymentHelper;
    @Autowired
    private CheckouterClient checkouterAPI;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private PaymentReadingDao paymentReadingDao;

    @BeforeEach
    public void setUp() throws IOException {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_SURCHARGE_ORDER_ACTION, true);
        trustMockConfigurer.mockWholeTrust();
        trustMockConfigurer.mockBindings("bindings_surcharge_response.json");
    }

    @AfterEach
    public void tearDown() {
        trustMockConfigurer.resetAll();
    }

    @Test
    public void successfulSurchargePaymentShouldBeCleared() {
        trustMockConfigurer.mockWholeTrust();
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        order = orderService.getOrder(order.getId());
        Long uid = order.getBuyer().getUid();
        ClientInfo clientInfo = new ClientInfo(ClientRole.USER, uid);
        OrderItem orderItem = order.getItems().iterator().next();
        ItemQuantitySurcharge itemQuantitySurcharge =
                new ItemQuantitySurcharge(orderItem.getId(), BigDecimal.valueOf(1.1));

        Payment payment = cashierService.startSurchargePayment(
                order.getId(),
                Collections.singletonList(itemQuantitySurcharge),
                clientInfo,
                "1",
                null,
                false
        );
        orderPayHelper.notifyPayment(payment);
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        payment = paymentReadingDao.loadPayment(payment.getId());

        assertEquals(PaymentStatus.CLEARED, payment.getStatus());
    }

    @Test
    public void startSurchargePaymentShouldCreateProperReceiptTest() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        order = orderService.getOrder(order.getId());
        Long uid = order.getBuyer().getUid();
        ClientInfo clientInfo = new ClientInfo(ClientRole.USER, uid);
        OrderItem orderItem = order.getItems().iterator().next();
        assertThat(orderItem.getBuyerPrice(), equalTo(BigDecimal.valueOf(250).setScale(2)));

        List<ItemQuantitySurcharge> itemQuantitySurchargeList =
                Collections.singletonList(new ItemQuantitySurcharge(orderItem.getId(), BigDecimal.valueOf(1.1)));

        Payment payment = cashierService.startSurchargePayment(
                order.getId(),
                itemQuantitySurchargeList,
                clientInfo,
                "1",
                null,
                false
        );
        order = orderService.getOrder(order.getId());
        List<Receipt> receipts = receiptService.findByPayment(payment);

        assertThat(payment.getTotalAmount(), equalTo(BigDecimal.valueOf(275).setScale(2)));
        assertThat(order.getId(), equalTo(payment.getOrderId()));
        assertThat(receipts, hasSize(1));
        Receipt receipt = receipts.iterator().next();
        assertThat(receipt.getType(), equalTo(ReceiptType.INCOME));
        assertThat(receipt.getItems(), hasSize(1));
        ReceiptItem receiptItem = receipt.getItems().iterator().next();
        assertThat(receiptItem.getItemId(), equalTo(orderItem.getId()));
        assertThat(receiptItem.getQuantity().compareTo(BigDecimal.valueOf(1.1)), equalTo(0));
        assertThat(receiptItem.getAmount(), equalTo(BigDecimal.valueOf(275).setScale(2)));
        assertThat(receiptItem.getPrice(), equalTo(BigDecimal.valueOf(250).setScale(2)));
    }

    @Test
    public void startSurchargePaymentShouldCreateProperReceiptRequestsTest() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters());
        Long orderId = order.getId();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        order = orderService.getOrder(orderId);
        Long uid = order.getBuyer().getUid();
        ClientInfo clientInfo = new ClientInfo(ClientRole.USER, uid);
        OrderItem orderItem = order.getItems().iterator().next();
        Long itemId = orderItem.getId();
        assertThat(orderItem.getBuyerPrice(), equalTo(BigDecimal.valueOf(250).setScale(2)));

        List<ItemQuantitySurcharge> itemQuantitySurchargeList =
                Collections.singletonList(new ItemQuantitySurcharge(itemId, BigDecimal.valueOf(1.1)));

        cashierService.startSurchargePayment(
                orderId,
                itemQuantitySurchargeList,
                clientInfo,
                "1",
                null,
                false
        );
        List<ServeEvent> createBasketEvents = payInformation();

        assertThat(createBasketEvents, hasSize(1));
        ServeEvent createBasketEvent = Iterables.getOnlyElement(createBasketEvents);
        JsonObject body = getRequestBodyAsJson(createBasketEvent);
        assertThat(body.get("orders").getAsJsonArray().size(), IsEqual.equalTo(1));
        JsonElement orderJsonElement = body.get("orders").getAsJsonArray().iterator().next();
        Assertions.assertEquals(((JsonObject) orderJsonElement).get("order_id").toString(), "\"" +
                orderId + "-item-" + itemId + "-" + "surcharge\"");
        Assertions.assertEquals(((JsonObject) orderJsonElement).get("qty").toString(), "\"1.1\"");
        Assertions.assertEquals(((JsonObject) orderJsonElement).get("price").toString(), "\"250.00\"");
    }

    @Test
    public void startSurchargePaymentShouldSendMitQueryParam() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters());
        Long orderId = order.getId();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        order = orderService.getOrder(orderId);
        Long uid = order.getBuyer().getUid();
        ClientInfo clientInfo = new ClientInfo(ClientRole.USER, uid);
        OrderItem orderItem = order.getItems().iterator().next();
        Long itemId = orderItem.getId();

        List<ItemQuantitySurcharge> itemQuantitySurchargeList =
                Collections.singletonList(new ItemQuantitySurcharge(itemId, BigDecimal.valueOf(1.1)));

        cashierService.startSurchargePayment(
                orderId,
                itemQuantitySurchargeList,
                clientInfo,
                "1",
                null,
                false
        );
        List<ServeEvent> createBasketEvents = payInformation();

        assertThat(createBasketEvents, hasSize(1));
        ServeEvent createBasketEvent = Iterables.getOnlyElement(createBasketEvents);
        JsonObject body = getRequestBodyAsJson(createBasketEvent);
        assertTrue(body.get("mit").getAsJsonObject().get("create").getAsBoolean());
    }

    @Test
    public void afterEatsCheckoutShouldCreateSurchargeInfoTest() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setAsyncPaymentCardId(CARD_ID);
        parameters.setLoginId(LOGIN_ID);
        parameters.getReportParameters().setFoodtechType(FoodtechType.EDA_RETAIL.getId());
        Order order = orderCreateHelper.createOrder(parameters);

        SurchargeInfo surchargeInfo = getSurchargeInfo(order.getId());
        assertThat(surchargeInfo.getCardId(), equalTo(CARD_ID));
        assertThat(surchargeInfo.getLoginId(), equalTo(LOGIN_ID));
    }

    @Test
    public void qcWriteOffDebtTest() throws Exception {
        Parameters parameters = BlueParametersProvider.bluePrepaidWithCustomPrice(BigDecimal.valueOf(300));
        parameters.getOrders().get(0).getItems().iterator().next().setCount(2);

        parameters.getReportParameters().setFoodtechType(FoodtechType.EDA_RETAIL.getId());
        parameters.configuration().checkout().request().setAsyncPaymentCardId("card-2");

        Order order = orderCreateHelper.createOrder(parameters);

        OrderItem orderItem = order.getItems().iterator().next();

        orderItem.setCount(orderItem.getCount() + 1);

        paymentHelper.payForOrder(order);

        assertEquals(1, payInformation().size());

        Long orderId = order.getId();
        ResultActions response = changeOrderItemsHelper.changeOrderItems(Collections.singletonList(orderItem),
                ClientHelper.shopClientFor(order),
                orderId);

        response.andExpect(status().isOk());

        order = orderService.getOrder(orderId);

        assertEquals(SurchargeStatus.INIT, getSurchargeInfo(orderId).getStatus());

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        assertEquals(SurchargeStatus.IN_PROGRESS, getSurchargeInfo(orderId).getStatus());

        Optional<SurchargeInfo> infoDaoByOrder = surchargeInfoDao.findByOrder(orderId);
        assertTrue(infoDaoByOrder.isPresent());

        List<ItemQuantitySurcharge> orderItemToQuantityList = infoDaoByOrder.get().getOrderItemToQuantityList();
        assertNotNull(orderItemToQuantityList);

        getOnlyOneCreateSurchargePaymentQC(orderId);
        executeCreateSurchargePaymentQC();

        List<ServeEvent> firstPay = payInformation();
        assertEquals(2, firstPay.size());
        assertEquals("card-2", getRequestBodyAsJson(firstPay.get(1)).get("paymethod_id").getAsString());

        getOnlyOneCreateSurchargePaymentQC(orderId);

        Long paymentId = getSurchargeInfo(orderId).getCurrentPayment();
        assertNotNull(paymentId);
        paymentHelper.updatePaymentStatus(paymentId, HOLD);

        // чтобы наверняка delay отработал
        setFixedTime(Instant.now().plus(62, ChronoUnit.SECONDS));

        executeCreateSurchargePaymentQC();

        assertTrue(queuedCallService.findQueuedCalls(
                CheckouterQCType.CREATE_SURCHARGE_PAYMENT, orderId).isEmpty());

        checkSurchargeFinished(orderId);
    }

    @Test
    public void sumUserSurchargeNoDataTest() {
        UserSurchargeResponse surcharge = checkouterAPI.getUserSurcharge(-1);

        assertEquals(0, BigDecimal.ZERO.compareTo(surcharge.getUserSurcharge()));
    }

    @Test
    public void sumUserSurchargeOneNoteTest() {
        long firstUser = -2;
        long secondUser = -3;
        long otherUser = -4;

        List<SurchargeInfo> firstUserInfo = createFakeSurchargeInfo(firstUser, BigDecimal.valueOf(3.2));
        List<SurchargeInfo> secondUserInfo = createFakeSurchargeInfo(secondUser, BigDecimal.valueOf(3.2),
                BigDecimal.valueOf(5.2),
                BigDecimal.valueOf(4),
                BigDecimal.valueOf(700000));

        secondUserInfo.get(1).setPaymentId(777L);
        secondUserInfo.get(3).setStatus(SurchargeStatus.EXPIRED);

        transactionTemplate.execute(st -> {
            surchargeInfoDao.insert(firstUserInfo);
            surchargeInfoDao.insert(secondUserInfo);
            return null;
        });

        assertEquals(0, BigDecimal.valueOf(3.2).compareTo(
                checkouterAPI.getUserSurcharge(firstUser).getUserSurcharge()));

        assertEquals(0, BigDecimal.valueOf(7.2).compareTo(
                checkouterAPI.getUserSurcharge(secondUser).getUserSurcharge()));

        assertEquals(0, BigDecimal.ZERO.compareTo(
                checkouterAPI.getUserSurcharge(otherUser).getUserSurcharge()));
    }

    private List<SurchargeInfo> createFakeSurchargeInfo(long userId, BigDecimal... debtSum) {

        return Arrays.stream(debtSum).map(it -> {
            Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
            Order order = orderCreateHelper.createOrder(parameters);

            SurchargeInfo surchargeInfo = new SurchargeInfo();
            surchargeInfo.setOrderId(order.getId());
            surchargeInfo.setUserId(userId);
            surchargeInfo.setSurchargeTotal(it);
            surchargeInfo.setStatus(SurchargeStatus.IN_PROGRESS);
            return surchargeInfo;
        }).collect(Collectors.toList());
    }

    private List<ServeEvent> payInformation() {
        return trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_BASKET_STUB))
                .collect(Collectors.toList());
    }

    @Test
    public void updateAndReadUsedCardsInformationTest() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());

        surchargeInfoService.initSurchargeData(List.of(order), "1", "2");
        List<String> usedCards = List.of("4", "2", "1", "7");
        Long orderId = order.getId();
        surchargeInfoService.updateCurrentPayment(orderId, usedCards, null);

        SurchargeInfo surchargeInfo = getSurchargeInfo(orderId);
        assertEquals(usedCards, surchargeInfo.getUsedCards());
        assertNull(surchargeInfo.getPaymentId());

        surchargeInfoService.bindPayment(orderId, new Payment() {{
            setId(777L);
        }});

        assertTrue(getSurchargeInfo(orderId).getUsedCards().isEmpty());
    }

    @Test
    public void retrySurchargeAllCardsTest() {
        Order order = createHandMadeSurchargeQC();
        Long orderId = order.getId();

        executeCreateSurchargePaymentQC();
        SurchargeInfo firstSurchargeInfo = getSurchargeInfo(orderId);
        List<ServeEvent> firstPay = payInformation();
        assertEquals(2, firstPay.size());
        assertEquals(List.of("card-2"), firstSurchargeInfo.getUsedCards());
        assertEquals("card-2", getRequestBodyAsJson(firstPay.get(1)).get("paymethod_id").getAsString());

        // чтобы наверняка delay отработал (но без смены статуса ничего не должно поменяться)
        setFixedTime(getClock().instant().plus(62, ChronoUnit.SECONDS));
        executeCreateSurchargePaymentQC();
        getOnlyOneCreateSurchargePaymentQC(orderId);
        SurchargeInfo secondSurchargeInfo = getSurchargeInfo(orderId);
        List<ServeEvent> secondPay = payInformation();
        // платеж не сменился
        assertEquals(firstSurchargeInfo.getCurrentPayment(), secondSurchargeInfo.getCurrentPayment());
        assertEquals(2, secondPay.size());

        // отменяем, чтобы проверить смещение в рамках первого промежутка = retryFirstPeriodInterval
        paymentHelper.updatePaymentStatus(firstSurchargeInfo.getCurrentPayment(), CANCELLED);
        // чтобы наверняка delay отработал
        setFixedTime(getClock().instant().plus(62, ChronoUnit.SECONDS));
        executeCreateSurchargePaymentQC();
        getOnlyOneCreateSurchargePaymentQC(orderId);
        checkSurchargeInfoIsReset(orderId);
        checkNextTryIn12Hours(orderId);

        // смотрим что будет через 12 часов
        // (должна взяться карта card-2 как единственный доступный вариант списания в первые 24 часа)
        setFixedTime(getClock().instant().plus(12, ChronoUnit.HOURS));
        executeCreateSurchargePaymentQC();
        // вызов делается 4ый раз, а списание 3
        SurchargeInfo forthSurchargeInfo = getSurchargeInfo(orderId);
        List<ServeEvent> thirdPay = payInformation();
        // оплата сменилась
        assertNotEquals(firstSurchargeInfo.getCurrentPayment(), forthSurchargeInfo.getCurrentPayment());
        assertEquals(3, thirdPay.size());
        assertEquals("card-2", getRequestBodyAsJson(thirdPay.get(2)).get("paymethod_id").getAsString());

        // отменяем, чтобы взялась следующая 3 карта на списание, а не смещение
        paymentHelper.updatePaymentStatus(forthSurchargeInfo.getCurrentPayment(), CANCELLED);
        setFixedTime(getClock().instant().plus(62, ChronoUnit.SECONDS));
        executeCreateSurchargePaymentQC();
        // снова смещаемся на 12 часов, поскольку с карты card-2 не удалось списать сумму
        QueuedCall callSec12 = getOnlyOneCreateSurchargePaymentQC(orderId);
        Duration betweenCreatedAndNextSec12 = Duration.between(callSec12.getCreatedAt(), callSec12.getNextTryAt());
        // 23 и 25 потому, что разница между созданием, а не предыдущей попыткой
        assertThat(betweenCreatedAndNextSec12, lessThan(Duration.ofHours(25)));
        assertThat(betweenCreatedAndNextSec12, not(lessThan(Duration.ofHours(23))));

        // смотрим что будет через 12 часов
        // (должны взяться остальные 2 карты: card-1, card-3, причем в следующем порядке 2, 3, 1)
        setFixedTime(getClock().instant().plus(12, ChronoUnit.HOURS));
        executeCreateSurchargePaymentQC();
        // вызов делается 5ый раз, а списание 4
        SurchargeInfo fifthSurchargeInfo = getSurchargeInfo(orderId);
        List<ServeEvent> forthPay = payInformation();
        // оплата сменилась
        assertNotEquals(forthSurchargeInfo.getCurrentPayment(), fifthSurchargeInfo.getCurrentPayment());
        assertEquals(4, forthPay.size());
        assertEquals("card-2", getRequestBodyAsJson(forthPay.get(3)).get("paymethod_id").getAsString());

        // отменяем, чтобы взялась следующая 1 карта на списание, а не смещение
        paymentHelper.updatePaymentStatus(fifthSurchargeInfo.getCurrentPayment(), CANCELLED);
        setFixedTime(getClock().instant().plus(62, ChronoUnit.SECONDS));
        executeCreateSurchargePaymentQC();
        // вызов делается 6ой раз, а списание 5
        SurchargeInfo sixthSurchargeInfo = getSurchargeInfo(orderId);
        List<ServeEvent> fifthPay = payInformation();
        // оплата сменилась
        assertNotEquals(fifthSurchargeInfo.getCurrentPayment(), sixthSurchargeInfo.getCurrentPayment());
        assertEquals(5, fifthPay.size());
        assertEquals("card-3", getRequestBodyAsJson(fifthPay.get(4)).get("paymethod_id").getAsString());

        setFixedTime(getClock().instant().plus(62, ChronoUnit.SECONDS));
        paymentHelper.updatePaymentStatus(sixthSurchargeInfo.getCurrentPayment(), CANCELLED);
        executeCreateSurchargePaymentQC();
        // вызов делается 7ой раз, а списание 6
        SurchargeInfo seventhSurchargeInfo = getSurchargeInfo(orderId);
        List<ServeEvent> sixthPay = payInformation();
        // оплата сменилась
        assertNotEquals(sixthSurchargeInfo.getCurrentPayment(), seventhSurchargeInfo.getCurrentPayment());
        assertEquals(6, sixthPay.size());
        assertEquals("card-1", getRequestBodyAsJson(sixthPay.get(5)).get("paymethod_id").getAsString());

        paymentHelper.updatePaymentStatus(seventhSurchargeInfo.getCurrentPayment(), CANCELLED);
        // отрабатывает delay и уходим на следующий круг, который должен составить 24 часа
        setFixedTime(getClock().instant().plus(62, ChronoUnit.SECONDS));
        executeCreateSurchargePaymentQC();
        checkNextTryIn48Hours(orderId);

        // смещаемся еще на 24 часа (2 день), чтобы начат списание со всех карт
        setFixedTime(getClock().instant().plus(24, ChronoUnit.HOURS));
        // заходим на финальные итерации. Проверим, что карты снова перебираются и остановимся
        executeCreateSurchargePaymentQC();
        // вызов делается 8ой раз, а списание 7
        SurchargeInfo eighthSurchargeInfo = getSurchargeInfo(orderId);
        List<ServeEvent> seventhPay = payInformation();
        // оплата сменилась
        assertNotEquals(seventhSurchargeInfo.getCurrentPayment(), eighthSurchargeInfo.getCurrentPayment());
        assertEquals(7, seventhPay.size());
        assertEquals("card-2", getRequestBodyAsJson(seventhPay.get(6)).get("paymethod_id").getAsString());

        paymentHelper.updatePaymentStatus(eighthSurchargeInfo.getCurrentPayment(), CANCELLED);
        setFixedTime(getClock().instant().plus(62, ChronoUnit.SECONDS));
        executeCreateSurchargePaymentQC();
        // вызов делается 9ый раз, а списание 8
        SurchargeInfo ninthSurchargeInfo = getSurchargeInfo(orderId);
        List<ServeEvent> eighthPay = payInformation();
        // оплата сменилась
        assertNotEquals(eighthSurchargeInfo.getCurrentPayment(), ninthSurchargeInfo.getCurrentPayment());
        assertEquals(8, eighthPay.size());
        assertEquals("card-3", getRequestBodyAsJson(eighthPay.get(7)).get("paymethod_id").getAsString());

        paymentHelper.updatePaymentStatus(ninthSurchargeInfo.getCurrentPayment(), HOLD);
        setFixedTime(getClock().instant().plus(62, ChronoUnit.SECONDS));
        executeCreateSurchargePaymentQC();
        assertTrue(queuedCallService.findQueuedCalls(
                CheckouterQCType.CREATE_SURCHARGE_PAYMENT, orderId).isEmpty());
        checkSurchargeFinished(orderId);
    }

    private void checkSurchargeInfoIsReset(Long orderId) {
        SurchargeInfo thirdSurchargeInfo = getSurchargeInfo(orderId);
        assertTrue(thirdSurchargeInfo.getUsedCards().isEmpty());
        assertNull(thirdSurchargeInfo.getCurrentPayment());
        assertNull(thirdSurchargeInfo.getPaymentId());
    }

    private void checkNextTryIn48Hours(Long orderId) {
        QueuedCall call24 = getOnlyOneCreateSurchargePaymentQC(orderId);

        Duration betweenCreatedAndNext24 = Duration.between(call24.getCreatedAt(), call24.getNextTryAt());

        assertThat(betweenCreatedAndNext24, not(lessThan(Duration.ofHours(47))));
        assertThat(betweenCreatedAndNext24, lessThan(Duration.ofHours(49)));
    }

    private void checkNextTryIn12Hours(Long orderId) {
        QueuedCall call12 = getOnlyOneCreateSurchargePaymentQC(orderId);

        Duration betweenCreatedAndNext12 = Duration.between(call12.getCreatedAt(), call12.getNextTryAt());

        assertThat(betweenCreatedAndNext12, not(lessThan(Duration.ofHours(11))));
        assertThat(betweenCreatedAndNext12, lessThan(Duration.ofHours(13)));
    }

    @Test
    public void checkSurchargeExpirationEndsWithSuccessTest() {
        Order order = createHandMadeSurchargeQC();
        Long orderId = order.getId();

        // первый вызов, чтобы добавилась карта и по ней пошли ретраи
        // сразу делаем смещение на 3 месяца, чтобы убедиться в ожидании крайнего списания и прекращения работы QC
        executeCreateSurchargePaymentQC();

        // делаем смещение по времени
        setFixedTime(getClock().instant().plus(2160, ChronoUnit.HOURS));

        executeCreateSurchargePaymentQC();
        getOnlyOneCreateSurchargePaymentQC(orderId);

        setFixedTime(getClock().instant().plus(62, ChronoUnit.SECONDS));

        // до сих пор проверяем, однако уже больше 3 месяцев прошло
        executeCreateSurchargePaymentQC();
        getOnlyOneCreateSurchargePaymentQC(orderId);

        SurchargeInfo lastTry = getSurchargeInfo(orderId);

        // и о чудо, деньги появились
        paymentHelper.updatePaymentStatus(lastTry.getCurrentPayment(), HOLD);
        setFixedTime(getClock().instant().plus(62, ChronoUnit.SECONDS));

        executeCreateSurchargePaymentQC();

        checkSurchargeFinished(orderId);
    }

    @Test
    public void checkSurchargeExpirationEndsWithCancelledTest() {
        Order order = createHandMadeSurchargeQC();
        Long orderId = order.getId();

        // первый вызов, чтобы добавилась карта и по ней пошли ретраи
        // сразу делаем смещение на 3 месяца, чтобы убедиться в ожидании крайнего списания и прекращения работы QC
        executeCreateSurchargePaymentQC();

        // делаем смещение по времени
        setFixedTime(getClock().instant().plus(2160, ChronoUnit.HOURS));

        executeCreateSurchargePaymentQC();
        getOnlyOneCreateSurchargePaymentQC(orderId);

        SurchargeInfo lastTry = getSurchargeInfo(orderId);

        setFixedTime(getClock().instant().plus(62, ChronoUnit.SECONDS));

        paymentHelper.updatePaymentStatus(lastTry.getCurrentPayment(), CANCELLED);

        executeCreateSurchargePaymentQC();

        SurchargeInfo finalTry = getSurchargeInfo(orderId);

        assertNull(finalTry.getPaymentId());
        assertNull(finalTry.getCurrentPayment());
        assertEquals(Collections.emptyList(), finalTry.getUsedCards());
        assertEquals(SurchargeStatus.EXPIRED, finalTry.getStatus());
    }

    private Order createHandMadeSurchargeQC() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        surchargeInfoService.initSurchargeData(List.of(order), "", "card-2");

        Long orderId = order.getId();

        transactionTemplate.execute(st -> {
            queuedCallService.addQueuedCall(CheckouterQCType.CREATE_SURCHARGE_PAYMENT, orderId);
            return null;
        });

        paymentHelper.payForOrder(order);

        order = orderService.getOrder(orderId);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        return order;
    }

    private void checkSurchargeFinished(Long orderId) {
        SurchargeInfo tenthSurchargeInfo = getSurchargeInfo(orderId);

        assertNotNull(tenthSurchargeInfo.getPaymentId());
        assertNull(tenthSurchargeInfo.getCurrentPayment());
        assertEquals(Collections.emptyList(), tenthSurchargeInfo.getUsedCards());
        assertEquals(SurchargeStatus.FINISHED, tenthSurchargeInfo.getStatus());
    }

    private QueuedCall getOnlyOneCreateSurchargePaymentQC(Long orderId) {
        Collection<QueuedCall> queuedCalls =
                queuedCallService.findQueuedCalls(CheckouterQCType.CREATE_SURCHARGE_PAYMENT, orderId);
        assertThat(queuedCalls, hasSize(1));
        return queuedCalls.iterator().next();
    }

    private SurchargeInfo getSurchargeInfo(Long orderId) {
        Optional<SurchargeInfo> surchargeInfo = surchargeInfoService.findByOrder(orderId);
        assertTrue(surchargeInfo.isPresent());
        return surchargeInfo.get();
    }

    private void executeCreateSurchargePaymentQC() {
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.CREATE_SURCHARGE_PAYMENT);
    }
}
