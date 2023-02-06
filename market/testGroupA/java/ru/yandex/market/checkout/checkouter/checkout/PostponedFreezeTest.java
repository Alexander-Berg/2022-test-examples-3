package ru.yandex.market.checkout.checkouter.checkout;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.type.permanent.PermanentComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.stock.PostponedFreezeMode;
import ru.yandex.market.checkout.checkouter.storage.OrderWritingDao;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.configuration.MockConfiguration;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.stock.StockStorageConfigurer;
import ru.yandex.market.queuedcalls.QueuedCall;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PostponedFreezeTest extends AbstractWebTestBase {

    @Autowired
    private StockStorageConfigurer stockStorageConfigurer;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private OrderWritingDao orderWritingDao;

    @Test
    public void shouldFreezeStocksWhenStockStorageIsAvailableAndPostponedFreezeModeIsOff() {
        Order order = createOrder(PostponedFreezeMode.OFF, MockConfiguration.StockStorageMockType.OK);

        assertEquals(OrderStatus.UNPAID, order.getStatus());
        assertTrue(order.getItems().stream().noneMatch(it -> it.getFitFreezed() == 0));
        assertNull(order.getProperty(OrderPropertyType.POSTPONED_STOCK_FREEZE));
    }

    @Test
    public void shouldNotFreezeStocksWhenStockStorageIsAvailableAndNoStocksLeftAndPostponedFreezeModeIsOff() {
        OrderFailure orderFailure = checkoutFailedOrder(PostponedFreezeMode.OFF,
                MockConfiguration.StockStorageMockType.NO_STOCKS);

        assertEquals("Unable to freeze stock in StockStorage", orderFailure.getErrorDetails());
    }

    @Test
    public void shouldNotFreezeStocksWhenStockStorageIsResponsesWithErrorAndPostponedFreezeModeIsOff() {
        OrderFailure orderFailure = checkoutFailedOrder(PostponedFreezeMode.OFF,
                MockConfiguration.StockStorageMockType.ERROR);

        assertEquals("Unable to freeze stock in StockStorage", orderFailure.getErrorDetails());
    }

    @Test
    public void shouldNotFreezeStocksWhenStockStorageIsUnavailableAndPostponedFreezeModeIsOff() {
        OrderFailure orderFailure = checkoutFailedOrder(PostponedFreezeMode.OFF,
                MockConfiguration.StockStorageMockType.REQUEST_TIMEOUT);

        assertEquals("Unable to freeze stock in StockStorage", orderFailure.getErrorDetails());
    }

    @Test
    public void shouldFreezeStocksWhenStockStorageIsAvailableAndPostponedFreezeModeIsOnSsFailure() {
        Order order = createOrder(PostponedFreezeMode.ON_SS_FAILURE, MockConfiguration.StockStorageMockType.OK);

        assertEquals(OrderStatus.UNPAID, order.getStatus());
        assertTrue(order.getItems().stream().noneMatch(it -> it.getFitFreezed() == 0));
        assertNull(order.getProperty(OrderPropertyType.POSTPONED_STOCK_FREEZE));
    }

    @Test
    public void shouldNotFreezeStocksWhenStockStorageIsAvailableAndNoStocksLeftAndPostponedFreezeModeIsOnSsFailure() {
        OrderFailure orderFailure = checkoutFailedOrder(PostponedFreezeMode.ON_SS_FAILURE,
                MockConfiguration.StockStorageMockType.NO_STOCKS);

        assertEquals("Unable to freeze stock in StockStorage", orderFailure.getErrorDetails());
    }

    @Test
    public void shouldFreezeStocksWhenStockStorageIsResponsesWithErrorAndPostponedFreezeModeIsOnSsFailure() {
        Order order = createOrder(PostponedFreezeMode.ON_SS_FAILURE, MockConfiguration.StockStorageMockType.ERROR);

        assertEquals(OrderStatus.UNPAID, order.getStatus());
        assertTrue(order.getItems().stream().allMatch(it -> it.getFitFreezed() == 0));
        assertEquals(Boolean.TRUE, order.getProperty(OrderPropertyType.POSTPONED_STOCK_FREEZE));
    }

    @Test
    public void shouldFreezeStocksWhenStockStorageIsUnavailableAndPostponedFreezeModeIsOnSsFailure() {
        Order order = createOrder(PostponedFreezeMode.ON_SS_FAILURE,
                MockConfiguration.StockStorageMockType.REQUEST_TIMEOUT);

        assertEquals(OrderStatus.UNPAID, order.getStatus());
        assertTrue(order.getItems().stream().allMatch(it -> it.getFitFreezed() == 0));
        assertEquals(Boolean.TRUE, order.getProperty(OrderPropertyType.POSTPONED_STOCK_FREEZE));
    }

    @Test
    public void shouldFreezeStocksWhenStockStorageIsAvailableAndPostponedFreezeModeIsAlwaysOn() {
        Order order = createOrder(PostponedFreezeMode.ALWAYS_ON, MockConfiguration.StockStorageMockType.OK);

        assertEquals(OrderStatus.UNPAID, order.getStatus());
        assertTrue(order.getItems().stream().allMatch(it -> it.getFitFreezed() == 0));
        assertEquals(Boolean.TRUE, order.getProperty(OrderPropertyType.POSTPONED_STOCK_FREEZE));
        assertThat(stockStorageConfigurer.getServeEvents(), empty());
    }

    @Test
    public void shouldFreezeStocksWhenStockStorageIsAvailableAndNoStocksLeftAndPostponedFreezeModeIsAlwaysOn() {
        Order order = createOrder(PostponedFreezeMode.ALWAYS_ON, MockConfiguration.StockStorageMockType.NO_STOCKS);

        assertEquals(OrderStatus.UNPAID, order.getStatus());
        assertTrue(order.getItems().stream().allMatch(it -> it.getFitFreezed() == 0));
        assertEquals(Boolean.TRUE, order.getProperty(OrderPropertyType.POSTPONED_STOCK_FREEZE));
        assertThat(stockStorageConfigurer.getServeEvents(), empty());
    }

    @Test
    public void shouldFreezeStocksWhenStockStorageIsResponsesWithErrorAndPostponedFreezeModeIsAlwaysOn() {
        Order order = createOrder(PostponedFreezeMode.ALWAYS_ON, MockConfiguration.StockStorageMockType.ERROR);

        assertEquals(OrderStatus.UNPAID, order.getStatus());
        assertTrue(order.getItems().stream().allMatch(it -> it.getFitFreezed() == 0));
        assertEquals(Boolean.TRUE, order.getProperty(OrderPropertyType.POSTPONED_STOCK_FREEZE));
        assertThat(stockStorageConfigurer.getServeEvents(), empty());
    }

    @Test
    public void shouldFreezeStocksWhenStockStorageIsUnavailableWithErrorAndPostponedFreezeModeIsAlwaysOn() {
        Order order = createOrder(PostponedFreezeMode.ALWAYS_ON,
                MockConfiguration.StockStorageMockType.REQUEST_TIMEOUT);

        assertEquals(OrderStatus.UNPAID, order.getStatus());
        assertTrue(order.getItems().stream().allMatch(it -> it.getFitFreezed() == 0));
        assertEquals(Boolean.TRUE, order.getProperty(OrderPropertyType.POSTPONED_STOCK_FREEZE));
        assertThat(stockStorageConfigurer.getServeEvents(), empty());
    }

    @Test
    public void shouldReturnErrorOnCartWhenSsIsUnavailableAndPostponedFreezeModeIsOff() {
        checkouterFeatureWriter.writeValue(PermanentComplexFeatureType.POSTPONED_FREEZE_MODE, PostponedFreezeMode.OFF);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setCheckCartErrors(false);
        parameters.setShouldMockStockStorageGetAmountResponse(false);
        stockStorageConfigurer.mockErrorForGetAvailableCount();

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        OrderFailure orderFailure = Iterables.getOnlyElement(multiCart.getCartFailures());

        assertEquals("Unable to actualize items count in StockStorage", orderFailure.getErrorDetails());
    }

    @Test
    public void shouldDelayQcWhenPostponedFreezeModeIsAlwaysOn() {
        Order order = createOrder(PostponedFreezeMode.ALWAYS_ON, MockConfiguration.StockStorageMockType.OK);

        assertEquals(OrderStatus.UNPAID, order.getStatus());
        assertTrue(order.getItems().stream().allMatch(it -> it.getFitFreezed() == 0));
        assertEquals(Boolean.TRUE, order.getProperty(OrderPropertyType.POSTPONED_STOCK_FREEZE));
        assertThat(stockStorageConfigurer.getServeEvents(), empty());

        setFixedTime(getClock().instant().plus(1, ChronoUnit.MINUTES));
        stockStorageConfigurer.mockOkForFreeze();
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.POSTPONED_STOCK_FREEZE);
        order = orderService.getOrder(order.getId());
        QueuedCall qc = queuedCallService.findQueuedCalls(CheckouterQCType.POSTPONED_STOCK_FREEZE, order.getId())
                .iterator().next();
        assertEquals(OrderStatus.UNPAID, order.getStatus());
        assertThat(stockStorageConfigurer.getServeEvents(), empty());
        assertNotNull(qc.getNextTryAt());
    }

    // Тесты полного сценария создания, оплаты и фриза в случае деградации stock-storage

    @Test
    public void shouldCreateOrderAndPostponeFreezeStocksForUnpaidOrderWhenStocksAvailable() {
        Order order = createOrder(PostponedFreezeMode.ON_SS_FAILURE, MockConfiguration.StockStorageMockType.ERROR);

        stockStorageConfigurer.mockErrorForFreeze();
        setFixedTime(getClock().instant().plus(1, ChronoUnit.MINUTES));
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.POSTPONED_STOCK_FREEZE);
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.UNPAID, order.getStatus());
        QueuedCall qc = queuedCallService.findQueuedCalls(CheckouterQCType.POSTPONED_STOCK_FREEZE, order.getId())
                .iterator().next();
        assertNotNull(qc.getLastTryErrorMessage());
        assertNotNull(qc.getNextTryAt());

        setFixedTime(getClock().instant().plus(1, ChronoUnit.MINUTES));
        stockStorageConfigurer.mockOkForFreeze();
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.POSTPONED_STOCK_FREEZE);
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.UNPAID, order.getStatus());

        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
        assertTrue(order.getItems().stream().noneMatch(it -> it.getFitFreezed() == 0));
        assertEquals(OrderStatus.PROCESSING, order.getStatus());
    }

    @Test
    public void shouldCreateOrderAndPostponeFreezeStocksForPendingOrderWhenStocksAvailable() {
        Order order = createOrder(PostponedFreezeMode.ON_SS_FAILURE, MockConfiguration.StockStorageMockType.ERROR);

        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(OrderSubstatus.WAITING_FOR_STOCKS, order.getSubstatus());

        stockStorageConfigurer.mockErrorForFreeze();
        setFixedTime(getClock().instant().plus(1, ChronoUnit.MINUTES));
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.POSTPONED_STOCK_FREEZE);
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(OrderSubstatus.WAITING_FOR_STOCKS, order.getSubstatus());
        QueuedCall qc = queuedCallService.findQueuedCalls(CheckouterQCType.POSTPONED_STOCK_FREEZE, order.getId())
                .iterator().next();
        assertNotNull(qc.getLastTryErrorMessage());
        assertNotNull(qc.getNextTryAt());

        setFixedTime(getClock().instant().plus(1, ChronoUnit.MINUTES));
        stockStorageConfigurer.mockOkForFreeze();
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.POSTPONED_STOCK_FREEZE);
        order = orderService.getOrder(order.getId());
        assertTrue(order.getItems().stream().noneMatch(it -> it.getFitFreezed() == 0));
        assertEquals(OrderStatus.PROCESSING, order.getStatus());
    }

    @Test
    public void shouldCreateOrderAndFailPostponeFreezeStocksForUnpaidOrderWhenNoStocksLeft() {
        Order order = createOrder(PostponedFreezeMode.ON_SS_FAILURE, MockConfiguration.StockStorageMockType.ERROR);

        stockStorageConfigurer.mockNoStocksForFreeze();
        setFixedTime(getClock().instant().plus(1, ChronoUnit.MINUTES));
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.POSTPONED_STOCK_FREEZE);
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(OrderSubstatus.MISSING_ITEM, order.getSubstatus());
    }

    @Test
    public void shouldCreateOrderAndFailPostponeFreezeStocksForPendingOrderWhenNoStocksLeft() {
        Order order = createOrder(PostponedFreezeMode.ON_SS_FAILURE, MockConfiguration.StockStorageMockType.ERROR);

        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(OrderSubstatus.WAITING_FOR_STOCKS, order.getSubstatus());

        stockStorageConfigurer.mockNoStocksForFreeze();
        setFixedTime(getClock().instant().plus(1, ChronoUnit.MINUTES));
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.POSTPONED_STOCK_FREEZE);
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(OrderSubstatus.MISSING_ITEM, order.getSubstatus());
    }

    @Test
    public void shouldCreateOrderAndDelayPostponeFreezeExecutionForPlacingOrder() {
        long orderId = orderCreateService.createOrder(OrderProvider.getBlueOrder(), ClientInfo.SYSTEM);
        Order order = orderService.getOrder(orderId);
        assertEquals(OrderStatus.PLACING, order.getStatus());

        List<OrderItem> itemsToUpdateFitFreezed = order.getItems().stream()
                .filter(item -> item.getId() != null)
                .peek(item -> item.setFitFreezed(0))
                .collect(Collectors.toList());

        transactionTemplate.execute(tc -> {
            orderWritingDao.updateFitFreezed(itemsToUpdateFitFreezed);
            orderUpdateService.addOrderProperty(OrderPropertyType.POSTPONED_STOCK_FREEZE.create(orderId, true));
            queuedCallService.addQueuedCall(CheckouterQCType.POSTPONED_STOCK_FREEZE, orderId);
            return null;
        });

        setFixedTime(getClock().instant());
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.POSTPONED_STOCK_FREEZE);
        QueuedCall qc = queuedCallService.findQueuedCalls(CheckouterQCType.POSTPONED_STOCK_FREEZE, orderId)
                .iterator().next();
        assertEquals(qc.getNextTryAt(), getClock().instant().plus(1, ChronoUnit.MINUTES));
    }

    @Test
    public void shouldCreateOrderAndDelayPostponeFreezeExecutionForUnpaidOrderWhenStockStorageIsUnavailable() {
        Order order = createOrder(PostponedFreezeMode.ON_SS_FAILURE,
                MockConfiguration.StockStorageMockType.REQUEST_TIMEOUT);

        stockStorageConfigurer.mockRequestTimeoutForFreeze();
        setFixedTime(getClock().instant().plus(1, ChronoUnit.MINUTES));
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.POSTPONED_STOCK_FREEZE);
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.UNPAID, order.getStatus());
        QueuedCall qc = queuedCallService.findQueuedCalls(CheckouterQCType.POSTPONED_STOCK_FREEZE, order.getId())
                .iterator().next();
        assertNotNull(qc.getLastTryErrorMessage());
        assertNotNull(qc.getNextTryAt());

        setFixedTime(getClock().instant().plus(1, ChronoUnit.MINUTES));
        stockStorageConfigurer.mockOkForFreeze();
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.POSTPONED_STOCK_FREEZE);
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.UNPAID, order.getStatus());

        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
        assertTrue(order.getItems().stream().noneMatch(it -> it.getFitFreezed() == 0));
        assertEquals(OrderStatus.PROCESSING, order.getStatus());
    }

    @Test
    public void shouldCreateOrderAndDelayPostponeFreezeExecutionForUnpaidOrderWhenStockStorageIsUnavailable4xxFreeze() {
        Order order = createOrder(PostponedFreezeMode.ON_400_AND_FAILURE,
                MockConfiguration.StockStorageMockType.REQUEST_TIMEOUT);

        stockStorageConfigurer.mockNoStocksForFreeze();
        setFixedTime(getClock().instant().plus(1, ChronoUnit.MINUTES));
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.POSTPONED_STOCK_FREEZE);
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.UNPAID, order.getStatus());
        QueuedCall qc = queuedCallService.findQueuedCalls(CheckouterQCType.POSTPONED_STOCK_FREEZE, order.getId())
                .iterator().next();
        assertNotNull(qc.getNextTryAt());

        setFixedTime(getClock().instant().plus(1, ChronoUnit.MINUTES));
        stockStorageConfigurer.mockOkForFreeze();
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.POSTPONED_STOCK_FREEZE);
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.UNPAID, order.getStatus());

        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
        assertTrue(order.getItems().stream().noneMatch(it -> it.getFitFreezed() == 0));
        assertEquals(OrderStatus.PROCESSING, order.getStatus());
    }

    private Order createOrder(PostponedFreezeMode postponedFreezeMode,
                              MockConfiguration.StockStorageMockType stockStorageMockType) {
        checkouterFeatureWriter.writeValue(PermanentComplexFeatureType.POSTPONED_FREEZE_MODE, postponedFreezeMode);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        if (stockStorageMockType == MockConfiguration.StockStorageMockType.ERROR) {
            parameters.setShouldMockStockStorageGetAmountResponse(false);
            stockStorageConfigurer.mockErrorForGetAvailableCount();
        }
        parameters.setStockStorageMockType(stockStorageMockType);

        Long orderId = orderCreateHelper.createOrder(parameters).getId();
        return orderService.getOrder(orderId);
    }

    private OrderFailure checkoutFailedOrder(PostponedFreezeMode postponedFreezeMode,
                                             MockConfiguration.StockStorageMockType stockStorageMockType) {
        checkouterFeatureWriter.writeValue(PermanentComplexFeatureType.POSTPONED_FREEZE_MODE, postponedFreezeMode);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setStockStorageMockType(stockStorageMockType);
        parameters.setCheckOrderCreateErrors(false);

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        return Iterables.getOnlyElement(multiOrder.getOrderFailures());
    }
}
