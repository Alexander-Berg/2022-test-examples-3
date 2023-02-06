package ru.yandex.market.pvz.core.domain.order;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.ItemParameter;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.UnitValue;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.OfferPicture;
import ru.yandex.market.logistic.api.model.delivery.CargoType;
import ru.yandex.market.pvz.client.model.order.DeliveryServiceType;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.order.model.OrderType;
import ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus;
import ru.yandex.market.pvz.core.domain.order.model.cashbox.CashboxPaymentStatus;
import ru.yandex.market.pvz.core.domain.order.model.cashbox.OrderCashboxTransaction;
import ru.yandex.market.pvz.core.domain.order.model.cashbox.OrderCashboxTransactionParams;
import ru.yandex.market.pvz.core.domain.order.model.history.OrderHistoryRecord;
import ru.yandex.market.pvz.core.domain.order.model.history.OrderHistoryRepository;
import ru.yandex.market.pvz.core.domain.order.model.params.OrderParams;
import ru.yandex.market.pvz.core.domain.order.model.params.OrderSimpleParams;
import ru.yandex.market.pvz.core.domain.order.model.params.OrderUpdateParams;
import ru.yandex.market.pvz.core.domain.order.model.personal.OrderPersonal;
import ru.yandex.market.pvz.core.domain.order.model.personal.OrderPersonalRepository;
import ru.yandex.market.pvz.core.domain.order.model.sibling.SiblingGroupParams;
import ru.yandex.market.pvz.core.domain.order.model.sibling.SiblingOrderQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointCapacityManager;
import ru.yandex.market.pvz.core.domain.pickup_point.calendar.override.PickupPointCalendarOverrideParams;
import ru.yandex.market.pvz.core.domain.pickup_point.schedule.model.PickupPointScheduleDay;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.configuration.ConfigurationProvider;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.exception.TplForbiddenException;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.common.util.logging.Tracer;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.pvz.core.config.PvzCoreInternalConfiguration.TRACER_LOGIN;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.DETERMINE_IS_FASHION_AFTER_SHIPMENT;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.EXTEND_STORAGE_PERIOD_MAX_DAYS;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.NEW_CAPACITY_CALCULATION_ENABLED;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.ORDER_VERIFICATION_CODE_LIMIT;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.VERIFICATION_CODE_CLIENT_ENABLED;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.VERIFICATION_CODE_ON_DEMAND_ENABLED;
import static ru.yandex.market.pvz.core.domain.order.OrderCommandService.DEFAULT_CHANGE_OUTBOUND_VERIFICATION_CODE_REQUESTED_DELAY;
import static ru.yandex.market.pvz.core.domain.order.OrderCommandService.DEFAULT_ON_DEMAND_STORAGE_PERIOD;
import static ru.yandex.market.pvz.core.domain.order.OrderCommandService.ITEM_PARAMETER_COLOR;
import static ru.yandex.market.pvz.core.domain.order.OrderCommandService.ITEM_PARAMETER_SIZE;
import static ru.yandex.market.pvz.core.domain.order.OrderCommandService.PVZ_INCONSISTENT_ORDER_PAYMENT_TYPE;
import static ru.yandex.market.pvz.core.domain.order.OrderCommandService.SIMPLIFIED_DELIVERY_PERIOD;
import static ru.yandex.market.pvz.core.domain.order.model.OrderPaymentStatus.PAID;
import static ru.yandex.market.pvz.core.domain.order.model.OrderPaymentStatus.UNPAID;
import static ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType.CARD;
import static ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType.CASH;
import static ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType.UNKNOWN;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.ARRIVED_TO_PICKUP_POINT;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.CANCELLED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.CHANGE_OUTBOUND_VERIFICATION_CODE_REQUESTED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.CREATED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.DELIVERED_TO_RECIPIENT;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.DELIVERY_DATE_UPDATED_BY_DELIVERY;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.READY_FOR_RETURN;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.RETURNED_ORDER_WAS_DISPATCHED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.STORAGE_PERIOD_EXPIRED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.STORAGE_PERIOD_EXTENDED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.TRANSMITTED_TO_RECIPIENT;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.TRANSPORTATION_RECIPIENT;
import static ru.yandex.market.pvz.core.domain.order.model.cashbox.CashboxPaymentStatus.SUCCESS;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.CIS_1_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.CIS_2_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_RECIPIENT_EMAIL;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_RECIPIENT_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_RECIPIENT_PHONE;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_VERIFICATION_CODE;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.R18_ITEM;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.UIT_1_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.UIT_2_1;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_STORAGE_PERIOD;

@Log4j2
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderCommandServiceTest {

    private static final String OLD_NAME = "Name";
    private static final String OLD_PHONE = "+79112223344";
    private static final String OLD_EMAIL = "email@yandex.ru";
    private static final String NEW_NAME = "Name 2.0";
    private static final String LOGIN = "dora_the_explorer";

    private final TestableClock clock;
    private final TransactionTemplate transactionTemplate;
    private final OrderHistoryRepository orderHistoryRepository;
    private final OrderRepository orderRepository;
    private final OrderQueryService orderQueryService;
    private final OrderAdditionalInfoRepository orderAdditionalInfoRepository;
    private final OrderPersonalRepository orderPersonalRepository;

    private final SiblingOrderQueryService siblingOrderQueryService;
    private final OrderCashboxTransactionRepository transactionRepository;

    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;

    private final OrderCommandService orderCommandService;
    private final OrderExpirationDateService expirationDateService;
    private final OrderParamsMapper orderParamsMapper;

    private final PickupPointCapacityManager capacityManager;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;
    private final ConfigurationProvider configurationProvider;

    @MockBean
    private ComplexMonitoring monitoring;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.now(), ZoneId.systemDefault());
        configurationGlobalCommandService.setValue(EXTEND_STORAGE_PERIOD_MAX_DAYS, 7);
        configurationGlobalCommandService.setValue(VERIFICATION_CODE_CLIENT_ENABLED, true);
        configurationGlobalCommandService.setValue(VERIFICATION_CODE_ON_DEMAND_ENABLED, true);
        configurationGlobalCommandService.setValue(NEW_CAPACITY_CALCULATION_ENABLED, true);
        configurationGlobalCommandService.setValue(DETERMINE_IS_FASHION_AFTER_SHIPMENT, true);
    }

    @ParameterizedTest
    @CsvSource(
            {
                    " , , true, AWAITING_CASHBOX_STATUS",
                    "true, false, true, AWAITING_CASHBOX_STATUS",
                    "false, true, false, PROCESSED_THROUGH_UI",
                    " , false, false, PROCESSED_THROUGH_UI",
                    " , true, true, AWAITING_CASHBOX_STATUS"
            }
    )
    void updateCashboxTransaction(
            Boolean apiConfiguration, Boolean apiAnswer, boolean apiConfigurationExpected,
            CashboxPaymentStatus paymentStatusExpected
    ) {
        var pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                .params(TestPickupPointFactory.PickupPointTestParams.builder()
                        .cashboxApiSupportAnswer(apiConfiguration)
                        .build())
                .build());
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());
        orderFactory.receiveOrder(order.getId());
        var cashboxTransaction = OrderCashboxTransaction.builder()
                .orderId(order.getId())
                .callbackToken("12321321")
                .build();

        var createdTransaction = orderCommandService.createCashboxTransaction(cashboxTransaction);
        var cashboxParamsToUpdate = OrderCashboxTransactionParams.builder()
                .deliveryType(OrderDeliveryType.PAYMENT)
                .transactionId("123LERATOP")
                .serviceLink("yadnex.ru")
                .callbackToken(createdTransaction.getCallbackToken())
                .apiSupportAnswer(apiAnswer)
                .build();
        var updatedTransaction = orderCommandService.updateCashboxTransaction(
                cashboxParamsToUpdate, order.getId(), order.getIdOfPickupPoint(),
                createdTransaction.getCallbackToken());
        createdTransaction.setApiSupportAnswer(apiConfigurationExpected);
        createdTransaction.setServiceLink(updatedTransaction.getServiceLink());
        createdTransaction.setDeliveryType(updatedTransaction.getDeliveryType());
        createdTransaction.setTransactionId(updatedTransaction.getTransactionId());
        createdTransaction.setPaymentStatus(paymentStatusExpected);

        assertThat(updatedTransaction).isEqualTo(createdTransaction);
    }

    @Transactional
    @Test
    void testOrderHistoryIsRecorded() {
        Instant creationTime = Instant.ofEpochSecond(60);
        Instant cancellationTime = Instant.ofEpochSecond(120);
        clock.setFixed(creationTime, clock.getZone());

        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());

        clock.setFixed(cancellationTime, clock.getZone());

        order.setStatusAndCheckpoint(CANCELLED);
        order = orderFactory.updateOrder(order);

        List<OrderHistoryRecord> historyRecords = orderHistoryRepository.getOrderHistory(order.getId());
        assertThat(historyRecords).hasSize(2);
        checkHistoryRecord(historyRecords.get(0), order, CREATED, CREATED.getCode(), creationTime);
        checkHistoryRecord(historyRecords.get(1), order, CANCELLED, CANCELLED.getCode(), cancellationTime);
    }

    private void checkHistoryRecord(OrderHistoryRecord record, Order order, PvzOrderStatus status,
                                    int dsApiCheckPoint, Instant updatedAt) {
        assertThat(record.getOrderId()).isEqualTo(order.getId());
        assertThat(record.getExternalId()).isEqualTo(order.getExternalId());
        assertThat(record.getStatus()).isEqualTo(status);
        assertThat(record.getDsApiCheckpoint()).isEqualTo(dsApiCheckPoint);
        assertThat(record.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void receiveShipment() {
        ZoneOffset zone = ZoneOffset.ofHours(PickupPoint.DEFAULT_TIME_OFFSET);
        LocalDateTime paymentDate = LocalDateTime.of(2020, 9, 16, 16, 0, 0);
        clock.setFixed(paymentDate.toInstant(zone), zone);

        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .deliveryDate(paymentDate.plusDays(1).toLocalDate())
                        .build())
                .build());
        orderFactory.receiveOrder(order.getId());

        Order received = orderRepository.findByIdOrThrow(order.getId());

        assertThat(received.getStatus()).isEqualTo(ARRIVED_TO_PICKUP_POINT);
        assertThat(received.getArrivedAt()).isEqualTo(OffsetDateTime.ofInstant(clock.instant(), zone));
        assertThat(received.getExpirationDate())
                .isEqualTo(paymentDate.toLocalDate().plusDays(DEFAULT_STORAGE_PERIOD));

        assertThat(
                orderHistoryRepository.getOrderHistory(order.getId()).stream()
                        .map(OrderHistoryRecord::getStatus)
                        .collect(Collectors.toList()))
                .containsExactly(CREATED, ARRIVED_TO_PICKUP_POINT);
    }

    @Test
    void receiveShipmentWithOnDemandOrder() {
        ZoneOffset zone = ZoneOffset.ofHours(PickupPoint.DEFAULT_TIME_OFFSET);
        LocalDateTime paymentDate = LocalDateTime.of(2020, 9, 16, 16, 0, 0);
        clock.setFixed(paymentDate.toInstant(zone), zone);

        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .type(OrderType.ON_DEMAND)
                        .deliveryDate(paymentDate.plusDays(1).toLocalDate())
                        .build())
                .build());
        orderFactory.receiveOrder(order.getId());

        Order received = orderRepository.findByIdOrThrow(order.getId());

        assertThat(received.getStatus()).isEqualTo(ARRIVED_TO_PICKUP_POINT);
        assertThat(received.getArrivedAt()).isEqualTo(OffsetDateTime.ofInstant(clock.instant(), zone));
        assertThat(received.getExpirationDate())
                .isEqualTo(paymentDate.toLocalDate().plusDays(DEFAULT_ON_DEMAND_STORAGE_PERIOD.toDays()));

        assertThat(
                orderHistoryRepository.getOrderHistory(order.getId()).stream()
                        .map(OrderHistoryRecord::getStatus)
                        .collect(Collectors.toList()))
                .containsExactly(CREATED, ARRIVED_TO_PICKUP_POINT);
    }

    @Test
    void receiveShipmentWithPastDeliveryDate() {
        ZoneOffset zone = ZoneOffset.ofHours(PickupPoint.DEFAULT_TIME_OFFSET);
        LocalDateTime paymentDate = LocalDateTime.of(2020, 9, 16, 16, 0, 0);
        clock.setFixed(paymentDate.toInstant(zone), zone);

        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .deliveryDate(paymentDate.minusDays(1).toLocalDate())
                        .build())
                .build());
        orderFactory.receiveOrder(order.getId());

        Order received = orderRepository.findByIdOrThrow(order.getId());

        assertThat(received.getStatus()).isEqualTo(ARRIVED_TO_PICKUP_POINT);
        assertThat(received.getArrivedAt()).isEqualTo(OffsetDateTime.ofInstant(clock.instant(), zone));
        assertThat(received.getExpirationDate())
                .isEqualTo(paymentDate.toLocalDate().plusDays(DEFAULT_STORAGE_PERIOD));

        assertThat(
                orderHistoryRepository.getOrderHistory(order.getId()).stream()
                        .map(OrderHistoryRecord::getStatus)
                        .collect(Collectors.toList()))
                .containsExactly(CREATED, ARRIVED_TO_PICKUP_POINT);
    }

    @Transactional
    @Test
    void receiveShipmentAlreadyShippedOrder() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());
        orderFactory.receiveOrder(order.getId());

        assertThatThrownBy(() -> orderFactory.receiveOrder(order.getId()))
                .isExactlyInstanceOf(TplInvalidActionException.class);
    }

    @Test
    void revertShipment() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());
        order = orderFactory.receiveOrder(order.getId());

        orderCommandService.revertShipment(order.getExternalId(), order.getIdOfPickupPoint());

        Order received = orderRepository.findByIdOrThrow(order.getId());
        assertThat(received.getStatus()).isEqualTo(CREATED);

        order = orderFactory.receiveOrder(order.getId());
        received = orderRepository.findByIdOrThrow(order.getId());
        assertThat(received.getStatus()).isEqualTo(ARRIVED_TO_PICKUP_POINT);

        assertThat(
                orderHistoryRepository.getOrderHistory(order.getId()).stream()
                        .map(OrderHistoryRecord::getStatus)
                        .collect(Collectors.toList()))
                .containsExactly(CREATED, ARRIVED_TO_PICKUP_POINT, CREATED, ARRIVED_TO_PICKUP_POINT);
    }

    @Transactional
    @Test
    void unableToRevertShipmentForCancelledOrder() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());
        String externalOrderId = order.getExternalId();
        long pickupPointId = order.getIdOfPickupPoint();
        order = orderFactory.receiveOrder(order.getId());
        OrderUpdateParams orderUpdateParams = orderParamsMapper.mapUpdate(order, configurationProvider);
        orderCommandService.cancel(orderUpdateParams);

        assertThat(
                orderHistoryRepository.getOrderHistory(order.getId()).stream()
                        .map(OrderHistoryRecord::getStatus)
                        .collect(Collectors.toList()))
                .containsExactly(CREATED, ARRIVED_TO_PICKUP_POINT, READY_FOR_RETURN);

        assertThatThrownBy(() -> orderCommandService.revertShipment(externalOrderId, pickupPointId))
                .isExactlyInstanceOf(TplInvalidActionException.class);
    }

    @Transactional
    @Test
    void revertShipmentForExpiredOrder() {
        Instant now = Instant.now(clock);
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());
        order = orderFactory.receiveOrder(order.getId());

        clock.setFixed(now.plus(order.getPickupPoint().getStoragePeriod() + 2, ChronoUnit.DAYS), clock.getZone());
        orderCommandService.sendToReturnExpiredOrders();
        Order expired = orderRepository.findByIdOrThrow(order.getId());
        assertThat(expired.getStatus()).isEqualTo(STORAGE_PERIOD_EXPIRED);

        orderCommandService.revertShipment(order.getExternalId(), order.getIdOfPickupPoint());

        Order received = orderRepository.findByIdOrThrow(order.getId());
        assertThat(received.getStatus()).isEqualTo(CREATED);

        assertThat(
                orderHistoryRepository.getOrderHistory(order.getId()).stream()
                        .map(OrderHistoryRecord::getStatus)
                        .collect(Collectors.toList()))
                .containsExactly(CREATED, ARRIVED_TO_PICKUP_POINT, STORAGE_PERIOD_EXPIRED, CREATED);
    }

    @Transactional
    @Test
    void dispatchShipment() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());
        orderFactory.readyForReturn(order.getId());
        order = orderRepository.findByIdOrThrow(order.getId());

        orderFactory.dispatchOrder(order.getId());

        Order dispatched = orderRepository.findByIdOrThrow(order.getId());

        assertThat(dispatched.getStatus()).isEqualTo(RETURNED_ORDER_WAS_DISPATCHED);
    }

    @Transactional
    @Test
    void unableToDispatchShipment() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());
        assertThatThrownBy(() -> orderFactory.dispatchOrder(order.getId()))
                .isExactlyInstanceOf(TplInvalidActionException.class);
    }

    @Transactional
    @Test
    void extendStoragePeriodForOrder() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());

        order = orderFactory.receiveOrder(order.getId());

        LocalDate newDate = clock.instant().plus(DEFAULT_STORAGE_PERIOD + 3, ChronoUnit.DAYS)
                .atZone(ZoneId.systemDefault()).toLocalDate();

        orderCommandService.extendStoragePeriod(order, newDate);

        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getStatus()).isEqualTo(PvzOrderStatus.STORAGE_PERIOD_EXTENDED);
        assertThat(order.getExpirationDate()).isEqualTo(newDate);

        assertThat(
                orderHistoryRepository.getOrderHistory(order.getId()).stream()
                        .map(OrderHistoryRecord::getStatus)
                        .collect(Collectors.toList()))
                .containsExactly(CREATED, ARRIVED_TO_PICKUP_POINT, STORAGE_PERIOD_EXTENDED);

        var additionalInfo = order.getOrderAdditionalInfo();
        assertThat(additionalInfo.getStoragePeriodExtended()).isTrue();
    }

    @Test
    void extendStoragePeriodForOrderInInvalidStatus() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());

        LocalDate newDate = clock.instant().plus(6, ChronoUnit.DAYS)
                .atZone(ZoneId.systemDefault()).toLocalDate();

        assertThatThrownBy(() -> orderCommandService.extendStoragePeriod(order, newDate))
                .isExactlyInstanceOf(TplInvalidActionException.class);
    }

    @Test
    void extendStorageForSameDay() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());

        Order shippedOrder = orderFactory.receiveOrder(order.getId());

        LocalDate newDate = clock.instant().atZone(ZoneId.systemDefault()).toLocalDate();

        assertThatThrownBy(() -> orderCommandService.extendStoragePeriod(shippedOrder, newDate))
                .isExactlyInstanceOf(TplInvalidActionException.class);
    }

    @Test
    void extendStorageForPastDate() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());

        Order shippedOrder = orderFactory.receiveOrder(order.getId());

        LocalDate newDate = clock.instant().minus(2, ChronoUnit.DAYS)
                .atZone(ZoneId.systemDefault()).toLocalDate();

        assertThatThrownBy(() -> orderCommandService.extendStoragePeriod(shippedOrder, newDate))
                .isExactlyInstanceOf(TplInvalidActionException.class);
    }

    @Test
    void extendStorageForTooLargePeriod() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());

        Order shippedOrder = orderFactory.receiveOrder(order.getId());

        LocalDate newDate = clock.instant()
                .plus(order.getPickupPoint().getStoragePeriod()
                                + configurationProvider.getValueAsInteger(EXTEND_STORAGE_PERIOD_MAX_DAYS).orElseThrow()
                                + 2,
                        ChronoUnit.DAYS)
                .atZone(ZoneId.systemDefault()).toLocalDate();

        assertThatThrownBy(() -> orderCommandService.extendStoragePeriod(shippedOrder, newDate))
                .isExactlyInstanceOf(TplInvalidActionException.class);
    }

    @Transactional
    @Test
    void successDeliver() {
        Tracer.global().put(TRACER_LOGIN, LOGIN);
        clock.setFixed(Instant.now(), ZoneId.systemDefault());

        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());
        long orderId = order.getId();
        orderFactory.receiveOrder(orderId);
        order = orderFactory.verifyOrder(orderId);

        orderCommandService.deliver(order, OrderDeliveryType.VERIFICATION_CODE, null);

        Order transmitted = orderRepository.findByIdOrThrow(order.getId());
        assertThat(transmitted.getStatus()).isEqualTo(TRANSMITTED_TO_RECIPIENT);
        assertThat(transmitted.getDeliveredAt())
                .isEqualTo(clock.instant().atOffset(ZoneOffset.ofHours(transmitted.getPickupPoint().getTimeOffset())));

        assertThat(
                orderHistoryRepository.getOrderHistory(order.getId()).stream()
                        .map(OrderHistoryRecord::getStatus)
                        .collect(Collectors.toList()))
                .containsExactly(CREATED, ARRIVED_TO_PICKUP_POINT, TRANSMITTED_TO_RECIPIENT);
        assertEquals(orderAdditionalInfoRepository.findByOrderId(order.getId()).getDeliveredBy(), LOGIN);
    }

    @Transactional
    @Test
    void successDeliverExpired() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());

        order = orderFactory.receiveOrder(order.getId());
        order.setStatusAndCheckpoint(STORAGE_PERIOD_EXPIRED);
        orderFactory.updateOrder(order);
        order = orderFactory.verifyOrder(order.getId());
        orderCommandService.deliver(order, OrderDeliveryType.VERIFICATION_CODE, null);

        Order transmitted = orderRepository.findByIdOrThrow(order.getId());
        assertThat(transmitted.getStatus()).isEqualTo(TRANSMITTED_TO_RECIPIENT);
    }

    @Transactional
    @Test
    void successDeliverExtended() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());

        order = orderFactory.receiveOrder(order.getId());
        order.setStatusAndCheckpoint(STORAGE_PERIOD_EXTENDED);
        orderFactory.updateOrder(order);
        order = orderFactory.verifyOrder(order.getId());
        orderCommandService.deliver(order, OrderDeliveryType.VERIFICATION_CODE, null);

        Order transmitted = orderRepository.findByIdOrThrow(order.getId());
        assertThat(transmitted.getStatus()).isEqualTo(TRANSMITTED_TO_RECIPIENT);
    }

    @Test
    void invalidStatusForDeliver() {
        Order createdOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());

        assertThatThrownBy(() -> orderCommandService.deliver(createdOrder, OrderDeliveryType.VERIFICATION_CODE, null))
                .isExactlyInstanceOf(TplInvalidParameterException.class);
    }

    @Transactional
    @Test
    void unableToDeliverWithNullDeliveryType() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());
        long orderId = order.getId();
        orderFactory.receiveOrder(orderId);
        Order order1 = orderFactory.verifyOrder(orderId);

        assertThatThrownBy(() -> orderCommandService.deliver(order1, null, null))
                .isExactlyInstanceOf(TplForbiddenException.class);
    }

    @Transactional
    @Test
    void unableToDeliverWithVerificationCodeDeliveryTypeButOrderIsNotVerified() {
        clock.setFixed(Instant.now(), ZoneId.systemDefault());

        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());
        long orderId = order.getId();
        Order order1 = orderFactory.receiveOrder(orderId);

        assertThatThrownBy(() -> orderCommandService.deliver(order1, OrderDeliveryType.VERIFICATION_CODE, null))
                .isExactlyInstanceOf(TplForbiddenException.class);
    }

    @Transactional
    @Test
    void unableToDeliverWithBarcodeDeliveryTypeButOrderIsNotVerified() {
        clock.setFixed(Instant.now(), ZoneId.systemDefault());

        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());
        long orderId = order.getId();
        Order order2 = orderFactory.receiveOrder(orderId);

        assertThatThrownBy(() -> orderCommandService.deliver(order2, OrderDeliveryType.BARCODE, null))
                .isExactlyInstanceOf(TplForbiddenException.class);
    }

    @Transactional
    @Test
    void unableToDeliverPrepaidOrderWithPaymentDeliveryType() {
        clock.setFixed(Instant.now(), ZoneId.systemDefault());

        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());
        long orderId = order.getId();
        Order order1 = orderFactory.receiveOrder(orderId);

        assertThatThrownBy(() -> orderCommandService.deliver(order1, OrderDeliveryType.PAYMENT, null))
                .isExactlyInstanceOf(TplForbiddenException.class);
    }

    @Transactional
    @Test
    void successDeliverAdultOrderWithBarcodeDeliveryType() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder().items(R18_ITEM).build())
                .build());

        order = orderFactory.receiveOrder(order.getId());
        order.setStatusAndCheckpoint(STORAGE_PERIOD_EXPIRED);
        orderFactory.updateOrder(order);
        order = orderFactory.verifyOrder(order.getId());
        orderCommandService.deliver(order, OrderDeliveryType.BARCODE, null);

        Order transmitted = orderRepository.findByIdOrThrow(order.getId());
        assertThat(transmitted.getStatus()).isEqualTo(TRANSMITTED_TO_RECIPIENT);
        assertThat(transmitted.getDeliveryType()).isEqualTo(OrderDeliveryType.BARCODE);

        assertThat(
                orderHistoryRepository.getOrderHistory(order.getId()).stream()
                        .map(OrderHistoryRecord::getStatus)
                        .collect(Collectors.toList()))
                .containsExactly(CREATED, ARRIVED_TO_PICKUP_POINT, STORAGE_PERIOD_EXPIRED, TRANSMITTED_TO_RECIPIENT);
    }

    @Transactional
    @Test
    void successDeliverOrderWithSimplifiedDelivery() {
        var pickupPoint = pickupPointFactory.createPickupPoint();

        Order createdOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint).build());
        Order deliveredOrder = createAndReceiveOrder(pickupPoint);
        Order order = orderFactory.receiveOrder(createdOrder.getId());

        orderFactory.verifyOrder(deliveredOrder.getId());
        orderFactory.deliverOrder(deliveredOrder.getId(), OrderDeliveryType.VERIFICATION_CODE, null);

        transactionTemplate.execute(ts -> {
            orderCommandService.deliver(order, OrderDeliveryType.SIMPLIFIED_DELIVERY, null);
            return null;
        });
    }

    @Transactional
    @Test
    void failDeliverAdultOrderWithSimplifiedDelivery() {
        var pickupPoint = pickupPointFactory.createPickupPoint();

        Order createdOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder().items(R18_ITEM).build())
                .build());
        Order deliveredOrder = createAndReceiveOrder(pickupPoint);
        Order order = orderFactory.receiveOrder(createdOrder.getId());

        orderFactory.verifyOrder(deliveredOrder.getId());
        orderFactory.deliverOrder(deliveredOrder.getId(), OrderDeliveryType.VERIFICATION_CODE, null);

        transactionTemplate.execute(ts -> {
            assertThatThrownBy(() -> orderCommandService.deliver(order, OrderDeliveryType.SIMPLIFIED_DELIVERY, null))
                    .isExactlyInstanceOf(TplForbiddenException.class)
                    .hasMessage("Этот заказ нельзя выдать без проверки получателя");
            return null;
        });
    }

    @Transactional
    @Test
    void failDeliverOnDemandOrderWithSimplifiedDelivery() {
        var pickupPoint = pickupPointFactory.createPickupPoint();

        Order createdOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder().type(OrderType.ON_DEMAND).build())
                .build());
        Order deliveredOrder = createAndReceiveOrder(pickupPoint);
        Order order = orderFactory.receiveOrder(createdOrder.getId());

        orderFactory.verifyOrder(deliveredOrder.getId());
        orderFactory.deliverOrder(deliveredOrder.getId(), OrderDeliveryType.VERIFICATION_CODE, null);

        transactionTemplate.execute(ts -> {
            assertThatThrownBy(() -> orderCommandService.deliver(order, OrderDeliveryType.SIMPLIFIED_DELIVERY, null))
                    .isExactlyInstanceOf(TplForbiddenException.class)
                    .hasMessage("Невозможно выдать заказ, так как код не проверен");
            return null;
        });
    }

    @Transactional
    @ParameterizedTest
    @EnumSource(value = OrderPaymentType.class, names = {"CARD", "CASH"})
    void failDeliverPostPaidOrderWithSimplifiedDelivery(OrderPaymentType paymentType) {
        var pickupPoint = pickupPointFactory.createPickupPoint();

        Order createdOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder().paymentType(paymentType).build())
                .build());
        Order deliveredOrder = createAndReceiveOrder(pickupPoint);
        Order order = orderFactory.receiveOrder(createdOrder.getId());

        orderFactory.verifyOrder(deliveredOrder.getId());
        orderFactory.deliverOrder(deliveredOrder.getId(), OrderDeliveryType.VERIFICATION_CODE, null);

        transactionTemplate.execute(ts -> {
            assertThatThrownBy(() -> orderCommandService.deliver(order, OrderDeliveryType.SIMPLIFIED_DELIVERY, null))
                    .isExactlyInstanceOf(TplForbiddenException.class)
                    .hasMessage("Этот заказ нельзя выдать без проверки получателя");
            return null;
        });
    }

    @Transactional
    @Test
    void failDeliverOrderWithLongTimeAgoDeliveredSiblingWithSimplifiedDelivery() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());

        Order createdOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint).build());
        Order deliveredOrder = createAndReceiveOrder(pickupPoint);
        Order order = orderFactory.receiveOrder(createdOrder.getId());

        orderFactory.verifyOrder(deliveredOrder.getId());
        clock.setFixed(Instant.now().minus(SIMPLIFIED_DELIVERY_PERIOD).minus(1, ChronoUnit.MINUTES), zone);
        orderFactory.deliverOrder(deliveredOrder.getId(), OrderDeliveryType.VERIFICATION_CODE, null);

        clock.setFixed(Instant.now(), zone);
        transactionTemplate.execute(ts -> {
            assertThatThrownBy(() -> orderCommandService.deliver(order, OrderDeliveryType.SIMPLIFIED_DELIVERY, null))
                    .isExactlyInstanceOf(TplForbiddenException.class)
                    .hasMessage("Время упрощённой выдачи для заказа истекло. Обновите страницу");
            return null;
        });
    }

    @Transactional
    @Test
    void changePaymentTypeToCash() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(UNKNOWN)
                        .paymentStatus(UNPAID)
                        .build())
                .build());

        orderFactory.receiveOrder(order.getId());
        order = orderFactory.verifyOrder(order.getId());
        orderCommandService.deliver(order, OrderDeliveryType.VERIFICATION_CODE, OrderPaymentType.CASH);

        Order transmitted = orderRepository.findByIdOrThrow(order.getId());
        assertThat(transmitted.getPaymentType()).isEqualTo(OrderPaymentType.CASH);
        assertThat(transmitted.getPaymentStatus()).isEqualTo(PAID);

        assertThat(
                orderHistoryRepository.getOrderHistory(order.getId()).stream()
                        .map(OrderHistoryRecord::getStatus)
                        .collect(Collectors.toList()))
                .containsExactly(CREATED, ARRIVED_TO_PICKUP_POINT, TRANSMITTED_TO_RECIPIENT);
    }

    @Transactional
    @Test
    void tryToChangePaymentTypeToCashButPrepaid() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());

        orderFactory.receiveOrder(order.getId());
        order = orderFactory.verifyOrder(order.getId());
        orderCommandService.deliver(order, OrderDeliveryType.UNKNOWN, OrderPaymentType.CASH);

        Order transmitted = orderRepository.findByIdOrThrow(order.getId());
        assertTrue(transmitted.getPaymentType().isPrepaid());
        assertThat(transmitted.getPaymentStatus()).isEqualTo(PAID);
    }

    @Test
    @Transactional
    void paymentTypeIsNotProvidedForNonPrepaidOrder() {
        Order createdOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(UNKNOWN)
                        .build())
                .build());

        orderFactory.receiveOrder(createdOrder.getId());

        transactionTemplate.execute(ts -> {
            ts.setRollbackOnly();
            Order shippedOrder = orderFactory.verifyOrder(createdOrder.getId());

            assertThatThrownBy(() -> orderCommandService.deliver(shippedOrder, OrderDeliveryType.VERIFICATION_CODE,
                    null))
                    .isExactlyInstanceOf(TplInvalidParameterException.class);
            return null;
        });
    }

    @Test
    @Transactional
    void paymentTypeIsNotProvidedForCashOrder() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(CASH)
                        .paymentStatus(UNPAID)
                        .build())
                .build());

        orderFactory.receiveOrder(order.getId());
        order = orderFactory.verifyOrder(order.getId());
        orderCommandService.deliver(order, OrderDeliveryType.VERIFICATION_CODE, null);

        Order transmitted = orderRepository.findByIdOrThrow(order.getId());
        assertThat(transmitted.getPaymentType()).isEqualTo(OrderPaymentType.CASH);
        assertThat(transmitted.getPaymentStatus()).isEqualTo(PAID);
    }

    @Transactional
    @Test
    void changePaymentTypeFromCashToCard() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(CASH)
                        .paymentStatus(UNPAID)
                        .build())
                .build());

        orderFactory.receiveOrder(order.getId());
        order = orderFactory.verifyOrder(order.getId());
        orderCommandService.deliver(order, OrderDeliveryType.PAYMENT, OrderPaymentType.CARD);

        Order transmitted = orderRepository.findByIdOrThrow(order.getId());
        assertThat(transmitted.getPaymentType()).isEqualTo(OrderPaymentType.CARD);
        assertThat(transmitted.getPaymentStatus()).isEqualTo(PAID);
    }

    @Test
    @Transactional
    void tryToChangePaymentTypeToUnknownForNonPrepaidOrder() {
        Order createdOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(UNKNOWN)
                        .build())
                .build());

        orderFactory.receiveOrder(createdOrder.getId());

        transactionTemplate.execute(ts -> {
            ts.setRollbackOnly();
            Order shippedOrder = orderFactory.verifyOrder(createdOrder.getId());
            assertThatThrownBy(
                    () -> orderCommandService.deliver(shippedOrder, OrderDeliveryType.VERIFICATION_CODE, UNKNOWN)
            ).isExactlyInstanceOf(TplInvalidParameterException.class);
            return null;
        });
    }

    @Test
    void notProvidePaymentTypeForUnknownOrder() {
        Order createdOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(UNKNOWN)
                        .build())
                .build());

        orderFactory.receiveOrder(createdOrder.getId());
        orderFactory.verifyOrder(createdOrder.getId());

        assertThatThrownBy(() -> orderCommandService.deliver(createdOrder, OrderDeliveryType.VERIFICATION_CODE, null))
                .isExactlyInstanceOf(TplInvalidParameterException.class);
    }

    @Transactional
    @Test
    void successDeliverOnDemand() {
        clock.setFixed(Instant.now(), ZoneId.systemDefault());

        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .type(OrderType.ON_DEMAND)
                        .build())
                .build());
        long orderId = order.getId();
        orderFactory.receiveOrder(orderId);
        order = orderFactory.verifyOrder(orderId);
        orderCommandService.deliver(order, OrderDeliveryType.VERIFICATION_CODE, null);

        Order transmitted = orderRepository.findByIdOrThrow(order.getId());
        assertThat(transmitted.getStatus()).isEqualTo(TRANSPORTATION_RECIPIENT);
        assertThat(transmitted.getDeliveredAt())
                .isEqualTo(clock.instant().atOffset(ZoneOffset.ofHours(transmitted.getPickupPoint().getTimeOffset())));
    }

    @Transactional
    @Test
    void tryToDeliverWithNotAcceptedCode() {
        clock.setFixed(Instant.now(), ZoneId.systemDefault());

        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .type(OrderType.ON_DEMAND)
                        .build())
                .build());
        long orderId = order.getId();
        Order receivedOrder = orderFactory.receiveOrder(orderId);

        assertThatThrownBy(() -> orderCommandService.deliver(receivedOrder, OrderDeliveryType.VERIFICATION_CODE, null))
                .isExactlyInstanceOf(TplForbiddenException.class);
    }

    @Transactional
    @Test
    void successDeliverWithNotAcceptedCodeClientVerification() {
        clock.setFixed(Instant.now(), ZoneId.systemDefault());

        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());
        long orderId = order.getId();
        order = orderFactory.receiveOrder(orderId);

        orderCommandService.deliver(order, OrderDeliveryType.PASSPORT, null);

        Order transmitted = orderRepository.findByIdOrThrow(order.getId());
        assertThat(transmitted.getStatus()).isEqualTo(TRANSMITTED_TO_RECIPIENT);
        assertThat(transmitted.getDeliveredAt())
                .isEqualTo(clock.instant().atOffset(ZoneOffset.ofHours(transmitted.getPickupPoint().getTimeOffset())));
    }

    @Transactional
    @Test
    void successDeliverWithNotAcceptedCodeAndDisabledOnDemandVerification() {
        configurationGlobalCommandService.setValue(VERIFICATION_CODE_ON_DEMAND_ENABLED, false);
        clock.setFixed(Instant.now(), ZoneId.systemDefault());

        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .type(OrderType.ON_DEMAND)
                        .build())
                .build());
        long orderId = order.getId();
        order = orderFactory.receiveOrder(orderId);
        orderCommandService.deliver(order, OrderDeliveryType.PASSPORT, null);

        Order transmitted = orderRepository.findByIdOrThrow(order.getId());
        assertThat(transmitted.getStatus()).isEqualTo(TRANSPORTATION_RECIPIENT);
        assertThat(transmitted.getDeliveredAt())
                .isEqualTo(clock.instant().atOffset(ZoneOffset.ofHours(transmitted.getPickupPoint().getTimeOffset())));

        assertThat(
                orderHistoryRepository.getOrderHistory(order.getId()).stream()
                        .map(OrderHistoryRecord::getStatus)
                        .collect(Collectors.toList()))
                .containsExactly(CREATED, ARRIVED_TO_PICKUP_POINT, TRANSPORTATION_RECIPIENT);
    }

    @Transactional
    @Test
    void successDeliverWithNoVerificationCode() {
        clock.setFixed(Instant.now(), ZoneId.systemDefault());

        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .verificationCode(null)
                        .build())
                .build());
        long orderId = order.getId();
        order = orderFactory.receiveOrder(orderId);
        orderCommandService.deliver(order, OrderDeliveryType.PASSPORT, null);

        Order transmitted = orderRepository.findByIdOrThrow(order.getId());
        assertThat(transmitted.getStatus()).isEqualTo(TRANSMITTED_TO_RECIPIENT);
        assertThat(transmitted.getDeliveredAt())
                .isEqualTo(clock.instant().atOffset(ZoneOffset.ofHours(transmitted.getPickupPoint().getTimeOffset())));
    }

    @Test
    void commitDeliver() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());

        orderFactory.receiveOrder(order.getId());
        order = orderFactory.verifyOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.VERIFICATION_CODE, null);

        Order transmitted = orderRepository.findByIdOrThrow(order.getId());
        assertThat(transmitted.getStatus()).isEqualTo(TRANSMITTED_TO_RECIPIENT);
        orderCommandService.commitDeliver(order.getId());

        Order delivered = orderRepository.findByIdOrThrow(order.getId());
        assertThat(delivered.getStatus()).isEqualTo(DELIVERED_TO_RECIPIENT);
    }

    @Test
    void failCommitDeliver() {
        Order createdOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());

        Order shippedOrder = orderFactory.receiveOrder(createdOrder.getId());

        orderCommandService.commitDeliver(shippedOrder.getId());

        Order notDelivered = orderRepository.findByIdOrThrow(shippedOrder.getId());
        assertThat(notDelivered.getStatus()).isEqualTo(ARRIVED_TO_PICKUP_POINT);
    }

    @Transactional
    @Test
    void successCancelDeliveryForClientOrder() {
        Tracer.global().put(TRACER_LOGIN, LOGIN);
        clock.setFixed(Instant.now(), ZoneId.systemDefault());
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());

        orderFactory.receiveOrder(order.getId());
        order = orderFactory.verifyOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.VERIFICATION_CODE, null);

        assertEquals(orderAdditionalInfoRepository.findByOrderId(order.getId()).getDeliveredBy(), LOGIN);

        orderCommandService.cancelDelivery(order);

        assertNull(orderAdditionalInfoRepository.findByOrderId(order.getId()).getDeliveredBy());

        Order cancelled = orderRepository.findByIdOrThrow(order.getId());

        assertThat(cancelled.getStatus()).isEqualTo(ARRIVED_TO_PICKUP_POINT);
        assertThat(cancelled.getPaymentStatus()).isEqualTo(PAID);
        assertThat(cancelled.getDeliveredAt()).isNull();
        assertThat(cancelled.getCashboxPaymentStatus()).isNull();

        List<PvzOrderStatus> actualHistory = StreamEx.of(orderHistoryRepository.getOrderHistory(order.getId()))
                .map(OrderHistoryRecord::getStatus)
                .toList();

        List<PvzOrderStatus> expectedHistory = List.of(
                CREATED, ARRIVED_TO_PICKUP_POINT, TRANSMITTED_TO_RECIPIENT, ARRIVED_TO_PICKUP_POINT);

        assertThat(actualHistory).containsExactlyElementsOf(expectedHistory);
    }

    @Transactional
    @Test
    void successCancelDeliveryForOnDemandOrder() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        LocalDateTime deliveryTime = LocalDateTime.of(2021, 5, 25, 10, 30, 0);
        clock.setFixed(deliveryTime.toInstant(zone), zone);
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .type(OrderType.ON_DEMAND)
                        .build())
                .build());

        orderFactory.receiveOrder(order.getId());
        order = orderFactory.verifyOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.VERIFICATION_CODE, null);

        LocalDateTime cancellationTime =
                deliveryTime.plusMinutes(
                        DEFAULT_CHANGE_OUTBOUND_VERIFICATION_CODE_REQUESTED_DELAY.toMinutes()).plusHours(1);
        clock.setFixed(cancellationTime.toInstant(zone), zone);
        orderCommandService.cancelDelivery(order);

        Order cancelled = orderRepository.findByIdOrThrow(order.getId());

        assertThat(cancelled.getStatus()).isEqualTo(ARRIVED_TO_PICKUP_POINT);
        assertThat(cancelled.getPaymentStatus()).isEqualTo(PAID);
        assertThat(cancelled.getDeliveredAt()).isNull();
        assertThat(cancelled.getCashboxPaymentStatus()).isNull();

        List<PvzOrderStatus> actualHistory = StreamEx.of(orderHistoryRepository.getOrderHistory(order.getId()))
                .map(OrderHistoryRecord::getStatus)
                .toList();

        List<PvzOrderStatus> expectedHistory = List.of(
                CREATED, ARRIVED_TO_PICKUP_POINT, TRANSPORTATION_RECIPIENT, ARRIVED_TO_PICKUP_POINT,
                CHANGE_OUTBOUND_VERIFICATION_CODE_REQUESTED);

        assertThat(actualHistory).containsExactlyElementsOf(expectedHistory);
    }

    @Transactional
    @Test
    void successCancelDeliveryForOnDemandOrderWithoutRequestingNewVerificationCode() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        LocalDateTime deliveryTime = LocalDateTime.of(2021, 5, 25, 10, 30, 0);
        clock.setFixed(deliveryTime.toInstant(zone), zone);
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .type(OrderType.ON_DEMAND)
                        .build())
                .build());

        orderFactory.receiveOrder(order.getId());
        order = orderFactory.verifyOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.VERIFICATION_CODE, null);

        LocalDateTime cancellationTime =
                deliveryTime.plusMinutes(
                                DEFAULT_CHANGE_OUTBOUND_VERIFICATION_CODE_REQUESTED_DELAY.toMinutes())
                        .minusMinutes(2);
        clock.setFixed(cancellationTime.toInstant(zone), zone);
        orderCommandService.cancelDelivery(order);

        Order cancelled = orderRepository.findByIdOrThrow(order.getId());

        assertThat(cancelled.getStatus()).isEqualTo(ARRIVED_TO_PICKUP_POINT);
        assertThat(cancelled.getPaymentStatus()).isEqualTo(PAID);
        assertThat(cancelled.getDeliveredAt()).isNull();
        assertThat(cancelled.getCashboxPaymentStatus()).isNull();

        List<PvzOrderStatus> actualHistory = StreamEx.of(orderHistoryRepository.getOrderHistory(order.getId()))
                .map(OrderHistoryRecord::getStatus)
                .toList();

        List<PvzOrderStatus> expectedHistory = List.of(
                CREATED, ARRIVED_TO_PICKUP_POINT, TRANSPORTATION_RECIPIENT, ARRIVED_TO_PICKUP_POINT);

        assertThat(actualHistory).containsExactlyElementsOf(expectedHistory);
    }

    @Test
    void tryToCancelNotDeliveredOrder() {
        Order createdOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());

        orderFactory.receiveOrder(createdOrder.getId());

        assertThatThrownBy(() -> orderCommandService.cancelDelivery(createdOrder))
                .isExactlyInstanceOf(TplInvalidParameterException.class);
    }

    @Transactional
    @Test
    void cancelNotPrepaidOrder() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(UNKNOWN)
                        .paymentStatus(UNPAID)
                        .build())
                .build());

        order = orderFactory.receiveOrder(order.getId());
        order.setStatusAndCheckpoint(STORAGE_PERIOD_EXPIRED);
        orderFactory.updateOrder(order);
        orderFactory.verifyOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.VERIFICATION_CODE, CARD);

        orderCommandService.cancelDelivery(order);

        Order cancelled = orderRepository.findByIdOrThrow(order.getId());

        assertThat(cancelled.getStatus()).isEqualTo(ARRIVED_TO_PICKUP_POINT);
        assertThat(cancelled.getPaymentStatus()).isEqualTo(UNPAID);
    }

    @Test
    void updateRecipient() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .recipientName(OLD_NAME)
                        .recipientPhone(OLD_PHONE)
                        .recipientEmail(OLD_EMAIL)
                        .build())
                .build());
        orderCommandService.updateRecipient(order, NEW_NAME, null, "",
                OrderPersonal.builder()
                        .recipientFullNameId("1")
                        .recipientPhoneId("2")
                        .recipientEmailId("3")
                        .buyerYandexUid(99L)
                        .build());

        Order updatedOrder = orderRepository.findByIdOrThrow(order.getId());
        assertThat(updatedOrder.getRecipientName()).isEqualTo(NEW_NAME);
        assertThat(updatedOrder.getRecipientEmail()).isEqualTo(OLD_EMAIL);
        assertThat(updatedOrder.getRecipientPhone()).isEqualTo(OLD_PHONE);

        Optional<OrderPersonal> updatedPersonal = orderPersonalRepository.findActiveByOrderId(order.getId());
        assertThat(updatedPersonal).isPresent();
        assertThat(updatedPersonal.get().getRecipientFullNameId()).isEqualTo("1");
        assertThat(updatedPersonal.get().getRecipientPhoneId()).isEqualTo("2");
        assertThat(updatedPersonal.get().getRecipientEmailId()).isEqualTo("3");
        assertThat(updatedPersonal.get().getBuyerYandexUid()).isEqualTo(99L);

        orderCommandService.updateRecipient(order, DEFAULT_RECIPIENT_NAME, DEFAULT_RECIPIENT_EMAIL,
                DEFAULT_RECIPIENT_PHONE, null);
        updatedOrder = orderRepository.findByIdOrThrow(order.getId());
        assertThat(updatedOrder.getRecipientName()).isEqualTo(DEFAULT_RECIPIENT_NAME);
        assertThat(updatedOrder.getRecipientEmail()).isEqualTo(DEFAULT_RECIPIENT_EMAIL);
        assertThat(updatedOrder.getRecipientPhone()).isEqualTo(DEFAULT_RECIPIENT_PHONE);
    }

    @Test
    void updateDeliveryDate() {
        int capacity = 20;
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .capacity(capacity)
                        .storagePeriod(2)
                        .build());

        Instant creationTime = Instant.parse("2021-01-01T12:00:00Z");
        clock.setFixed(creationTime, clock.getZone());

        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .deliveryDate(LocalDate.now(clock))
                        .build())
                .build());

        assertThat(order.getStatus()).isEqualTo(CREATED);

        dbQueueTestUtil.executeAllQueueItems(PvzQueueType.CHANGE_PICKUP_POINT_CAPACITY);

        Instant updateTime = Instant.parse("2021-01-02T12:00:00Z");
        clock.setFixed(updateTime, clock.getZone());

        LocalDate date = LocalDate.now(clock);
        LocalTimeInterval localTimeInterval = LocalTimeInterval.valueOf("15:00-16:00");
        Interval interval = localTimeInterval.toInterval(date, ZoneOffset.ofHours(pickupPoint.getTimeOffset()));
        orderCommandService.updateDeliveryDate(order.getExternalId(), order.getIdOfPickupPoint(), interval);
        OrderSimpleParams updatedOrder = orderQueryService.getSimple(order.getId());

        assertThat(updatedOrder.getStatus()).isEqualTo(CREATED);
        assertThat(updatedOrder.getDeliveryDate()).isEqualTo(date);
        assertThat(updatedOrder.getDeliveryIntervalFrom()).isEqualTo(interval.getStart());
        assertThat(updatedOrder.getDeliveryIntervalTo()).isEqualTo(interval.getEnd());

        List<OrderHistoryRecord> historyRecords = orderHistoryRepository.getOrderHistory(order.getId());
        assertThat(historyRecords).hasSize(3);
        checkHistoryRecord(historyRecords.get(0), order, CREATED, CREATED.getCode(), creationTime);
        checkHistoryRecord(historyRecords.get(1), order,
                DELIVERY_DATE_UPDATED_BY_DELIVERY, DELIVERY_DATE_UPDATED_BY_DELIVERY.getCode(), updateTime);
        checkHistoryRecord(historyRecords.get(2), order, CREATED, CREATED.getCode(), updateTime);

        dbQueueTestUtil.executeAllQueueItems(PvzQueueType.CHANGE_PICKUP_POINT_CAPACITY);

        assertCapacityForDate(date.minusDays(2), capacity, pickupPoint);
        // это 2021-01-01, так как новая дата доставки менялась 2 числа, то не освободили место занимаемое заказом
        assertCapacityForDate(date.minusDays(1), capacity - 1, pickupPoint);
        assertCapacityForDate(date, capacity - 1, pickupPoint);
        assertCapacityForDate(date.plusDays(1), capacity - 1, pickupPoint);
        assertCapacityForDate(date.plusDays(2), capacity - 1, pickupPoint);
        assertCapacityForDate(date.plusDays(3), capacity, pickupPoint);
    }

    private void assertCapacityForDate(LocalDate date, int capacity, PickupPoint pickupPoint) {
        assertThat(capacityManager.calculateCapacityForPeriod(pickupPoint, date, 1).get(date))
                .isEqualTo(capacity);
    }

    @Test
    void verifyValidCode() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());
        order = orderFactory.receiveOrder(order.getId());

        var verifiedOrder =
                orderCommandService.verifyCode(order.getId(), pickupPoint.getId(), DEFAULT_VERIFICATION_CODE);

        assertThat(verifiedOrder.getOrderVerification()).isNotNull();
        assertThat(verifiedOrder.getOrderVerification().getVerificationAccepted()).isTrue();
        assertThat(verifiedOrder.getOrderVerification().getVerificationAttempts()).isEqualTo((short) 1);
    }

    @Test
    void verifyValidCodeSeveralTimes() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());
        order = orderFactory.receiveOrder(order.getId());

        orderCommandService.verifyCode(order.getId(), pickupPoint.getId(), DEFAULT_VERIFICATION_CODE);
        orderCommandService.verifyCode(order.getId(), pickupPoint.getId(), DEFAULT_VERIFICATION_CODE);
        var verifiedOrder =
                orderCommandService.verifyCode(order.getId(), pickupPoint.getId(), DEFAULT_VERIFICATION_CODE);

        assertThat(verifiedOrder.getOrderVerification()).isNotNull();
        assertThat(verifiedOrder.getOrderVerification().getVerificationAccepted()).isTrue();
        assertThat(verifiedOrder.getOrderVerification().getVerificationAttempts()).isEqualTo((short) 1);
    }

    @Test
    void verifyInvalidCode() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());
        order = orderFactory.receiveOrder(order.getId());

        var verifiedOrder =
                orderCommandService.verifyCode(order.getId(), pickupPoint.getId(), "99");

        assertThat(verifiedOrder.getOrderVerification()).isNotNull();
        assertThat(verifiedOrder.getOrderVerification().getVerificationAccepted()).isFalse();
        assertThat(verifiedOrder.getOrderVerification().getVerificationAttempts()).isEqualTo((short) 1);
    }

    @Test
    void successLastAttemptToVerifyCodeForOnDemandOrder() {
        configurationGlobalCommandService.setValue(ORDER_VERIFICATION_CODE_LIMIT, 1);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .type(OrderType.ON_DEMAND)
                        .build())
                .pickupPoint(pickupPoint)
                .build());
        order = orderFactory.receiveOrder(order.getId());

        var verifiedOrder =
                orderCommandService.verifyCode(order.getId(), pickupPoint.getId(), DEFAULT_VERIFICATION_CODE);

        assertThat(verifiedOrder.getOrderVerification()).isNotNull();
        assertThat(verifiedOrder.getOrderVerification().getVerificationAccepted()).isTrue();
        assertThat(verifiedOrder.getStatus()).isEqualTo(ARRIVED_TO_PICKUP_POINT);
    }

    @Test
    void failedLastAttemptToVerifyCodeForOnDemandOrder() {
        configurationGlobalCommandService.setValue(ORDER_VERIFICATION_CODE_LIMIT, 1);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .type(OrderType.ON_DEMAND)
                        .build())
                .pickupPoint(pickupPoint)
                .build());
        order = orderFactory.receiveOrder(order.getId());

        var notVerifiedOrder = orderCommandService.verifyCode(order.getId(), pickupPoint.getId(), "99");

        assertThat(notVerifiedOrder.getStatus()).isEqualTo(READY_FOR_RETURN);
    }

    @Test
    void noAttemptsToVerifyCode() {
        configurationGlobalCommandService.setValue(ORDER_VERIFICATION_CODE_LIMIT, 1);

        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());
        long orderId = order.getId();
        orderFactory.receiveOrder(orderId);

        orderCommandService.verifyCode(orderId, pickupPoint.getId(), "99");
        assertThatThrownBy(() -> orderCommandService.verifyCode(
                orderId, pickupPoint.getId(), DEFAULT_VERIFICATION_CODE))
                .isExactlyInstanceOf(TplForbiddenException.class);
    }

    @Test
    void unableToVerifyCodeForCreatedOrder() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());

        assertThatThrownBy(() -> orderCommandService.verifyCode(
                order.getId(), pickupPoint.getId(), DEFAULT_VERIFICATION_CODE))
                .isExactlyInstanceOf(TplIllegalStateException.class);
    }

    @Test
    void unableToVerifyCodeForDeliveredOrder() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());
        orderFactory.receiveOrder(order.getId());
        orderFactory.verifyOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.VERIFICATION_CODE, null);

        assertThatThrownBy(() -> orderCommandService.verifyCode(
                order.getId(), pickupPoint.getId(), DEFAULT_VERIFICATION_CODE))
                .isExactlyInstanceOf(TplIllegalStateException.class);
    }

    @Test
    void unableToVerifyOrderWithoutCode() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .verificationCode(null)
                        .build())
                .build());

        orderFactory.receiveOrder(order.getId());

        assertThatThrownBy(() -> orderCommandService.verifyCode(
                order.getId(), pickupPoint.getId(), DEFAULT_VERIFICATION_CODE))
                .isExactlyInstanceOf(TplIllegalStateException.class);
    }

    @Test
    void getExpirationDatesWithHoliday() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPointFactory.updateCalendarOverrides(
                pickupPoint.getId(),
                false,
                StreamEx.of((pickupPoint.getSchedule().getScheduleDays()))
                        .filter(d -> !d.getIsWorkingDay())
                        .map(PickupPointScheduleDay::getDayOfWeek)
                        .toList(),
                List.of(
                        PickupPointCalendarOverrideParams.builder()
                                .isHoliday(true)
                                .date(LocalDate.now().plusDays(10))
                                .build()
                ));

        var expirationDate = LocalDate.now().plusDays(9);
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .expirationDate(expirationDate)
                        .build())
                .build());
        orderFactory.setStatusAndCheckpoint(order.getId(), PvzOrderStatus.ARRIVED_TO_PICKUP_POINT);

        var dates = expirationDateService.getPossibleFutureExpirationDates(order.getExternalId(), pickupPoint.getId());
        var expected = new ArrayList<>();
        for (int days = 1; days <= 7; days++) {
            expected.add(expirationDate.plusDays(days));
        }
        expected.remove(LocalDate.now().plusDays(10));
        assertThat(dates).isEqualTo(expected);
    }

    @Test
    void extendStoragePeriodWrongOrderId() {
        assertThatThrownBy(() -> orderCommandService
                .extendStoragePeriodFromAppForLastOrderByExternalId("badId", LocalDate.now()));
    }

    @Test
    void extendStoragePeriodWrongDate() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var expirationDate = LocalDate.of(2021, 8, 2);
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .expirationDate(expirationDate)
                        .build())
                .build());
        orderFactory.setStatusAndCheckpoint(order.getId(), PvzOrderStatus.ARRIVED_TO_PICKUP_POINT);

        assertThatThrownBy(() ->
                orderCommandService.extendStoragePeriodFromAppForLastOrderByExternalId(order.getExternalId(),
                        LocalDate.of(2022, 8, 2))
        );
    }

    @Test
    void extendStoragePeriod() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .expirationDate(LocalDate.of(2021, 8, 2))
                        .build())
                .build());
        orderFactory.setStatusAndCheckpoint(order.getId(), PvzOrderStatus.ARRIVED_TO_PICKUP_POINT);

        var expirationDate = LocalDate.of(2021, 8, 5);
        orderCommandService.extendStoragePeriodFromAppForLastOrderByExternalId(order.getExternalId(), expirationDate);

        var actual = orderRepository.findByIdOrThrow(order.getId());
        assertThat(actual.getExpirationDate()).isEqualTo(expirationDate);
        assertThat(actual.getStatus()).isEqualTo(STORAGE_PERIOD_EXTENDED);
        assertThat(actual.getDsApiCheckpoint()).isEqualTo(STORAGE_PERIOD_EXTENDED.getCode());

        assertThat(
                orderHistoryRepository.getOrderHistory(order.getId()).stream()
                        .map(OrderHistoryRecord::getStatus)
                        .collect(Collectors.toList()))
                .containsExactly(CREATED, ARRIVED_TO_PICKUP_POINT, STORAGE_PERIOD_EXTENDED);

        var additionalInfo = orderQueryService.getOrderAdditionalParams(order.getId());
        assertThat(additionalInfo.getStoragePeriodExtended()).isTrue();
    }

    @Test
    void enrichOrder() {
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .items(List.of(
                                TestOrderFactory.OrderItemParams.builder()
                                        .vendorArticle("article1")
                                        .vendorId(101L)
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .vendorArticle("article2")
                                        .vendorId(102L)
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .isService(true)
                                        .name("Доставка")
                                        .count(1)
                                        .build()
                        ))
                        .build())
                .build());
        var checkouterOrder = new ru.yandex.market.checkout.checkouter.order.Order();
        var delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        checkouterOrder.setFulfilment(true);
        checkouterOrder.setDelivery(delivery);

        var item1 = new OrderItem();
        item1.setFeedOfferId(FeedOfferId.from(1, "1"));
        item1.setShopSku("article1");
        item1.setSupplierId(101L);
        item1.setPictures(List.of(
                new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id11.jpeg/"),
                new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id12.jpeg/")
        ));
        item1.setMainPicture(List.of(new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id10.jpeg/")));

        ItemParameter color = new ItemParameter();
        color.setSubType(ITEM_PARAMETER_COLOR);
        color.setValue("red");

        ItemParameter size = new ItemParameter();
        size.setSubType(ITEM_PARAMETER_SIZE);
        UnitValue unit = new UnitValue();
        unit.setDefaultUnit(true);
        unit.setValues(List.of("46"));
        size.setUnits(List.of(unit));

        item1.setKind2Parameters(List.of(size, color));

        var item2 = new OrderItem();
        item1.setFeedOfferId(FeedOfferId.from(2, "2"));
        item2.setShopSku("article2");
        item2.setSupplierId(102L);
        item2.setPictures(List.of(
                new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id21.jpeg/"),
                new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id22.jpeg/")
        ));
        item2.setMainPicture(List.of(new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id20.jpeg/")));

        checkouterOrder.setItems(List.of(item1, item2));

        orderCommandService.enrichOrder(order.getExternalId(), checkouterOrder, order.getIdOfPickupPoint());

        var enrichedOrder = orderQueryService.get(order.getId());
        assertThat(enrichedOrder.getIsClickAndCollect()).isFalse();
        assertThat(enrichedOrder.getFbs()).isFalse();
        assertThat(enrichedOrder.getIsDropShipBySeller()).isFalse();
        assertThat(enrichedOrder.getPartialDeliveryAvailable()).isFalse();
        assertThat(enrichedOrder.getItems()).hasSize(3);

        var enrichedItems = StreamEx.of(enrichedOrder.getItems())
                .filter(item -> !item.isService())
                .sorted(Comparator.comparing(i -> i.getVendorArticle().getArticle()))
                .toList();
        assertThat(enrichedItems.get(0).getPhotoUrl())
                .isEqualTo("//avatars.mds.yandex.net/get-mpic/1866164/img_id10.jpeg/");
        assertThat(enrichedItems.get(0).getPhotoUrls()).isEqualTo(List.of(
                "//avatars.mds.yandex.net/get-mpic/1866164/img_id11.jpeg/",
                "//avatars.mds.yandex.net/get-mpic/1866164/img_id12.jpeg/")
        );
        assertThat(enrichedItems.get(0).getColor()).isEqualTo("red");
        assertThat(enrichedItems.get(0).getSize()).isEqualTo("46");
        assertThat(enrichedItems.get(1).getPhotoUrl())
                .isEqualTo("//avatars.mds.yandex.net/get-mpic/1866164/img_id20.jpeg/");
        assertThat(enrichedItems.get(1).getPhotoUrls()).isEqualTo(List.of(
                "//avatars.mds.yandex.net/get-mpic/1866164/img_id21.jpeg/",
                "//avatars.mds.yandex.net/get-mpic/1866164/img_id22.jpeg/")
        );
        assertThat(enrichedItems.get(1).getColor()).isNull();
        assertThat(enrichedItems.get(1).getSize()).isNull();
    }

    @Test
    void enrichDropShipBySellerOrder() {
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .items(List.of(
                                TestOrderFactory.OrderItemParams.builder()
                                        .vendorArticle("article1")
                                        .vendorId(101L)
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .vendorArticle("article2")
                                        .vendorId(102L)
                                        .build()
                        ))
                        .build())
                .build());
        var checkouterOrder = new ru.yandex.market.checkout.checkouter.order.Order();
        checkouterOrder.setItems(List.of(new OrderItem()));
        var delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        delivery.setType(DeliveryType.DELIVERY);
        checkouterOrder.setDelivery(delivery);

        orderCommandService.enrichOrder(order.getExternalId(), checkouterOrder, order.getIdOfPickupPoint());

        var enrichedOrder = orderQueryService.get(order.getId());
        assertThat(enrichedOrder.getIsClickAndCollect()).isFalse();
        assertThat(enrichedOrder.getFbs()).isFalse();
        assertThat(enrichedOrder.getIsDropShipBySeller()).isTrue();
        assertThat(enrichedOrder.getPartialDeliveryAvailable()).isFalse();
    }

    @Test
    void enrichClickAndCollectOrder() {
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .items(List.of(
                                TestOrderFactory.OrderItemParams.builder()
                                        .vendorArticle("article1")
                                        .vendorId(101L)
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .vendorArticle("article2")
                                        .vendorId(102L)
                                        .build()
                        ))
                        .build())
                .build());
        var checkouterOrder = new ru.yandex.market.checkout.checkouter.order.Order();
        checkouterOrder.setItems(List.of(new OrderItem()));
        var delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        delivery.setType(DeliveryType.PICKUP);
        checkouterOrder.setDelivery(delivery);

        orderCommandService.enrichOrder(order.getExternalId(), checkouterOrder, order.getIdOfPickupPoint());

        var enrichedOrder = orderQueryService.get(order.getId());
        assertThat(enrichedOrder.getIsClickAndCollect()).isTrue();
        assertThat(enrichedOrder.getFbs()).isFalse();
        assertThat(enrichedOrder.getIsDropShipBySeller()).isFalse();
        assertThat(enrichedOrder.getPartialDeliveryAvailable()).isFalse();
    }

    @Test
    void enrichDropShipOrder() {
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .items(List.of(
                                TestOrderFactory.OrderItemParams.builder()
                                        .vendorArticle("article1")
                                        .vendorId(101L)
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .vendorArticle("article2")
                                        .vendorId(102L)
                                        .build()
                        ))
                        .build())
                .build());
        var checkouterOrder = new ru.yandex.market.checkout.checkouter.order.Order();
        checkouterOrder.setFulfilment(false);
        checkouterOrder.setItems(List.of(new OrderItem()));
        var delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        checkouterOrder.setDelivery(delivery);

        orderCommandService.enrichOrder(order.getExternalId(), checkouterOrder, order.getIdOfPickupPoint());

        var enrichedOrder = orderQueryService.get(order.getId());
        assertThat(enrichedOrder.getIsClickAndCollect()).isFalse();
        assertThat(enrichedOrder.getFbs()).isTrue();
        assertThat(enrichedOrder.getIsDropShipBySeller()).isFalse();
        assertThat(enrichedOrder.getPartialDeliveryAvailable()).isFalse();
    }

    @Test
    void enrichFashionOrder() {
        var pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.PickupPointTestParams
                .builder()
                .brandingType(PickupPointBrandingType.FULL)
                .build());
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .items(List.of(
                                TestOrderFactory.OrderItemParams.builder()
                                        .count(1)
                                        .vendorArticle("article1")
                                        .vendorId(101L)
                                        .cargoTypeCodes(List.of(CargoType.FASHION.getCode()))
                                        .uitValues(List.of(UIT_1_1))
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .count(1)
                                        .vendorArticle("article2")
                                        .vendorId(102L)
                                        .cargoTypeCodes(List.of(CargoType.FASHION.getCode()))
                                        .uitValues(List.of(UIT_2_1))
                                        .build()
                        ))
                        .build())
                .build());
        var checkouterOrder = new ru.yandex.market.checkout.checkouter.order.Order();
        checkouterOrder.setFulfilment(false);
        checkouterOrder.setItems(List.of(new OrderItem()));
        var delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        checkouterOrder.setFulfilment(true);
        checkouterOrder.setDelivery(delivery);

        orderCommandService.enrichOrder(order.getExternalId(), checkouterOrder, order.getIdOfPickupPoint());

        var enrichedOrder = orderQueryService.get(order.getId());
        assertThat(enrichedOrder.getIsClickAndCollect()).isFalse();
        assertThat(enrichedOrder.getFbs()).isFalse();
        assertThat(enrichedOrder.getIsDropShipBySeller()).isFalse();
        assertThat(enrichedOrder.getPartialDeliveryAvailable()).isTrue();
    }

    @Test
    void enrichFashionOrderForNonBrandedPickupPoint() {
        var pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.PickupPointTestParams
                .builder()
                .brandingType(PickupPointBrandingType.NONE)
                .build());
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .items(List.of(
                                TestOrderFactory.OrderItemParams.builder()
                                        .count(1)
                                        .vendorArticle("article1")
                                        .vendorId(101L)
                                        .cargoTypeCodes(List.of(CargoType.FASHION.getCode()))
                                        .uitValues(List.of(UIT_1_1))
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .count(1)
                                        .vendorArticle("article2")
                                        .vendorId(102L)
                                        .cargoTypeCodes(List.of(CargoType.FASHION.getCode()))
                                        .uitValues(List.of(UIT_2_1))
                                        .build()
                        ))
                        .build())
                .build());
        var checkouterOrder = new ru.yandex.market.checkout.checkouter.order.Order();
        checkouterOrder.setFulfilment(false);
        checkouterOrder.setItems(List.of(new OrderItem()));
        var delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        checkouterOrder.setFulfilment(true);
        checkouterOrder.setDelivery(delivery);

        orderCommandService.enrichOrder(order.getExternalId(), checkouterOrder, order.getIdOfPickupPoint());

        var enrichedOrder = orderQueryService.get(order.getId());
        assertThat(enrichedOrder.getIsClickAndCollect()).isFalse();
        assertThat(enrichedOrder.getFbs()).isFalse();
        assertThat(enrichedOrder.getIsDropShipBySeller()).isFalse();
        assertThat(enrichedOrder.getPartialDeliveryAvailable()).isFalse();
    }

    @Test
    void enrichFashionOnDemandOrder() {
        var pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.PickupPointTestParams
                .builder()
                .brandingType(PickupPointBrandingType.FULL)
                .build());
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .type(OrderType.ON_DEMAND)
                        .items(List.of(
                                TestOrderFactory.OrderItemParams.builder()
                                        .count(1)
                                        .vendorArticle("article1")
                                        .vendorId(101L)
                                        .cargoTypeCodes(List.of(CargoType.FASHION.getCode()))
                                        .uitValues(List.of(UIT_1_1))
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .count(1)
                                        .vendorArticle("article2")
                                        .vendorId(102L)
                                        .cargoTypeCodes(List.of(CargoType.FASHION.getCode()))
                                        .uitValues(List.of(UIT_2_1))
                                        .build()
                        ))
                        .build())
                .build());
        var checkouterOrder = new ru.yandex.market.checkout.checkouter.order.Order();
        checkouterOrder.setFulfilment(false);
        checkouterOrder.setItems(List.of(new OrderItem()));
        var delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        checkouterOrder.setFulfilment(true);
        checkouterOrder.setDelivery(delivery);

        orderCommandService.enrichOrder(order.getExternalId(), checkouterOrder, order.getIdOfPickupPoint());

        var enrichedOrder = orderQueryService.get(order.getId());
        assertThat(enrichedOrder.getIsClickAndCollect()).isFalse();
        assertThat(enrichedOrder.getFbs()).isFalse();
        assertThat(enrichedOrder.getIsDropShipBySeller()).isFalse();
        assertThat(enrichedOrder.getPartialDeliveryAvailable()).isFalse();
    }

    @Test
    void enrichFbyFashionOrderWithoutUits() {
        var pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.PickupPointTestParams
                .builder()
                .brandingType(PickupPointBrandingType.FULL)
                .build());
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .items(List.of(
                                TestOrderFactory.OrderItemParams.builder()
                                        .count(1)
                                        .vendorArticle("article1")
                                        .vendorId(101L)
                                        .cargoTypeCodes(List.of(CargoType.FASHION.getCode()))
                                        .uitValues(List.of(UIT_1_1))
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .count(1)
                                        .vendorArticle("article2")
                                        .vendorId(102L)
                                        .cargoTypeCodes(List.of(CargoType.FASHION.getCode()))
                                        .uitValues(List.of())
                                        .build()
                        ))
                        .build())
                .build());
        var checkouterOrder = new ru.yandex.market.checkout.checkouter.order.Order();
        checkouterOrder.setFulfilment(false);
        checkouterOrder.setItems(List.of(new OrderItem()));
        var delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        checkouterOrder.setFulfilment(true);
        checkouterOrder.setDelivery(delivery);

        orderCommandService.enrichOrder(order.getExternalId(), checkouterOrder, order.getIdOfPickupPoint());

        var enrichedOrder = orderQueryService.get(order.getId());
        assertThat(enrichedOrder.getIsClickAndCollect()).isFalse();
        assertThat(enrichedOrder.getFbs()).isFalse();
        assertThat(enrichedOrder.getIsDropShipBySeller()).isFalse();
        assertThat(enrichedOrder.getPartialDeliveryAvailable()).isFalse();
    }

    @Test
    void enrichFbsFashionOrderWithCis() {
        var pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.PickupPointTestParams
                .builder()
                .brandingType(PickupPointBrandingType.FULL)
                .build());
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .items(List.of(
                                TestOrderFactory.OrderItemParams.builder()
                                        .count(1)
                                        .vendorArticle("article1")
                                        .vendorId(101L)
                                        .cargoTypeCodes(List.of(CargoType.FASHION.getCode()))
                                        .cisValues(List.of(CIS_1_1))
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .count(1)
                                        .vendorArticle("article2")
                                        .vendorId(102L)
                                        .cargoTypeCodes(List.of(CargoType.FASHION.getCode()))
                                        .cisValues(List.of(CIS_2_1))
                                        .build()
                        ))
                        .build())
                .build());
        var checkouterOrder = new ru.yandex.market.checkout.checkouter.order.Order();
        checkouterOrder.setFulfilment(false);
        checkouterOrder.setItems(List.of(new OrderItem()));
        var delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        checkouterOrder.setDelivery(delivery);

        orderCommandService.enrichOrder(order.getExternalId(), checkouterOrder, order.getIdOfPickupPoint());

        var enrichedOrder = orderQueryService.get(order.getId());
        assertThat(enrichedOrder.getIsClickAndCollect()).isFalse();
        assertThat(enrichedOrder.getFbs()).isTrue();
        assertThat(enrichedOrder.getIsDropShipBySeller()).isFalse();
        assertThat(enrichedOrder.getPartialDeliveryAvailable()).isTrue();
    }

    @Test
    void enrichFbsFashionOrderWithoutAnyCodes() {
        var pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.PickupPointTestParams
                .builder()
                .brandingType(PickupPointBrandingType.FULL)
                .build());
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .items(List.of(
                                TestOrderFactory.OrderItemParams.builder()
                                        .count(1)
                                        .vendorArticle("article1")
                                        .vendorId(101L)
                                        .cargoTypeCodes(List.of(CargoType.FASHION.getCode()))
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .count(1)
                                        .vendorArticle("article2")
                                        .vendorId(102L)
                                        .cargoTypeCodes(List.of(CargoType.FASHION.getCode()))
                                        .build()
                        ))
                        .build())
                .build());
        var checkouterOrder = new ru.yandex.market.checkout.checkouter.order.Order();
        checkouterOrder.setFulfilment(false);
        checkouterOrder.setItems(List.of(new OrderItem()));
        var delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        checkouterOrder.setDelivery(delivery);

        orderCommandService.enrichOrder(order.getExternalId(), checkouterOrder, order.getIdOfPickupPoint());

        var enrichedOrder = orderQueryService.get(order.getId());
        assertThat(enrichedOrder.getIsClickAndCollect()).isFalse();
        assertThat(enrichedOrder.getFbs()).isTrue();
        assertThat(enrichedOrder.getIsDropShipBySeller()).isFalse();
        assertThat(enrichedOrder.getPartialDeliveryAvailable()).isTrue();
    }

    @Test
    void enrichOrderItemsWithNoPhotos() {
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .items(List.of(
                                TestOrderFactory.OrderItemParams.builder()
                                        .vendorArticle("article1")
                                        .vendorId(101L)
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .vendorArticle("article2")
                                        .vendorId(102L)
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .isService(true)
                                        .name("Доставка")
                                        .count(1)
                                        .build()
                        ))
                        .build())
                .build());
        var checkouterOrder = new ru.yandex.market.checkout.checkouter.order.Order();
        var delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        checkouterOrder.setFulfilment(true);
        checkouterOrder.setDelivery(delivery);

        var item1 = new OrderItem();
        item1.setFeedOfferId(FeedOfferId.from(1, "1"));
        item1.setShopSku("article1");
        item1.setSupplierId(101L);

        var item2 = new OrderItem();
        item1.setFeedOfferId(FeedOfferId.from(2, "2"));
        item2.setShopSku("article2");
        item2.setSupplierId(102L);

        checkouterOrder.setItems(List.of(item1, item2));

        orderCommandService.enrichOrder(order.getExternalId(), checkouterOrder, order.getIdOfPickupPoint());

        var enrichedOrder = orderQueryService.get(order.getId());
        assertThat(enrichedOrder.getIsClickAndCollect()).isFalse();
        assertThat(enrichedOrder.getFbs()).isFalse();
        assertThat(enrichedOrder.getIsDropShipBySeller()).isFalse();
        assertThat(enrichedOrder.getItems()).hasSize(3);

        var enrichedItems = StreamEx.of(enrichedOrder.getItems())
                .filter(item -> !item.isService())
                .sorted(Comparator.comparing(i -> i.getVendorArticle().getArticle()))
                .toList();
        assertThat(enrichedItems.get(0).getPhotoUrl()).isNull();
        assertThat(enrichedItems.get(0).getPhotoUrls()).isEqualTo(List.of());
        assertThat(enrichedItems.get(1).getPhotoUrl()).isNull();
        assertThat(enrichedItems.get(1).getPhotoUrls()).isEqualTo(List.of());
    }

    @Test
    void enrichOrderItemsWithNoMainPhoto() {
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .items(List.of(
                                TestOrderFactory.OrderItemParams.builder()
                                        .vendorArticle("article1")
                                        .vendorId(101L)
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .vendorArticle("article2")
                                        .vendorId(102L)
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .isService(true)
                                        .name("Доставка")
                                        .count(1)
                                        .build()
                        ))
                        .build())
                .build());
        var checkouterOrder = new ru.yandex.market.checkout.checkouter.order.Order();
        var delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        checkouterOrder.setFulfilment(true);
        checkouterOrder.setDelivery(delivery);

        var item1 = new OrderItem();
        item1.setFeedOfferId(FeedOfferId.from(1, "1"));
        item1.setShopSku("article1");
        item1.setSupplierId(101L);
        item1.setPictures(List.of(
                new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id11.jpeg/"),
                new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id12.jpeg/")
        ));

        var item2 = new OrderItem();
        item2.setFeedOfferId(FeedOfferId.from(2, "2"));
        item2.setShopSku("article2");
        item2.setSupplierId(102L);
        item2.setPictures(List.of(
                new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id21.jpeg/"),
                new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id22.jpeg/")
        ));

        checkouterOrder.setItems(List.of(item1, item2));

        orderCommandService.enrichOrder(order.getExternalId(), checkouterOrder, order.getIdOfPickupPoint());

        var enrichedOrder = orderQueryService.get(order.getId());
        assertThat(enrichedOrder.getIsClickAndCollect()).isFalse();
        assertThat(enrichedOrder.getFbs()).isFalse();
        assertThat(enrichedOrder.getIsDropShipBySeller()).isFalse();
        assertThat(enrichedOrder.getItems()).hasSize(3);

        var enrichedItems = StreamEx.of(enrichedOrder.getItems())
                .filter(item -> !item.isService())
                .sorted(Comparator.comparing(i -> i.getVendorArticle().getArticle()))
                .toList();
        assertThat(enrichedItems.get(0).getPhotoUrl())
                .isEqualTo("//avatars.mds.yandex.net/get-mpic/1866164/img_id11.jpeg/");
        assertThat(enrichedItems.get(0).getPhotoUrls()).isEqualTo(List.of(
                "//avatars.mds.yandex.net/get-mpic/1866164/img_id11.jpeg/",
                "//avatars.mds.yandex.net/get-mpic/1866164/img_id12.jpeg/")
        );
        assertThat(enrichedItems.get(1).getPhotoUrl())
                .isEqualTo("//avatars.mds.yandex.net/get-mpic/1866164/img_id21.jpeg/");
        assertThat(enrichedItems.get(1).getPhotoUrls()).isEqualTo(List.of(
                "//avatars.mds.yandex.net/get-mpic/1866164/img_id21.jpeg/",
                "//avatars.mds.yandex.net/get-mpic/1866164/img_id22.jpeg/")
        );
    }

    @Test
    void enrichOrderItemsWithDuplicatedByVendorAndArticle() {
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .items(List.of(
                                TestOrderFactory.OrderItemParams.builder()
                                        .vendorArticle("article1")
                                        .vendorId(101L)
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .vendorArticle("article1")
                                        .vendorId(101L)
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .isService(true)
                                        .name("Доставка")
                                        .count(1)
                                        .build()
                        ))
                        .build())
                .build());
        var checkouterOrder = new ru.yandex.market.checkout.checkouter.order.Order();
        var delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        checkouterOrder.setFulfilment(true);
        checkouterOrder.setDelivery(delivery);

        var item1 = new OrderItem();
        item1.setFeedOfferId(FeedOfferId.from(1, "1"));
        item1.setShopSku("article1");
        item1.setSupplierId(101L);
        item1.setPictures(List.of(
                new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id11.jpeg/"),
                new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id12.jpeg/")
        ));
        item1.setMainPicture(List.of(new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id10.jpeg/")));

        checkouterOrder.setItems(List.of(item1));

        orderCommandService.enrichOrder(order.getExternalId(), checkouterOrder, order.getIdOfPickupPoint());

        var enrichedOrder = orderQueryService.get(order.getId());
        assertThat(enrichedOrder.getIsClickAndCollect()).isFalse();
        assertThat(enrichedOrder.getFbs()).isFalse();
        assertThat(enrichedOrder.getIsDropShipBySeller()).isFalse();
        assertThat(enrichedOrder.getItems()).hasSize(3);

        var enrichedItems = StreamEx.of(enrichedOrder.getItems())
                .filter(item -> !item.isService())
                .sorted(Comparator.comparing(i -> i.getVendorArticle().getArticle()))
                .toList();
        assertThat(enrichedItems.get(0).getPhotoUrl())
                .isEqualTo("//avatars.mds.yandex.net/get-mpic/1866164/img_id10.jpeg/");
        assertThat(enrichedItems.get(0).getPhotoUrls()).isEqualTo(List.of(
                "//avatars.mds.yandex.net/get-mpic/1866164/img_id11.jpeg/",
                "//avatars.mds.yandex.net/get-mpic/1866164/img_id12.jpeg/")
        );
        assertThat(enrichedItems.get(1).getPhotoUrl())
                .isEqualTo("//avatars.mds.yandex.net/get-mpic/1866164/img_id10.jpeg/");
        assertThat(enrichedItems.get(1).getPhotoUrls()).isEqualTo(List.of(
                "//avatars.mds.yandex.net/get-mpic/1866164/img_id11.jpeg/",
                "//avatars.mds.yandex.net/get-mpic/1866164/img_id12.jpeg/")
        );
    }

    @Test
    void enrichOrderItemsWithNullArticle() {
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .items(List.of(
                                TestOrderFactory.OrderItemParams.builder()
                                        .vendorArticle(null)
                                        .vendorId(101L)
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .vendorArticle("article2")
                                        .vendorId(null)
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .isService(true)
                                        .name("Доставка")
                                        .count(1)
                                        .build()
                        ))
                        .build())
                .build());
        var checkouterOrder = new ru.yandex.market.checkout.checkouter.order.Order();
        var delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        checkouterOrder.setFulfilment(true);
        checkouterOrder.setDelivery(delivery);

        var item1 = new OrderItem();
        item1.setFeedOfferId(FeedOfferId.from(1, "1"));
        item1.setShopSku("article1");
        item1.setSupplierId(101L);
        item1.setPictures(List.of(
                new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id11.jpeg/"),
                new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id12.jpeg/")
        ));
        item1.setMainPicture(List.of(new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id10.jpeg/")));

        var item2 = new OrderItem();
        item1.setFeedOfferId(FeedOfferId.from(2, "2"));
        item2.setShopSku("article2");
        item2.setSupplierId(102L);
        item2.setPictures(List.of(
                new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id21.jpeg/"),
                new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id22.jpeg/")
        ));
        item2.setMainPicture(List.of(new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id20.jpeg/")));

        checkouterOrder.setItems(List.of(item1, item2));

        orderCommandService.enrichOrder(order.getExternalId(), checkouterOrder, order.getIdOfPickupPoint());

        var enrichedOrder = orderQueryService.get(order.getId());
        assertThat(enrichedOrder.getIsClickAndCollect()).isFalse();
        assertThat(enrichedOrder.getFbs()).isFalse();
        assertThat(enrichedOrder.getIsDropShipBySeller()).isFalse();
        assertThat(enrichedOrder.getItems()).hasSize(3);

        var enrichedItems = enrichedOrder.getItems();
        assertThat(enrichedItems.get(0).getPhotoUrl()).isNull();
        assertThat(enrichedItems.get(0).getPhotoUrls()).isNull();
        assertThat(enrichedItems.get(1).getPhotoUrl()).isNull();
        assertThat(enrichedItems.get(1).getPhotoUrls()).isNull();
    }

    @Test
    void enrichOrderWithInconsistentPaymentType() {
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(CASH)
                        .items(List.of(
                                TestOrderFactory.OrderItemParams.builder()
                                        .vendorArticle("article1")
                                        .vendorId(101L)
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .vendorArticle("article2")
                                        .vendorId(102L)
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .isService(true)
                                        .name("Доставка")
                                        .count(1)
                                        .build()
                        ))
                        .build())
                .build());
        var checkouterOrder = new ru.yandex.market.checkout.checkouter.order.Order();
        checkouterOrder.setPaymentType(PaymentType.PREPAID);
        var delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        checkouterOrder.setFulfilment(true);
        checkouterOrder.setDelivery(delivery);

        var item1 = new OrderItem();
        item1.setFeedOfferId(FeedOfferId.from(1, "1"));
        item1.setShopSku("article1");
        item1.setSupplierId(101L);
        item1.setPictures(List.of(
                new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id11.jpeg/"),
                new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id12.jpeg/")
        ));
        item1.setMainPicture(List.of(new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id10.jpeg/")));

        ItemParameter color = new ItemParameter();
        color.setSubType(ITEM_PARAMETER_COLOR);
        color.setValue("red");

        ItemParameter size = new ItemParameter();
        size.setSubType(ITEM_PARAMETER_SIZE);
        UnitValue unit = new UnitValue();
        unit.setDefaultUnit(true);
        unit.setValues(List.of("46"));
        size.setUnits(List.of(unit));

        item1.setKind2Parameters(List.of(size, color));

        var item2 = new OrderItem();
        item1.setFeedOfferId(FeedOfferId.from(2, "2"));
        item2.setShopSku("article2");
        item2.setSupplierId(102L);
        item2.setPictures(List.of(
                new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id21.jpeg/"),
                new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id22.jpeg/")
        ));
        item2.setMainPicture(List.of(new OfferPicture("//avatars.mds.yandex.net/get-mpic/1866164/img_id20.jpeg/")));

        checkouterOrder.setItems(List.of(item1, item2));

        orderCommandService.enrichOrder(order.getExternalId(), checkouterOrder, order.getIdOfPickupPoint());
        verify(monitoring, times(1)).addTemporaryWarning(
                eq(PVZ_INCONSISTENT_ORDER_PAYMENT_TYPE), anyString(), eq(1L), eq(TimeUnit.HOURS));
    }

    @Test
    void testCommitPartialDeliver() {
        Order createdOrder = orderFactory.createOrder();
        orderFactory.forceDeliver(createdOrder.getId(), LocalDate.now());
        orderFactory.setStatusAndCheckpoint(createdOrder.getId(), ARRIVED_TO_PICKUP_POINT);
        orderFactory.setStatusOnly(createdOrder.getId(), TRANSMITTED_TO_RECIPIENT);

        OrderParams order = orderQueryService.get(createdOrder.getId());
        assertThat(order.getDsApiCheckpoint()).isEqualTo(ARRIVED_TO_PICKUP_POINT.getCode());
        assertThat(order.getStatus()).isEqualTo(TRANSMITTED_TO_RECIPIENT);
        assertThat(order.getBillingAt()).isNull();

        orderCommandService.commitPartialDeliver(order.getId());

        order = orderQueryService.get(createdOrder.getId());
        assertThat(order.getDsApiCheckpoint()).isEqualTo(TRANSMITTED_TO_RECIPIENT.getCode());
        assertThat(order.getStatus()).isEqualTo(TRANSMITTED_TO_RECIPIENT);
        assertThat(order.getBillingAt()).isNotNull();
    }

    @Test
    void testNotCommitPartialDeliverAlreadyDelivered() {
        Order createdOrder = orderFactory.createOrder();

        orderFactory.receiveOrder(createdOrder.getId());
        orderFactory.verifyOrder(createdOrder.getId());
        orderFactory.deliverOrder(createdOrder.getId(), OrderDeliveryType.BARCODE, CASH);

        OrderParams order = orderQueryService.get(createdOrder.getId());
        assertThat(order.getDsApiCheckpoint()).isEqualTo(TRANSMITTED_TO_RECIPIENT.getCode());
        assertThat(order.getStatus()).isEqualTo(TRANSMITTED_TO_RECIPIENT);

        assertThatThrownBy(() -> orderCommandService.commitPartialDeliver(order.getId()));
    }

    @ParameterizedTest
    @CsvSource({
            "SUCCESS, CARD, 123, LERA_TOP, VERIFICATION_CODE",
            "SUCCESS, CASH, 123, LERA_TOP, PAYMENT",
            "ERROR, CARD, 123, 3234c, PAYMENT"
    })
    void testCashboxCommitPayment(
            CashboxPaymentStatus paymentStatus,
            OrderPaymentType paymentType,
            String transactionId,
            String token,
            OrderDeliveryType deliveryType
    ) {
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder().externalId("123432").paymentType(paymentType).build())
                .build());
        orderFactory.receiveOrder(order.getId());
        orderFactory.verifyOrder(order.getId());

        transactionRepository.save(new OrderCashboxTransaction(order.getId(), transactionId, null, null,
                deliveryType, null, null, true, null, token));

        OrderCashboxTransactionParams.builder()
                .paymentStatus(paymentStatus)
                .paymentType(paymentType)
                .transactionId(transactionId)
                .callbackToken(token)
                .orderId(Long.valueOf(order.getExternalId()))
                .build();
        if (paymentStatus == SUCCESS) {
            orderCommandService.deliverThroughCashbox(paymentStatus, order.getId(), deliveryType, paymentType);
        }
        var orderCashboxParams =
                orderCommandService.commitPayment(transactionId, paymentStatus, paymentType);
        var updatedOrder = orderQueryService.get(order.getId());
        var expectedCashboxParams = OrderCashboxTransactionParams.builder()
                .paymentStatus(paymentStatus)
                .paymentType(paymentType)
                .transactionId(transactionId)
                .callbackToken(token)
                .orderId(order.getId())
                .deliveryType(deliveryType)
                .apiSupportAnswer(true)
                .build();

        assertThat(orderCashboxParams).isEqualTo(expectedCashboxParams);
        assertThat(updatedOrder.getCashboxPaymentStatus()).isEqualTo(paymentStatus == SUCCESS ? SUCCESS : null);
    }

    @Test
    void testSendToReturnExpiredOrders() {
        Instant now = Instant.now(clock);
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();

        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());
        Order dbsOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .deliveryServiceType(DeliveryServiceType.DBS).build()
                ).build());
        order = orderFactory.receiveOrder(order.getId());
        dbsOrder = orderFactory.receiveOrder(dbsOrder.getId());

        clock.setFixed(now.plus(pickupPoint.getStoragePeriod() + 2, ChronoUnit.DAYS), clock.getZone());

        orderCommandService.sendToReturnExpiredOrders();

        OrderParams expiredOrder = orderQueryService.get(order.getId());
        OrderParams expiredDbsOrder = orderQueryService.get(dbsOrder.getId());
        assertThat(expiredOrder.getStatus()).isEqualTo(STORAGE_PERIOD_EXPIRED);
        assertThat(expiredOrder.getDsApiCheckpoint()).isEqualTo(STORAGE_PERIOD_EXPIRED.getCode());
        assertThat(expiredDbsOrder.getStatus()).isEqualTo(READY_FOR_RETURN);
        assertThat(expiredDbsOrder.getDsApiCheckpoint()).isEqualTo(READY_FOR_RETURN.getCode());

        List<PvzOrderStatus> dbsOrderHistory = orderHistoryRepository.getOrderHistory(dbsOrder.getId()).stream()
                .map(OrderHistoryRecord::getStatus)
                .collect(Collectors.toList());
        assertThat(dbsOrderHistory)
                .isEqualTo(List.of(CREATED, ARRIVED_TO_PICKUP_POINT, STORAGE_PERIOD_EXPIRED, READY_FOR_RETURN));
    }

    @Test
    void testCreateSiblingGroupOnArrival() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        Order firstOrder = createAndReceiveOrder(pickupPoint);

        SiblingGroupParams noSiblingInfo = getSiblingGroupByRecipient(firstOrder);
        assertThat(noSiblingInfo).isEqualTo(SiblingGroupParams.EMPTY);

        Order secondOrder = createAndReceiveOrder(pickupPoint);

        SiblingGroupParams siblingGroup = getSiblingGroupByRecipient(secondOrder);
        SiblingGroupParams expectedInfo = new SiblingGroupParams(List.of(firstOrder.getExternalId()), null);

        assertThat(siblingGroup).isEqualTo(expectedInfo);
    }

    @Test
    void testOrdersWithDeliveredSiblingHaveDeliveredAt() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        OffsetDateTime expectedDeliveredAt = OffsetDateTime.now(clock);
        setClockAt(expectedDeliveredAt);

        Order firstOrder = createAndReceiveOrder(pickupPoint);
        Order secondOrder = createAndReceiveOrder(pickupPoint);
        Order thirdOrder = createAndReceiveOrder(pickupPoint);
        verifyAndDeliverOrder(firstOrder);

        SiblingGroupParams siblingGroup = getSiblingGroupByRecipient(secondOrder);
        SiblingGroupParams expectedInfo = new SiblingGroupParams(List.of(thirdOrder.getExternalId()), expectedDeliveredAt);

        assertThat(siblingGroup).isEqualTo(expectedInfo);
    }

    @Test
    void testSingleOrderWithDeliveredSiblingHasDeliveredAt() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        OffsetDateTime expectedDeliveredAt = OffsetDateTime.now(clock);
        setClockAt(expectedDeliveredAt);

        Order firstOrder = createAndReceiveOrder(pickupPoint);
        Order secondOrder = createAndReceiveOrder(pickupPoint);
        verifyAndDeliverOrder(firstOrder);

        SiblingGroupParams siblingGroup = getSiblingGroupByRecipient(secondOrder);
        SiblingGroupParams expectedInfo = new SiblingGroupParams(List.of(), expectedDeliveredAt);

        assertThat(siblingGroup).isEqualTo(expectedInfo);
    }

    @Test
    void testCancelledOrderDisbandsPairOfArrivedSiblings() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();

        Order firstOrder = createAndReceiveOrder(pickupPoint);
        Order secondOrder = createAndReceiveOrder(pickupPoint);
        orderFactory.cancelOrder(firstOrder.getId());

        SiblingGroupParams siblingGroup = getSiblingGroupByRecipient(secondOrder);

        assertThat(siblingGroup).isEqualTo(SiblingGroupParams.EMPTY);
    }

    @Test
    void testCancelledOrderShrinksGroupOfArrivedSiblings() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();

        Order firstOrder = createAndReceiveOrder(pickupPoint);
        Order secondOrder = createAndReceiveOrder(pickupPoint);
        Order thirdOrder = createAndReceiveOrder(pickupPoint);
        orderFactory.cancelOrder(firstOrder.getId());

        SiblingGroupParams siblingGroup = getSiblingGroupByRecipient(secondOrder);
        SiblingGroupParams expectedInfo = new SiblingGroupParams(List.of(thirdOrder.getExternalId()), null);

        assertThat(siblingGroup).isEqualTo(expectedInfo);
    }

    @Test
    void testCancelDeliveryEnlargesGroupOfSiblings() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();

        Order firstOrder = createAndReceiveOrder(pickupPoint);
        Order secondOrder = createAndReceiveOrder(pickupPoint);
        Order thirdOrder = createAndReceiveOrder(pickupPoint);
        verifyAndDeliverOrder(firstOrder);
        orderFactory.cancelDelivery(firstOrder.getId());

        SiblingGroupParams siblingGroup = getSiblingGroupByRecipient(thirdOrder);
        SiblingGroupParams expectedInfo = new SiblingGroupParams(
                List.of(secondOrder.getExternalId(), firstOrder.getExternalId()), null);

        assertThat(siblingGroup).isEqualTo(expectedInfo);
    }

    @Test
    void testCancelDeliveryRecreatesPairOfSiblings() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();

        Order firstOrder = createAndReceiveOrder(pickupPoint);
        Order secondOrder = createAndReceiveOrder(pickupPoint);
        verifyAndDeliverOrder(firstOrder);
        orderFactory.cancelDelivery(firstOrder.getId());

        SiblingGroupParams siblingGroup = getSiblingGroupByRecipient(secondOrder);
        SiblingGroupParams expectedInfo = new SiblingGroupParams(List.of(firstOrder.getExternalId()), null);

        assertThat(siblingGroup).isEqualTo(expectedInfo);
    }

    @Test
    void testCancelDeliveryRecreatesSingleSiblingAndRestoresDeliveredAt() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();

        Order firstOrder = createAndReceiveOrder(pickupPoint);
        Order secondOrder = createAndReceiveOrder(pickupPoint);

        setClockAt(OffsetDateTime.now().minus(10, ChronoUnit.MINUTES));
        verifyAndDeliverOrder(firstOrder);
        OffsetDateTime expectedDeliveredAt = OffsetDateTime.now(clock);
        setClockAt(expectedDeliveredAt);
        verifyAndDeliverOrder(secondOrder);

        orderFactory.cancelDelivery(firstOrder.getId());

        SiblingGroupParams siblingGroup = getSiblingGroupByRecipient(firstOrder);
        SiblingGroupParams expectedInfo = new SiblingGroupParams(List.of(), expectedDeliveredAt);

        assertThat(siblingGroup).isEqualTo(expectedInfo);
    }

    @Test
    void testCancelDeliveryRestoresGroupAndRevertsDeliveredAt() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();

        Order firstOrder = createAndReceiveOrder(pickupPoint);
        Order secondOrder = createAndReceiveOrder(pickupPoint);
        Order thirdOrder = createAndReceiveOrder(pickupPoint);

        setClockAt(OffsetDateTime.now().minus(10, ChronoUnit.MINUTES));
        verifyAndDeliverOrder(firstOrder);
        OffsetDateTime expectedDeliveredAt = OffsetDateTime.now(clock);
        setClockAt(expectedDeliveredAt);
        verifyAndDeliverOrder(secondOrder);

        orderFactory.cancelDelivery(firstOrder.getId());

        SiblingGroupParams siblingGroup = getSiblingGroupByRecipient(firstOrder);
        SiblingGroupParams expectedInfo = new SiblingGroupParams(List.of(thirdOrder.getExternalId()), expectedDeliveredAt);

        assertThat(siblingGroup).isEqualTo(expectedInfo);
    }

    private SiblingGroupParams getSiblingGroupByRecipient(Order order) {
        return siblingOrderQueryService.getSiblingGroupByRecipient(order.getId(), order.getRecipientPhone(),
                order.getBuyerYandexUid(), order.getIdOfPickupPoint());
    }

    private void setClockAt(OffsetDateTime dateTime) {
        clock.setFixed(dateTime.toInstant(), clock.getZone());
    }

    private Order createAndReceiveOrder(PickupPoint pickupPoint) {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());
        long orderId = order.getId();
        orderFactory.receiveOrder(orderId);
        return order;
    }

    private void verifyAndDeliverOrder(Order order) {
        order = orderFactory.verifyOrder(order.getId());
        orderCommandService.deliver(order, OrderDeliveryType.VERIFICATION_CODE, OrderPaymentType.PREPAID);
    }
}
