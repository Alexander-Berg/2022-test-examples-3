package ru.yandex.market.pvz.core.domain.order;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus;
import ru.yandex.market.pvz.core.domain.pickup_point.calendar.override.PickupPointCalendarOverrideParams;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.domain.order.OrderCommandService.DEFAULT_ON_DEMAND_STORAGE_PERIOD;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderExpirationDateServiceTest {

    private final TestableClock clock;

    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;

    private final OrderExpirationDateService orderExpirationDateService;

    @Test
    void getExpirationDateForUsualOrderForPickupPointWithoutHolidays() {
        var pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .storagePeriod(5)
                        .schedule(TestPickupPointFactory.PickupPointScheduleTestParams.builder()
                                .scheduleDays(List.of(
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.MONDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.TUESDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.WEDNESDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.THURSDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.FRIDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SATURDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SUNDAY)
                                                .build()
                                ))
                                .build())
                        .build());
        LocalDate date = LocalDate.of(2022, 5, 16); // Monday
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        clock.setFixed(OffsetDateTime.of(date, LocalTime.NOON, zone).toInstant(), zone);

        LocalDate actual = orderExpirationDateService.getExpirationDateForArrivedOrder(
                pickupPoint.getId(), pickupPoint.getStoragePeriod(), (int) DEFAULT_ON_DEMAND_STORAGE_PERIOD.toDays(),
                false, date);
        assertThat(actual).isEqualTo(LocalDate.of(2022, 5, 21));
    }

    @Test
    void getExpirationDateForUsualOrderForPickupPointWithHolidays() {
        var pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .storagePeriod(5)
                        .schedule(TestPickupPointFactory.PickupPointScheduleTestParams.builder()
                                .scheduleDays(List.of(
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.MONDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.TUESDAY)
                                                .isWorkingDay(false)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.WEDNESDAY)
                                                .isWorkingDay(false)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.THURSDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.FRIDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SATURDAY)
                                                .isWorkingDay(false)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SUNDAY)
                                                .isWorkingDay(false)
                                                .build()
                                ))
                                .build())
                        .build());
        LocalDate date = LocalDate.of(2022, 5, 16); // Monday
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        clock.setFixed(OffsetDateTime.of(date, LocalTime.NOON, zone).toInstant(), zone);

        LocalDate actual = orderExpirationDateService.getExpirationDateForArrivedOrder(
                pickupPoint.getId(), pickupPoint.getStoragePeriod(), (int) DEFAULT_ON_DEMAND_STORAGE_PERIOD.toDays(),
                false, date);
        assertThat(actual).isEqualTo(LocalDate.of(2022, 5, 27));
    }

    @Test
    void getExpirationDateForOnDemandOrderForPickupPointWithoutHolidays() {
        var pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .storagePeriod(5)
                        .schedule(TestPickupPointFactory.PickupPointScheduleTestParams.builder()
                                .scheduleDays(List.of(
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.MONDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.TUESDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.WEDNESDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.THURSDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.FRIDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SATURDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SUNDAY)
                                                .build()
                                ))
                                .build())
                        .build());
        LocalDate date = LocalDate.of(2022, 5, 16); // Monday
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        clock.setFixed(OffsetDateTime.of(date, LocalTime.NOON, zone).toInstant(), zone);

        LocalDate actual = orderExpirationDateService.getExpirationDateForArrivedOrder(
                pickupPoint.getId(), pickupPoint.getStoragePeriod(), (int) DEFAULT_ON_DEMAND_STORAGE_PERIOD.toDays(),
                true, date);
        assertThat(actual).isEqualTo(LocalDate.of(2022, 5, 20));
    }

    @Test
    void getExpirationDateForOnDemandOrderForPickupPointWithHolidays() {
        var pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .storagePeriod(5)
                        .schedule(TestPickupPointFactory.PickupPointScheduleTestParams.builder()
                                .scheduleDays(List.of(
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.MONDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.TUESDAY)
                                                .isWorkingDay(false)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.WEDNESDAY)
                                                .isWorkingDay(false)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.THURSDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.FRIDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SATURDAY)
                                                .isWorkingDay(false)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SUNDAY)
                                                .isWorkingDay(false)
                                                .build()
                                ))
                                .build())
                        .build());
        LocalDate date = LocalDate.of(2022, 5, 16); // Monday
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        clock.setFixed(OffsetDateTime.of(date, LocalTime.NOON, zone).toInstant(), zone);

        LocalDate actual = orderExpirationDateService.getExpirationDateForArrivedOrder(
                pickupPoint.getId(), pickupPoint.getStoragePeriod(), (int) DEFAULT_ON_DEMAND_STORAGE_PERIOD.toDays(),
                true, date);
        assertThat(actual).isEqualTo(LocalDate.of(2022, 5, 26));
    }

    @Test
    void getExpirationDateForUsualOrderForPickupPointWithMonthOverrides() {
        var pickupPoint = pickupPointFactory.createPickupPointFromCrm();

        LocalDate date = LocalDate.of(2022, 5, 16); // Monday
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        clock.setFixed(OffsetDateTime.of(date.minusMonths(1), LocalTime.NOON, zone).toInstant(), zone);

        List<PickupPointCalendarOverrideParams> overrides = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            overrides.add(PickupPointCalendarOverrideParams.builder()
                    .date(date.plusDays(i))
                    .isHoliday(true)
                    .build()
            );
        }
        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .storagePeriod(5)
                        .schedule(TestPickupPointFactory.PickupPointScheduleTestParams.builder()
                                .scheduleDays(List.of(
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.MONDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.TUESDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.WEDNESDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.THURSDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.FRIDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SATURDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SUNDAY)
                                                .build()
                                ))
                                .build())
                        .build());
        pickupPoint = pickupPointFactory.updateCalendarOverrides(pickupPoint.getId(), false, List.of(), overrides);

        clock.setFixed(OffsetDateTime.of(date, LocalTime.NOON, zone).toInstant(), zone);
        LocalDate actual = orderExpirationDateService.getExpirationDateForArrivedOrder(
                pickupPoint.getId(), pickupPoint.getStoragePeriod(), (int) DEFAULT_ON_DEMAND_STORAGE_PERIOD.toDays(),
                false, date);
        assertThat(actual).isEqualTo(LocalDate.of(2022, 6, 16));
    }

    @Test
    void getExpirationDatesWrongStatus() {
        var order = orderFactory.createOrder();

        var dates = orderExpirationDateService.getPossibleFutureExpirationDates(order.getExternalId(),
                order.getIdOfPickupPoint());
        assertThat(dates).isEmpty();
    }

    @Test
    void getExpirationDatesStoragePeriodExtended() {
        var order = orderFactory.createOrder();
        orderFactory.setStatusAndCheckpoint(order.getId(), PvzOrderStatus.STORAGE_PERIOD_EXTENDED);
        order = orderFactory.setStatusAndCheckpoint(order.getId(), PvzOrderStatus.ARRIVED_TO_PICKUP_POINT);

        var dates = orderExpirationDateService.getPossibleFutureExpirationDates(order.getExternalId(),
                order.getIdOfPickupPoint());
        assertThat(dates).isEmpty();
    }

    @Test
    void getExpirationDates() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var expirationDate = LocalDate.of(2021, 8, 2);
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .expirationDate(expirationDate)
                        .build())
                .build());
        orderFactory.setStatusAndCheckpoint(order.getId(), PvzOrderStatus.ARRIVED_TO_PICKUP_POINT);

        var dates = orderExpirationDateService.getPossibleFutureExpirationDates(order.getExternalId(),
                pickupPoint.getId());
        var expected = new ArrayList<>();
        for (int days = 1; days <= 7; days++) {
            expected.add(expirationDate.plusDays(days));
        }

        assertThat(dates).isEqualTo(expected);
    }
}
