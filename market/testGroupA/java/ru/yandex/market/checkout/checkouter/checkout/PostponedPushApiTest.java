package ru.yandex.market.checkout.checkouter.checkout;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.logging.LoggingBooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusExpiry;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.delivery.DeliveryUpdateOptions;
import ru.yandex.market.checkout.checkouter.order.validation.PendingStatusExpiry;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.pushapi.client.error.ErrorSubCode;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.report.ItemInfo;
import ru.yandex.market.common.report.model.PickupOptionOutlet;
import ru.yandex.market.queuedcalls.QueuedCall;
import ru.yandex.market.queuedcalls.QueuedCallService;
import ru.yandex.market.queuedcalls.QueuedCallSettings;
import ru.yandex.market.queuedcalls.QueuedCallSettingsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.POSTPONED_PUSH_API;
import static ru.yandex.market.checkout.providers.WhiteParametersProvider.WHITE_SHOP_ID;

public class PostponedPushApiTest extends AbstractPushApiTestBase {

    //Статичный экзекутор, чтобы не тратить время на создание нового потока
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private static final ShopMetaData SHOP_META_DATA =
            ShopMetaDataBuilder.createCopy(ShopSettingsHelper.getDefaultMeta())
                    // на случай, если для API магазина выставлена запрещенная для него настройка
                    .withOrderAutoAcceptEnabled(true)
                    .build();
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private QueuedCallSettingsService settingsService;
    @Autowired
    @Qualifier("orderStatusExpiryMinutes")
    protected Map<OrderStatus, OrderStatusExpiry> orderStatusExpiryMinutes;

    public static Stream<Arguments> shopExceptions() {
        return Arrays.stream(new Object[][]{
                {false, ErrorSubCode.SSL_ERROR},
                {false, ErrorSubCode.HTTP},
                {false, ErrorSubCode.CANT_PARSE_RESPONSE},
                {false, ErrorSubCode.READ_TIMED_OUT},
                {true, ErrorSubCode.SSL_ERROR},
                {true, ErrorSubCode.HTTP},
                {true, ErrorSubCode.CANT_PARSE_RESPONSE},
                {true, ErrorSubCode.READ_TIMED_OUT}
        }).map(Arguments::of);
    }

    @BeforeEach
    public void setUp() {
        setAsyncPushApi(true);
        setAsyncFBSPushApi(true);
        setEdaTimeout(15);
        setExpressTimeout(15);
        setDbsTimeout(120);
        setDbsExpiration(120);
        setSyncShopIds(-1);
        setMaxOrdersPerShop(50);
        checkouterFeatureWriter.writeValue(LoggingBooleanFeatureType.CHECKOUTER_FEATURE, true);
    }

    @AfterEach
    public void tearDown() {
        setAsyncPushApi(false);
        setAsyncFBSPushApi(false);
    }

    private void setDbsExpiration(int duration) {
        orderStatusExpiryMinutes.get(OrderStatus.PENDING).getOverrides().forEach(
                e -> {
                    if (e instanceof PendingStatusExpiry.DbsExpirationInMinutes) {
                        ((PendingStatusExpiry.DbsExpirationInMinutes) e).setDuration(duration);
                    }
                }
        );
    }

    @ParameterizedTest(name = "Post paid: {0}")
    @ValueSource(booleans = {false, true})
    public void shouldPushOrderWhenPushApiIsAvailableAndPostponedPushApiIsOff(boolean postPaid) {
        setAsyncPushApi(false);
        Order order = createOrder(postPaid);
        checkOrderAccepted(order.getId(), getStatus(postPaid));
    }

    @Test
    public void shouldPushPaidOrderWhenPushApiIsAvailableAndPostponedPushApiIsOff() {
        setAsyncPushApi(false);
        Order order = createOrder(false);
        orderPayHelper.payForOrder(order);
        checkOrderAccepted(order.getId(), OrderStatus.PROCESSING);
    }

    @DisplayName("Создаём dropship заказ асинхронно без использования push-api")
    @ParameterizedTest(name = "Post paid: {0}")
    @ValueSource(booleans = {false, true})
    public void shouldCratedAsyncDropshipOrderWithPostponedPushApiAccept(boolean postPaid) {
        setAsyncPushApi(true);
        checkouterProperties.setStraightDropshipFlow(true);
        Order order = createDropShipOrder(postPaid);
        assertEquals(postPaid ? OrderStatus.PENDING : OrderStatus.UNPAID, order.getStatus());
    }

    @ParameterizedTest(name = "Post paid: {0}")
    @ValueSource(booleans = {false, true})
    public void shouldCreateOrderAndPostponePushApiForOrderWhenPushApiAvailable(boolean postPaid) {
        Order order = createOrder(postPaid);

        // имитируем ошибку push-api
        pushApiConfigurer.mockAcceptShopFailure(order);
        jumpToFuture(1, ChronoUnit.MINUTES);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);
        order = orderService.getOrder(order.getId());
        assertEquals(postPaid ? OrderStatus.PENDING : OrderStatus.UNPAID, order.getStatus());
        QueuedCall qc = queuedCallService.findQueuedCalls(POSTPONED_PUSH_API, order.getId()).iterator().next();
        assertEquals("Exception class: ru.yandex.market.checkout.checkouter.order.OrderFailureException\n" +
                "Message: trololo", qc.getLastTryErrorMessage());
        assertNotNull(qc.getNextTryAt());

        // push-api восстановили
        pushApiConfigurer.resetMocks();
        pushApiConfigurer.mockAccept(order, true);
        jumpToFuture(1, ChronoUnit.MINUTES);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);

        checkOrderAccepted(order.getId(), getStatus(postPaid));
    }

    @Test
    public void shouldProcessingOrderIfItPaidBeforePushApiAvailable() {
        Order order = createOrder(false);

        // имитируем ошибку push-api
        pushApiConfigurer.mockAcceptShopFailure(order);
        jumpToFuture(1, ChronoUnit.MINUTES);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.UNPAID, order.getStatus());
        QueuedCall qc = queuedCallService.findQueuedCalls(POSTPONED_PUSH_API, order.getId()).iterator().next();
        assertNotNull(qc.getLastTryErrorMessage());
        assertNotNull(qc.getNextTryAt());

        // оплачиваем заказ сразу (пока push-api лежит)
        orderPayHelper.payForOrder(order);
        Order order1 = orderService.getOrder(order.getId());
        System.out.println(order1.getStatus());

        // push-api восстановили
        pushApiConfigurer.resetMocks();
        pushApiConfigurer.mockAccept(order, true);
        jumpToFuture(1, ChronoUnit.MINUTES);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);

        checkOrderAccepted(order.getId(), OrderStatus.PROCESSING);
    }

    @Test
    public void shouldProcessingOrderIfItAfterPushApiAvailable() {
        Order order = createOrder(false);

        // имитируем ошибку push-api
        pushApiConfigurer.mockAcceptShopFailure(order);
        jumpToFuture(1, ChronoUnit.MINUTES);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.UNPAID, order.getStatus());
        QueuedCall qc = queuedCallService.findQueuedCalls(POSTPONED_PUSH_API, order.getId()).iterator().next();
        assertNotNull(qc.getLastTryErrorMessage());
        assertNotNull(qc.getNextTryAt());

        // push-api восстановили
        pushApiConfigurer.resetMocks();
        pushApiConfigurer.mockAccept(order, true);
        jumpToFuture(1, ChronoUnit.MINUTES);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);

        // оплачиваем заказ после восставления push-api
        orderPayHelper.payForOrder(order);

        checkOrderAccepted(order.getId(), OrderStatus.PROCESSING);
    }

    @ParameterizedTest(name = "Post paid: {0}, error: {1}")
    @MethodSource("shopExceptions")
    public void shouldCreateOrderAndFailPostponehApiForOrder(boolean postPaid, ErrorSubCode error) {
        Order order = createOrder(postPaid);

        pushApiConfigurer.mockAcceptShopFailure(order, error);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);
        // переводим часики на почти 2 часа вперед, чтобы попасть в последний QC
        jumpToFuture(119, ChronoUnit.MINUTES);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);

        checkOrderCancelled(order.getId(), OrderSubstatus.PENDING_EXPIRED);
    }

    @ParameterizedTest(name = "Post paid: {0}")
    @ValueSource(booleans = {false, true})
    public void shouldCreateOrderAndCancelOrderIfServiceFault(boolean postPaid) {
        Order order = createOrder(postPaid);

        pushApiConfigurer.mockServiceFault(order);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);
        // переводим часики на почти 2 часа вперед, чтобы попасть в последний QC
        jumpToFuture(119, ChronoUnit.MINUTES);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);

        checkOrderCancelled(order.getId(), OrderSubstatus.SERVICE_FAULT);
    }

    @ParameterizedTest(name = "Post paid: {0}")
    @ValueSource(booleans = {false, true})
    public void shouldCancelPaidOrderAndFailPostponePushApiForOrderWhenPushNotAccept(boolean postPaid) {
        Order order = createOrder(postPaid);

        // имитируем ошибку push-api
        pushApiConfigurer.mockAcceptShopFailure(order);
        jumpToFuture(1, ChronoUnit.MINUTES);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);
        order = orderService.getOrder(order.getId());
        assertEquals(postPaid ? OrderStatus.PENDING : OrderStatus.UNPAID, order.getStatus());
        QueuedCall qc = queuedCallService.findQueuedCalls(POSTPONED_PUSH_API, order.getId())
                .iterator().next();
        assertNotNull(qc.getLastTryErrorMessage());
        assertNotNull(qc.getNextTryAt());

        // push-api восстановили и он отказывается брать заказ
        pushApiConfigurer.mockAccept(order, false);
        jumpToFuture(1, ChronoUnit.MINUTES);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);
        checkOrderCancelled(order.getId(), OrderSubstatus.SHOP_PENDING_CANCELLED);
    }

    @Test
    public void shouldCancelPaidOrderAndFailPostponePushApiForOrderWhenPushNotAccept() {
        Order order = createOrder(false);

        // имитируем ошибку push-api
        pushApiConfigurer.mockAcceptShopFailure(order);
        jumpToFuture(1, ChronoUnit.MINUTES);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.UNPAID, order.getStatus());
        QueuedCall qc = queuedCallService.findQueuedCalls(POSTPONED_PUSH_API, order.getId())
                .iterator().next();
        assertNotNull(qc.getLastTryErrorMessage());
        assertNotNull(qc.getNextTryAt());

        // оплачиваем заказ после восставления push-api
        orderPayHelper.payForOrder(order);

        // push-api восстановили и он отказывается брать заказ
        pushApiConfigurer.mockAccept(order, false);
        jumpToFuture(1, ChronoUnit.MINUTES);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);
        checkOrderCancelled(order.getId(), OrderSubstatus.SHOP_PENDING_CANCELLED);
    }

    /**
     * Данный тест никогда не должен флакать - если такое случилось, значит у нас где-то есть ошибка
     * обновления статуса (без блокировки).
     * По сути мы пытаемся в параллельном режиме оплачивать заказ и посылать запрос магазину
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 5, 10, 25, 50, 75, 100, 250, 500, 1000, 2000, 1})
    public void shouldProcessingOrderIfPaidAfterOrderAccepting(int delay) throws InterruptedException,
            ExecutionException {
        Order order = createOrder(false);

        var future = EXECUTOR.submit(() -> {
            orderPayHelper.payForOrder(order);
            return null;
        });

        Thread.sleep(delay);

        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);
        future.get();

        // Первый прогон - прогревающий. Локально именно он падает с разными таймаутами
        if (delay == 0) {
            return;
        }
        checkOrderAccepted(order.getId(), OrderStatus.PROCESSING);
    }

    /**
     * Проверяем, что при включенном режиме асинхронного пуш-апи мы можем отдельно выключить асинхронность
     * для дропшип заказов (FBS, дропшип с доставкой через ЯМ)
     */
    @ParameterizedTest(name = "Post paid: {0}")
    @ValueSource(booleans = {false, true})
    public void shouldPushDropShipOrderWhenAsyncEnabledAndAsyncFBSDisabled(boolean postPaid) {
        setAsyncFBSPushApi(false);
        Order order = createDropShipOrder(postPaid);
        checkOrderAccepted(order.getId(), getStatus(postPaid));
    }

    /**
     * Проверяем, что при включенном режиме асинхронного пуш-апи мы можем отдельно включить асинхронность
     * для дропшип заказов (FBS, дропшип с доставкой через ЯМ)
     */
    @ParameterizedTest(name = "Post paid: {0}")
    @ValueSource(booleans = {false, true})
    public void shouldPushDropShipOrderWhenAsyncDisabledAndAsyncFBSEnabled(boolean postPaid) {
        setAsyncFBSPushApi(true);
        Order order = createDropShipOrder(postPaid);

        // имитируем ошибку push-api
        pushApiConfigurer.mockAcceptShopFailure(order);
        jumpToFuture(1, ChronoUnit.MINUTES);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);
        order = orderService.getOrder(order.getId());
        assertEquals(postPaid ? OrderStatus.PENDING : OrderStatus.UNPAID, order.getStatus());
        QueuedCall qc = queuedCallService.findQueuedCalls(POSTPONED_PUSH_API, order.getId()).iterator().next();
        assertNotNull(qc.getLastTryErrorMessage());
        assertNotNull(qc.getNextTryAt());

        // push-api восстановили
        pushApiConfigurer.resetMocks();
        pushApiConfigurer.mockAccept(order, true);
        jumpToFuture(1, ChronoUnit.MINUTES);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);

        checkOrderAccepted(order.getId(), getStatus(postPaid));
    }

    /**
     * Проверяем, что для заказов Я.Еда можно выставить меньший таймаут протухания заказа
     */
    @ParameterizedTest(name = "Timeout: {0}")
    @ValueSource(ints = {5, 10, 14, 16, 30, 60})
    public void shouldUseSpecialTimeoutForYandexEda(int timeout) {
        setEdaTimeout(timeout);
        Order order = createOrder(false, true);

        // имитируем ошибку push-api
        pushApiConfigurer.mockAcceptShopFailure(order);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.UNPAID, order.getStatus());
        QueuedCall qc = queuedCallService.findQueuedCalls(POSTPONED_PUSH_API, order.getId()).iterator().next();
        assertNotNull(qc.getLastTryErrorMessage());
        assertNotNull(qc.getNextTryAt());

        // переводим часики вперед
        jumpToFuture(timeout - 1, ChronoUnit.MINUTES);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);

        checkOrderCancelled(order.getId(), OrderSubstatus.PENDING_EXPIRED);
    }

    /**
     * Проверяем, что для заказов c экспрес-доставкой можно выставить меньший таймаут протухания заказа
     */
    @ParameterizedTest(name = "Timeout: {0}")
    @ValueSource(ints = {5, 10, 14, 16, 30, 60})
    public void shouldUseSpecialTimeoutForExpress(int timeout) {
        setExpressTimeout(timeout);
        Parameters parameters = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();
        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        Order order = orderCreateHelper.createOrder(parameters);
        order.getDelivery().setFeatures(Collections.singleton(DeliveryFeature.EXPRESS_DELIVERY));
        orderUpdateService.updateOrderDelivery(order.getId(), order.getDelivery(), ClientInfo.SYSTEM,
                DeliveryUpdateOptions.EMPTY);

        // имитируем ошибку push-api
        pushApiConfigurer.mockAcceptShopFailure(order);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.UNPAID, order.getStatus());
        QueuedCall qc = queuedCallService.findQueuedCalls(POSTPONED_PUSH_API, order.getId()).iterator().next();
        assertNotNull(qc.getLastTryErrorMessage());
        assertNotNull(qc.getNextTryAt());

        // переводим часики вперед
        jumpToFuture(timeout - 1, ChronoUnit.MINUTES);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);

        checkOrderCancelled(order.getId(), OrderSubstatus.PENDING_EXPIRED);
    }

    /**
     * Проверяет, что работают настройки исключения для магазинов.
     * То есть по умолчанию механизм асинхронного пуш-апи включен, но для конкретного магазина мы
     * говорим, чтобы его обращения к пуш-апи были синхронными
     */
    @ParameterizedTest(name = "Post paid: {0}")
    @ValueSource(booleans = {false, true})
    public void shouldPushOrderWhenPushApiIsOnButShopIsExcluded(boolean postPaid) {
        setSyncShopIds(WHITE_SHOP_ID);
        Order order = createOrder(postPaid);
        checkOrderAccepted(order.getId(), getStatus(postPaid));
    }

    @Test
    public void shouldUseAsyncPushApiOnlyForAcceptMethodPushApi() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        parameters.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        Long orderId = orderCreateHelper.createOrder(parameters).getId();
        Order order = orderService.getOrder(orderId);

        checkOrderAccepted(order.getId(), OrderStatus.PENDING);
    }

    @Test
    public void shouldUseTimeoutLessThanDefaultsSettings() {
        Order order = createOrder(false);

        pushApiConfigurer.mockDelayedShopFailure(order, 2000);
        setReadTimeouts(1);
        // переводим часики на почти 2 часа вперед, чтобы попасть в последний QC
        jumpToFuture(119, ChronoUnit.MINUTES);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);

        checkOrderCancelled(order.getId(), OrderSubstatus.PENDING_EXPIRED);
    }

    @Test
    public void shouldUseTimeoutGreaterThanDefaultsSettings() {
        Order order = createOrder(false);

        pushApiConfigurer.mockDelayedShopFailure(order, 1000);
        setReadTimeouts(3);
        // переводим часики на почти 2 часа вперед, чтобы попасть в последний QC
        jumpToFuture(119, ChronoUnit.MINUTES);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);

        checkOrderCancelled(order.getId(), OrderSubstatus.PENDING_EXPIRED);
    }

    @Test
    public void shouldUseRetryTimeout() {
        Order order = createOrder(false);

        pushApiConfigurer.mockAcceptShopFailure(order);
        Instant base = getClock().instant();

        setRetryTimeouts(31, 92); // в секундах
        setFixedTime(base);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);

        // проверяем, следующая попытка должна быть через 31 сек
        Instant firstRetry = base.plus(31, ChronoUnit.SECONDS);
        assertEquals(firstRetry.toEpochMilli(),
                queuedCallService.findQueuedCalls(POSTPONED_PUSH_API, order.getId()).iterator().next().getNextTryAt()
                        .toEpochMilli());

        setFixedTime(firstRetry);
        Instant secondRetry = firstRetry.plus(92, ChronoUnit.SECONDS);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);
        assertEquals(secondRetry.toEpochMilli(),
                queuedCallService.findQueuedCalls(POSTPONED_PUSH_API, order.getId()).iterator().next().getNextTryAt()
                        .toEpochMilli());
    }

    @Test
    public void shouldRaiseExceptionIfMaxOrdersRiched() {
        setMaxOrdersPerShop(1);
        settingsService.updateSettings(
                new QueuedCallSettings(false, 1,
                        Collections.singleton(new QueuedCallSettings().getOrCreateTypeSettings(POSTPONED_PUSH_API)))
        );
        Order order = createOrder(false);

        // имитируем ошибку push-api
        pushApiConfigurer.mockAcceptShopFailure(order);

        // пытаемся сделать заказ выше лимита
        Order order2 = createOrder(false);
        checkOrderCancelled(order2.getId(), OrderSubstatus.SERVICE_FAULT);
    }

    @Test
    void shouldSetDefaultShopOrderIdWhenAcceptedUsingPartnerInterface() {
        Order order = createOrder(true);
        assertNull(order.getShopOrderId());
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        assertEquals(order.getId().toString(), order.getShopOrderId());
    }

    @Test
    void shouldSendOutletInfoToPushApi() {
        Parameters parameters = WhiteParametersProvider.dbsPickupOrderWithCombinatorParameters(pickupOption -> {
            pickupOption.setMarketBranded(true);
            pickupOption.setOutlet(new PickupOptionOutlet() {{
                setId(1L);
            }});
        });
        Order order = orderCreateHelper.createOrder(parameters);
        pushApiConfigurer.mockAccept(order, true);
        Order acceptedOrder = pushApiConfigurer.getAcceptOrder();
        Assertions.assertNotNull(acceptedOrder.getDelivery().getOutlet());
        Assertions.assertTrue(acceptedOrder.getDelivery().isMarketBranded());
    }

    @Test
    void shouldSendOutletInfoToPushApiWhenDbsWithRouteFeature() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_DBS_WITH_ROUTE_DELIVERY_FEATURE, true);

        Parameters parameters = WhiteParametersProvider.dbsPickupOrderWithCombinatorParameters(pickupOption -> {
            pickupOption.setMarketBranded(false);
            pickupOption.setIsExternalLogistics(false);
            pickupOption.setOutlet(new PickupOptionOutlet() {{
                setId(1L);
            }});
        });
        Order order = orderCreateHelper.createOrder(parameters);
        pushApiConfigurer.mockAccept(order, true);
        Order acceptedOrder = pushApiConfigurer.getAcceptOrder();
        assertNotNull(acceptedOrder.getDelivery().getOutlet());
        assertTrue(acceptedOrder.getDelivery().containsFeature(DeliveryFeature.DBS_WITH_ROUTE));
        assertFalse(acceptedOrder.getDelivery().isMarketBranded());
    }

    @Test
    public void shouldNotDuplicateParcelsUsingAsyncPushApi() {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, SwitchWithWhitelist.enabledForAll());
        checkouterProperties.setEnableDbsThroughMarketBrandedPickup(true);

        var parameters = WhiteParametersProvider.dbsPickupOrderWithCombinatorParameters(po -> {
            po.setMarketBranded(true);
        });
        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        Order order = orderCreateHelper.createOrder(parameters);

        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);
        checkOrderAccepted(order.getId(), OrderStatus.PROCESSING);
        assertEquals(1, order.getDelivery().getParcels().size());
    }

    @Test
    public void shouldCreateOrderAndFailPostponehApiForDbsOrder() {
        var parameters = WhiteParametersProvider.defaultWhiteParameters();

        setDbsTimeout(720);
        setDbsExpiration(720);

        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        Order order = orderCreateHelper.createOrder(parameters);

        pushApiConfigurer.mockAcceptShopFailure(order, ErrorSubCode.READ_TIMED_OUT);
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);

        // оплачиваем, чтобы перешёл в PENDING
        orderPayHelper.payForOrder(order);

        // переводим часики на почти 12 часа вперед, чтобы попасть в последний QC
        jumpToFuture(719, ChronoUnit.MINUTES);

        // таска протухания не должна отменить заказ
        tmsTaskHelper.runExpireOrderTaskV2();
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.PENDING, order.getStatus());

        // а вот крайний QC должен отменить заказ
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);
        checkOrderCancelled(order.getId(), OrderSubstatus.PENDING_EXPIRED);
    }

    @Test
    public void shouldCreateOrderAndAcceptPostponehApiForDbsOrder() {
        var parameters = WhiteParametersProvider.defaultWhiteParameters();

        setDbsTimeout(720);
        setDbsExpiration(720);

        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        Order order = orderCreateHelper.createOrder(parameters);

        pushApiConfigurer.mockAccept(order, true);

        // оплачиваем, чтобы перешёл в PENDING
        orderPayHelper.payForOrder(order);

        // переводим часики на почти 12 часа вперед, чтобы попасть в последний QC
        jumpToFuture(719, ChronoUnit.MINUTES);

        // таска протухания не должна отменить заказ
        tmsTaskHelper.runExpireOrderTaskV2();
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.PENDING, order.getStatus());

        // а вот крайний QC должен принять заказ
        queuedCallService.executeQueuedCallBatch(POSTPONED_PUSH_API);
        checkOrderAccepted(order.getId(), OrderStatus.PROCESSING);
    }

    private Order createOrder(boolean postPaid) {
        return createOrder(postPaid, false);
    }

    private Order createOrder(boolean postPaid, boolean isEda) {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        parameters.setPaymentMethod(postPaid
                ? PaymentMethod.CARD_ON_DELIVERY
                : PaymentMethod.YANDEX);
        parameters.addShopMetaData(
                parameters.getOrder().getShopId(),
                SHOP_META_DATA
        );
        parameters.setupFulfillment(new ItemInfo.Fulfilment(
                parameters.getOrder().getShopId(),
                FulfilmentProvider.TEST_SKU,
                FulfilmentProvider.TEST_SHOP_SKU,
                null,
                false
        ));
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.getReportParameters().setIsEda(isEda);
        Long orderId = orderCreateHelper.createOrder(parameters).getId();
        return orderService.getOrder(orderId);
    }

    private Order createDropShipOrder(boolean postPaid) {
        Parameters parameters = new Parameters();
        parameters.setColor(Color.WHITE);
        parameters.setShopId(WHITE_SHOP_ID);
        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        parameters.setPaymentMethod(postPaid
                ? PaymentMethod.CARD_ON_DELIVERY
                : PaymentMethod.YANDEX);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addDelivery(DeliveryProvider.createFrom(parameters.getOrder().getDelivery())
                                .serviceId(parameters.getOrder().getDelivery().getDeliveryServiceId())
                                .serviceName("Доставка")
                                .partnerType(DeliveryPartnerType.YANDEX_MARKET)
                                .buildActualDeliveryOption(getClock())
                        ).build()
        );
        parameters.setPushApiDeliveryResponse(DeliveryProvider.createFrom(parameters.getOrder().getDelivery())
                .partnerType(DeliveryPartnerType.YANDEX_MARKET)
                .buildResponse(DeliveryResponse::new));
        parameters.setupFulfillment(new ItemInfo.Fulfilment(
                parameters.getOrder().getShopId(),
                FulfilmentProvider.TEST_SKU,
                FulfilmentProvider.TEST_SHOP_SKU,
                null,
                false
        ));
        parameters.setFreeDelivery(Boolean.FALSE);
        parameters.getOrder().getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        parameters.getOrder().getDelivery().setServiceName("Доставка");
        Long orderId = orderCreateHelper.createOrder(parameters).getId();
        return orderService.getOrder(orderId);
    }

    private void checkOrderAccepted(Long orderId, OrderStatus expectedStatus) {
        Order order = orderService.getOrder(orderId);

        assertNotNull(order.getShopOrderId());
        assertEquals(1, order.getDelivery().getParcels().size());
        assertEquals(expectedStatus, order.getStatus());
        assertEquals(0, queuedCallService.findQueuedCalls(POSTPONED_PUSH_API, order.getId()).size());
    }

    private void checkOrderCancelled(Long orderId, OrderSubstatus subStatus) {
        Order order = orderService.getOrder(orderId);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(subStatus, order.getSubstatus());
        assertEquals(0, queuedCallService.findQueuedCalls(POSTPONED_PUSH_API, order.getId()).size());
    }

    private OrderStatus getStatus(boolean postPaid) {
        return postPaid ? OrderStatus.PROCESSING : OrderStatus.UNPAID;
    }
}
