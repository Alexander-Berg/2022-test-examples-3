package ru.yandex.market.checkout.checkouter.lavka;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.YaLavkaHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.yalavka.YaLavkaDeliveryServiceConfigurer;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.service.yalavka.YaLavkaDeliveryOptionsPolicy.CHECK;
import static ru.yandex.market.checkout.helpers.YaLavkaHelper.lavkaOption;
import static ru.yandex.market.checkout.helpers.YaLavkaHelper.normalOption;

class YaLavkaOrderCancellationTest extends AbstractWebTestBase {

    @Autowired
    private YaLavkaDeliveryServiceConfigurer yaLavkaDSConfigurer;
    @Autowired
    private QueuedCallService qcService;
    @Autowired
    private YaLavkaHelper yaLavkaHelper;

    private Order order;
    private long orderId;

    @BeforeEach
    void setUp() throws Exception {
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndGps();
        yaLavkaDSConfigurer.reset();
        checkouterFeatureWriter.writeValue(ComplexFeatureType.YA_LAVKA_DELIVERY_OPTIONS_POLICY, CHECK);
        yaLavkaDSConfigurer.configureCheckDeliveryOptionsRequest(0);
        yaLavkaDSConfigurer.configureOrderReservationRequest(HttpStatus.OK);
        Parameters parameters = yaLavkaHelper.buildParameters(true, lavkaOption(1));
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        YaLavkaHelper.assertHasNoErrors(multiOrder);
        order = CollectionUtils.expectedSingleResult(multiOrder.getOrders());
        orderId = order.getId();
        assertEquals(OrderStatus.UNPAID, order.getStatus());
    }

    @Test
    void shouldCreateQueuedCallWhenOrderCancelled() {
        order = orderStatusHelper.proceedOrderFromUnpaidToCancelled(order);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());

        assertEquals(1, qcService.findQueuedCalls(CheckouterQCType.YANDEX_LAVKA_RESERVE_CANCELLATION, orderId).size());
    }

    @Test
    void shouldCancelReserveWhenOrderCancelled() {
        order = orderStatusHelper.proceedOrderFromUnpaidToCancelled(order);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());

        yaLavkaDSConfigurer.configureReserveCancellationRequest(HttpStatus.OK);
        qcService.executeQueuedCallBatch(CheckouterQCType.YANDEX_LAVKA_RESERVE_CANCELLATION);
        yaLavkaDSConfigurer.assertHasReserveCancellationRequests();

        assertEquals(0, qcService.findQueuedCalls(CheckouterQCType.YANDEX_LAVKA_RESERVE_CANCELLATION, orderId).size());
    }

    @Test
    void shouldNotRetryReserveCancellationWhen404() {
        order = orderStatusHelper.proceedOrderFromUnpaidToCancelled(order);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());

        yaLavkaDSConfigurer.configureReserveCancellationRequest(HttpStatus.NOT_FOUND);
        qcService.executeQueuedCallBatch(CheckouterQCType.YANDEX_LAVKA_RESERVE_CANCELLATION);
        yaLavkaDSConfigurer.assertHasReserveCancellationRequests();

        assertEquals(0, qcService.findQueuedCalls(CheckouterQCType.YANDEX_LAVKA_RESERVE_CANCELLATION, orderId).size());
    }

    @Test
    void shouldRetryReserveCancellationWhen500() {
        Instant now = Instant.now();
        setFixedTime(now);
        order = orderStatusHelper.proceedOrderFromUnpaidToCancelled(order);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());

        yaLavkaDSConfigurer.configureReserveCancellationRequest(HttpStatus.INTERNAL_SERVER_ERROR);
        qcService.executeQueuedCallBatch(CheckouterQCType.YANDEX_LAVKA_RESERVE_CANCELLATION);
        yaLavkaDSConfigurer.assertHasReserveCancellationRequests();

        assertEquals(1, qcService.findQueuedCalls(CheckouterQCType.YANDEX_LAVKA_RESERVE_CANCELLATION, orderId).size());

        setFixedTime(now.plusSeconds(3600));
        yaLavkaDSConfigurer.reset();

        yaLavkaDSConfigurer.configureReserveCancellationRequest(HttpStatus.OK);
        qcService.executeQueuedCallBatch(CheckouterQCType.YANDEX_LAVKA_RESERVE_CANCELLATION);
        yaLavkaDSConfigurer.assertHasReserveCancellationRequests();

        assertEquals(0, qcService.findQueuedCalls(CheckouterQCType.YANDEX_LAVKA_RESERVE_CANCELLATION, orderId).size());
    }

    @Test
    void shouldNotCancelReserveIfOrderInDelivery() {
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        assertEquals(OrderStatus.DELIVERY, order.getStatus());
        order = orderStatusHelper.proceedOrderFromUnpaidToCancelled(order);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());

        assertEquals(0, qcService.findQueuedCalls(CheckouterQCType.YANDEX_LAVKA_RESERVE_CANCELLATION, orderId).size());
        yaLavkaDSConfigurer.assertNoReserveCancellationRequests();
    }

    @Test
    void shouldNotCancelReserveForNonLavkaOrders() {
        Parameters parameters = yaLavkaHelper.buildParameters(false, normalOption(1));
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        YaLavkaHelper.assertHasNoErrors(multiOrder);
        Order normalOrder = CollectionUtils.expectedSingleResult(multiOrder.getOrders());

        normalOrder = orderStatusHelper.proceedOrderFromUnpaidToCancelled(normalOrder);
        assertEquals(OrderStatus.CANCELLED, normalOrder.getStatus());

        assertEquals(0,
                qcService.findQueuedCalls(CheckouterQCType.YANDEX_LAVKA_RESERVE_CANCELLATION, order.getId()).size());
        yaLavkaDSConfigurer.assertNoReserveCancellationRequests();
    }
}
