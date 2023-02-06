package ru.yandex.market.pvz.tms.executor.pickup_point.capacity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.tms.executor.pickup_point.capacity.CreatePickupPointCapacityRowsExecutor.HOW_MANY_DAYS_CREATE;

@TransactionlessEmbeddedDbTest
@Import({CreatePickupPointCapacityRowsExecutor.class})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CreatePickupPointCapacityRowsExecutorTest {

    private final TestableClock clock;
    private final CreatePickupPointCapacityRowsExecutor executor;
    private final TestPickupPointFactory pickupPointFactory;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TestOrderFactory orderFactory;
    private final DbQueueTestUtil dbQueueTestUtil;

    @Test
    void whenPickupPointWithoutRowsInPickupPointCapacity() throws Exception {
        clock.setFixed(Instant.now().truncatedTo(ChronoUnit.SECONDS), ZoneId.of("UTC+3"));

        var pickupPoint = createPickupPoint();
        var pickupPoint2 = createPickupPoint();

        Long rowsInDB = countRowsInDB(
                pickupPoint, LocalDate.now(clock), LocalDate.now(clock).plusDays(HOW_MANY_DAYS_CREATE)
        );
        assertThat(rowsInDB).isEqualTo(0);
        rowsInDB = countRowsInDB(
                pickupPoint2, LocalDate.now(clock), LocalDate.now(clock).plusDays(HOW_MANY_DAYS_CREATE)
        );
        assertThat(rowsInDB).isEqualTo(0);

        executor.doRealJob(null);
        rowsInDB = countRowsInDB(
                pickupPoint, LocalDate.now(clock), LocalDate.now(clock).plusDays(HOW_MANY_DAYS_CREATE)
        );
        assertThat(rowsInDB).isEqualTo(HOW_MANY_DAYS_CREATE + 1);
        rowsInDB = countRowsInDB(
                pickupPoint2, LocalDate.now(clock), LocalDate.now(clock).plusDays(HOW_MANY_DAYS_CREATE)
        );
        assertThat(rowsInDB).isEqualTo(HOW_MANY_DAYS_CREATE + 1);

        int daysPassed = 2;
        clock.setFixed(Instant.now().plus(daysPassed, ChronoUnit.DAYS), clock.getZone());

        executor.doRealJob(null);
        rowsInDB = countRowsInDB(
                pickupPoint,
                LocalDate.now(clock).minusDays(daysPassed),
                LocalDate.now(clock).plusDays(HOW_MANY_DAYS_CREATE)
        );
        assertThat(rowsInDB).isEqualTo(HOW_MANY_DAYS_CREATE + 3);
        rowsInDB = countRowsInDB(
                pickupPoint2,
                LocalDate.now(clock).minusDays(daysPassed),
                LocalDate.now(clock).plusDays(HOW_MANY_DAYS_CREATE)
        );
        assertThat(rowsInDB).isEqualTo(HOW_MANY_DAYS_CREATE + 3);
    }

    @Test
    void whenPickupPointWithRowsInPickupPointCapacity() throws Exception {
        clock.setFixed(Instant.now().truncatedTo(ChronoUnit.SECONDS), ZoneId.of("UTC+3"));

        var pickupPoint = createPickupPoint();
        var pickupPoint2 = createPickupPoint();
        createOrderWithDeliveryDate(pickupPoint, LocalDate.now(clock).plusDays(1));
        createOrderWithDeliveryDate(pickupPoint2, LocalDate.now(clock).plusDays(1));

        dbQueueTestUtil.executeAllQueueItems(PvzQueueType.CHANGE_PICKUP_POINT_CAPACITY);

        Long rowsInDB = countRowsInDB(
                pickupPoint, LocalDate.now(clock), LocalDate.now(clock).plusDays(HOW_MANY_DAYS_CREATE)
        );
        assertThat(rowsInDB).isEqualTo(pickupPoint.getCapacity() + 1);
        rowsInDB = countRowsInDB(
                pickupPoint2, LocalDate.now(clock), LocalDate.now(clock).plusDays(HOW_MANY_DAYS_CREATE)
        );
        assertThat(rowsInDB).isEqualTo(pickupPoint2.getCapacity() + 1);

        executor.doRealJob(null);

        rowsInDB = countRowsInDB(
                pickupPoint, LocalDate.now(clock), LocalDate.now(clock).plusDays(HOW_MANY_DAYS_CREATE)
        );
        assertThat(rowsInDB).isEqualTo(HOW_MANY_DAYS_CREATE + 1);
        rowsInDB = countRowsInDB(
                pickupPoint2, LocalDate.now(clock), LocalDate.now(clock).plusDays(HOW_MANY_DAYS_CREATE)
        );
        assertThat(rowsInDB).isEqualTo(HOW_MANY_DAYS_CREATE + 1);
    }

    private PickupPoint createPickupPoint() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        return pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .active(true)
                        .capacity(7)
                        .build());
    }

    private void createOrderWithDeliveryDate(PickupPoint pickupPoint, LocalDate deliveryDate) {
        orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .deliveryDate(deliveryDate)
                        .build())
                .build());
    }

    private Long countRowsInDB(PickupPoint pickupPoint, LocalDate start, LocalDate end) {
        String sql = "" +
                "SELECT COUNT(*) " +
                "FROM pickup_point_capacity " +
                "WHERE pickup_point_id = :pickupPointId " +
                "   AND date BETWEEN :start AND :end ";
        return jdbcTemplate.queryForObject(
                sql,
                Map.of(
                        "pickupPointId", pickupPoint.getId(),
                        "start", start,
                        "end", end),
                Long.class
        );
    }

}
