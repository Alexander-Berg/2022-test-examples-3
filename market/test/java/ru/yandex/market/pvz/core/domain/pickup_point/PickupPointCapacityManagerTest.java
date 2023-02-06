package ru.yandex.market.pvz.core.domain.pickup_point;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType;
import ru.yandex.market.pvz.core.domain.order.OrderCommandService;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.NEW_CAPACITY_CALCULATION_ENABLED;

@Log4j2
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PickupPointCapacityManagerTest {

    private static final int CAPACITY = 10;
    private static final int STORAGE_PERIOD = 3;

    private final TestableClock clock;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;

    private final PickupPointRepository pickupPointRepository;

    private final PickupPointCapacityManager capacityManager;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final OrderCommandService orderCommandService;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    private PickupPoint pickupPoint;

    @BeforeEach
    void setup() {
        pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .capacity(CAPACITY)
                        .storagePeriod(STORAGE_PERIOD)
                        .build());

        configurationGlobalCommandService.setValue(NEW_CAPACITY_CALCULATION_ENABLED, true);

        clock.setFixed(Instant.parse("2021-01-15T12:00:00Z"), ZoneId.systemDefault());
    }

    @Test
    void testCapacityForEmptyPickupPoint() {
        assertThat(capacityManager.calculateCapacityForPeriod(pickupPoint, LocalDate.now(clock), 1)
                .get(LocalDate.now(clock)))
                .isEqualTo(pickupPoint.getCapacity());
    }

    @Test
    void testSingleOrderCapacityBorders() {
        Order order = createOrderByDeliveryDate(LocalDate.now(clock));

        dbQueueTestUtil.executeAllQueueItems(PvzQueueType.CHANGE_PICKUP_POINT_CAPACITY);

        assertCapacityForDate(order.getDeliveryDate().minusDays(1), CAPACITY);
        assertCapacityForDate(order.getDeliveryDate(), CAPACITY - 1);
        assertCapacityForDate(order.getDeliveryDate().plusDays(1), CAPACITY - 1);
        assertCapacityForDate(order.getDeliveryDate().plusDays(2), CAPACITY - 1);
        assertCapacityForDate(order.getDeliveryDate().plusDays(3), CAPACITY - 1);
        assertCapacityForDate(order.getDeliveryDate().plusDays(4), CAPACITY);
    }

    @Test
    void testManyOrdersCapacityBorders() {
        LocalDate startDate = LocalDate.now(clock);

        createOrderByDeliveryDate(startDate);
        createOrderByDeliveryDate(startDate.plusDays(1));
        createOrderByDeliveryDate(startDate.plusDays(2));

        dbQueueTestUtil.executeAllQueueItems(PvzQueueType.CHANGE_PICKUP_POINT_CAPACITY);

        assertCapacityForDate(startDate.minusDays(1), CAPACITY);
        assertCapacityForDate(startDate, CAPACITY - 1);
        assertCapacityForDate(startDate.plusDays(1), CAPACITY - 2);
        assertCapacityForDate(startDate.plusDays(2), CAPACITY - 3);
        assertCapacityForDate(startDate.plusDays(3), CAPACITY - 3);
        assertCapacityForDate(startDate.plusDays(4), CAPACITY - 2);
        assertCapacityForDate(startDate.plusDays(5), CAPACITY - 1);
        assertCapacityForDate(startDate.plusDays(6), CAPACITY);
    }

    @Test
    void testCapacityIsNotNegative() {
        LocalDate date = LocalDate.now(clock);

        for (int i = 0; i < CAPACITY + 1; i++) {
            createOrderByDeliveryDate(date);
        }

        dbQueueTestUtil.executeAllQueueItems(PvzQueueType.CHANGE_PICKUP_POINT_CAPACITY);

        assertCapacityForDate(date.minusDays(1), CAPACITY);
        assertCapacityForDate(date, 0);
        assertCapacityForDate(date.plusDays(1), 0);
        assertCapacityForDate(date.plusDays(2), 0);
        assertCapacityForDate(date.plusDays(3), 0);
        assertCapacityForDate(date.plusDays(4), CAPACITY);
    }

    @Test
    void testOrdersWithCorrectStatusesTakesUpSpace() {
        LocalDate date = LocalDate.now(clock);

        createOrderByDeliveryDate(date);

        Order order = createOrderByDeliveryDate(date);
        orderFactory.receiveOrder(order.getId());

        order = createOrderByDeliveryDate(date);
        orderFactory.updateStatus(order.getId(), PvzOrderStatus.DELIVERY_DATE_UPDATED_BY_DELIVERY);

        order = createOrderByDeliveryDate(date);
        orderFactory.updateStatus(order.getId(), PvzOrderStatus.STORAGE_PERIOD_EXTENDED);

        dbQueueTestUtil.executeAllQueueItems(PvzQueueType.CHANGE_PICKUP_POINT_CAPACITY);

        assertCapacityForDate(date.minusDays(1), CAPACITY);
        assertCapacityForDate(date, CAPACITY - 4);
        assertCapacityForDate(date.plusDays(1), CAPACITY - 4);
        assertCapacityForDate(date.plusDays(2), CAPACITY - 4);
        assertCapacityForDate(date.plusDays(3), CAPACITY - 4);
        assertCapacityForDate(date.plusDays(4), CAPACITY);
    }

    private Order createOrderByDeliveryDate(LocalDate date) {
        return orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .deliveryDate(date)
                        .build())
                .build());
    }

    @Test
    void whenStatusFromCreatedToArrivedToPickupPointToCreated() {
        LocalDate date = LocalDate.now(clock);

        Order order = createOrderByDeliveryDate(date);
        order = orderFactory.receiveOrder(order.getId());
        orderCommandService.revertShipment(order.getExternalId(), order.getIdOfPickupPoint());

        dbQueueTestUtil.executeAllQueueItems(PvzQueueType.CHANGE_PICKUP_POINT_CAPACITY);

        assertCapacityForDate(date.minusDays(1), CAPACITY);
        assertCapacityForDate(date, CAPACITY - 1);
        assertCapacityForDate(date.plusDays(1), CAPACITY - 1);
        assertCapacityForDate(date.plusDays(2), CAPACITY - 1);
        assertCapacityForDate(date.plusDays(3), CAPACITY - 1);
        assertCapacityForDate(date.plusDays(4), CAPACITY);
    }

    @Test
    void testOrdersWithIncorrectStatusesNotTakingUpSpace() {
        LocalDate date = LocalDate.now(clock);
        var statuses = Stream.of(PvzOrderStatus.values())
                .filter(s -> !PvzOrderStatus.AWAITING_DELIVERY_OR_TRANSMISSION.contains(s))
                .collect(Collectors.toList());

        for (var status : statuses) {
            Order order = createOrderByDeliveryDate(date);

            orderFactory.updateStatus(order.getId(), status);
        }
        dbQueueTestUtil.executeAllQueueItems(PvzQueueType.CHANGE_PICKUP_POINT_CAPACITY);

        assertCapacityForDate(date, CAPACITY);
    }

    @Test
    void whenTransmittedToRecipient() {
        LocalDate date = LocalDate.now(clock);

        Order order = createOrderByDeliveryDate(date);
        order = orderFactory.receiveOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.UNKNOWN, OrderPaymentType.CARD);

        dbQueueTestUtil.executeAllQueueItems(PvzQueueType.CHANGE_PICKUP_POINT_CAPACITY);

        assertCapacityForDate(date.minusDays(1), CAPACITY);
        assertCapacityForDate(date, CAPACITY);
        assertCapacityForDate(date.plusDays(1), CAPACITY);
    }

    @Test
    void whenStatusFromCreatedToReadyForReturn() {
        LocalDate date = LocalDate.now(clock);

        Order order = createOrderByDeliveryDate(date);
        orderFactory.cancelOrder(order.getId());
        order = orderFactory.receiveOrder(order.getId());
        assertThat(order.getStatus()).isEqualTo(PvzOrderStatus.READY_FOR_RETURN);

        dbQueueTestUtil.executeAllQueueItems(PvzQueueType.CHANGE_PICKUP_POINT_CAPACITY);

        assertCapacityForDate(date.minusDays(1), CAPACITY);
        assertCapacityForDate(date, CAPACITY);
        assertCapacityForDate(date.plusDays(1), CAPACITY);
    }

    @Test
    void whenStatusFromTransmittedToRecipientToArrivedToPickupPoint() {
        LocalDate date = LocalDate.now(clock);

        Order order = createOrderByDeliveryDate(date);
        order = orderFactory.receiveOrder(order.getId());
        orderFactory.deliverOrder(order.getId(), OrderDeliveryType.UNKNOWN, OrderPaymentType.CARD);
        orderFactory.updateStatus(order.getId(), PvzOrderStatus.ARRIVED_TO_PICKUP_POINT);

        dbQueueTestUtil.executeAllQueueItems(PvzQueueType.CHANGE_PICKUP_POINT_CAPACITY);

        assertCapacityForDate(date.minusDays(1), CAPACITY);
        assertCapacityForDate(date, CAPACITY - 1);
        assertCapacityForDate(date.plusDays(1), CAPACITY - 1);
        assertCapacityForDate(date.plusDays(2), CAPACITY - 1);
        assertCapacityForDate(date.plusDays(3), CAPACITY - 1);
        assertCapacityForDate(date.plusDays(4), CAPACITY);
    }

    @Test
    void whenStoragePeriodExtended() {
        LocalDate date = LocalDate.now(clock);

        Order order = createOrderByDeliveryDate(date);
        order = orderFactory.receiveOrder(order.getId());
        orderCommandService.extendStoragePeriod(order.getExternalId(), order.getIdOfPickupPoint(), date.plusDays(5));

        dbQueueTestUtil.executeAllQueueItems(PvzQueueType.CHANGE_PICKUP_POINT_CAPACITY);

        assertCapacityForDate(date.minusDays(1), CAPACITY);
        assertCapacityForDate(date, CAPACITY - 1);
        assertCapacityForDate(date.plusDays(1), CAPACITY - 1);
        assertCapacityForDate(date.plusDays(2), CAPACITY - 1);
        assertCapacityForDate(date.plusDays(3), CAPACITY - 1);
        assertCapacityForDate(date.plusDays(4), CAPACITY - 1);
        assertCapacityForDate(date.plusDays(5), CAPACITY - 1);
        assertCapacityForDate(date.plusDays(6), CAPACITY);
    }

    @Test
    void whenStatusFromStoragePeriodExtendedToArrivedToPickupPoint() {
        LocalDate date = LocalDate.now(clock);

        Order order = createOrderByDeliveryDate(date);
        order = orderFactory.receiveOrder(order.getId());
        orderCommandService.extendStoragePeriod(order.getExternalId(), order.getIdOfPickupPoint(), date.plusDays(5));
        orderFactory.updateStatus(order.getId(), PvzOrderStatus.ARRIVED_TO_PICKUP_POINT);

        dbQueueTestUtil.executeAllQueueItems(PvzQueueType.CHANGE_PICKUP_POINT_CAPACITY);

        assertCapacityForDate(date.minusDays(1), CAPACITY);
        assertCapacityForDate(date, CAPACITY - 1);
        assertCapacityForDate(date.plusDays(1), CAPACITY - 1);
        assertCapacityForDate(date.plusDays(2), CAPACITY - 1);
        assertCapacityForDate(date.plusDays(3), CAPACITY - 1);
        assertCapacityForDate(date.plusDays(4), CAPACITY - 1);
        assertCapacityForDate(date.plusDays(5), CAPACITY - 1);
        assertCapacityForDate(date.plusDays(6), CAPACITY);
    }

    @Test
    void testNullCapacity() {
        pickupPoint.setCapacity(null);
        pickupPointRepository.save(pickupPoint);

        assertCapacityForDate(LocalDate.now(clock), Integer.MAX_VALUE);
    }

    private void assertCapacityForDate(LocalDate date, int capacity) {
        assertThat(capacityManager.calculateCapacityForPeriod(pickupPoint, date, 1).get(date))
                .isEqualTo(capacity);
    }

}
