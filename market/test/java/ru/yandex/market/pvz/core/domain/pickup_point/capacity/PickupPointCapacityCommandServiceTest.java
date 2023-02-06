package ru.yandex.market.pvz.core.domain.pickup_point.capacity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus;
import ru.yandex.market.pvz.core.domain.order.model.params.OrderUpdateCapacityParams;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

import static ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType.CHANGE_PICKUP_POINT_CAPACITY;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.CREATED;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PickupPointCapacityCommandServiceTest {

    private static final int CAPACITY = 10;
    private static final int STORAGE_PERIOD = 5;
    private static final Long ORDER_ID = 5L;

    private final TestPickupPointFactory pickupPointFactory;
    private final PickupPointCapacityCommandService capacityCommandService;
    private final TestableClock clock;
    private final DbQueueTestUtil dbQueueTestUtil;

    private PickupPoint pickupPoint;

    @BeforeEach
    public void setup() {
        clock.setFixed(Instant.now(), ZoneId.systemDefault());
        pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .capacity(CAPACITY)
                        .storagePeriod(STORAGE_PERIOD)
                        .build());
    }

    @Test
    void whenCapacityChangedFalseThenQueueEmpty() {
        //флаг capacityChanged не выставлен
        capacityCommandService.checkCapacityChanged(
                OrderUpdateCapacityParams.builder()
                        .capacityChanged(false)
                        .pickupPointId(pickupPoint.getId())
                        .deliveryDate(LocalDate.now(clock).plusDays(3))
                        .pickupPointStoragePeriod(STORAGE_PERIOD)
                        .status(CREATED)
                        .build()
        );
        dbQueueTestUtil.isEmpty(CHANGE_PICKUP_POINT_CAPACITY);
    }

    @Test
    void whenOrderStatusesIsNotAtPickupPointThenQueueEmpty() {
        //перешли из статуса не занимающего места на пвз в такой же статус
        capacityCommandService.checkCapacityChanged(
                OrderUpdateCapacityParams.builder()
                        .capacityChanged(true)
                        .pickupPointId(pickupPoint.getId())
                        .expirationDate(LocalDate.now(clock).plusDays(7))
                        .pickupPointStoragePeriod(STORAGE_PERIOD)
                        .status(PvzOrderStatus.STORAGE_PERIOD_EXPIRED)
                        .statusOld(PvzOrderStatus.TRANSMITTED_TO_RECIPIENT)
                        .build()
        );
        dbQueueTestUtil.isEmpty(CHANGE_PICKUP_POINT_CAPACITY);
    }

    @Test
    void whenInvalidDataThenQueueEmpty() {
        capacityCommandService.checkCapacityChanged(
                OrderUpdateCapacityParams.builder().build()
        );
        dbQueueTestUtil.isEmpty(CHANGE_PICKUP_POINT_CAPACITY);
    }

    @Test
    void whenNewStatusCreated() {
        // создание заказа
        capacityCommandService.checkCapacityChanged(
                OrderUpdateCapacityParams.builder()
                        .capacityChanged(true)
                        .pickupPointId(pickupPoint.getId())
                        .expirationDate(null)
                        .expirationDateOld(null)
                        .pickupPointStoragePeriod(STORAGE_PERIOD)
                        .status(PvzOrderStatus.CREATED)
                        .statusOld(null)
                        .id(ORDER_ID)
                        .deliveryDate(LocalDate.now(clock).plusDays(2))
                        .deliveryDateOld(null)
                        .build()
        );
        dbQueueTestUtil.assertQueueHasSize(CHANGE_PICKUP_POINT_CAPACITY, 1);
    }
}
