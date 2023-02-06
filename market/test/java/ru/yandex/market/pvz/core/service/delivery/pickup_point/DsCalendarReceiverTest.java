package ru.yandex.market.pvz.core.service.delivery.pickup_point;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistic.api.model.delivery.DateBool;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestCourierDsDayOffFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointCourierMappingFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.NEW_CAPACITY_CALCULATION_ENABLED;
import static ru.yandex.market.pvz.core.domain.pickup_point.calendar.override.PickupPointCalendarManager.CALENDAR_GENERATION_PERIOD_IN_DAYS;
import static ru.yandex.market.pvz.core.service.delivery.pickup_point.DsCalendarReceiver.DEFAULT_CALENDAR_DAY_TIME;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointCourierMappingFactory.PickupPointCourierMappingTestParams.DEFAULT_COURIER_DELIVERY_SERVICE_ID;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleDayTestParams.DEFAULT_TIME_FROM;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class DsCalendarReceiverTest {

    private final TestPickupPointFactory pickupPointFactory;
    private final TestPickupPointCourierMappingFactory pickupPointCourierMappingFactory;
    private final TestCourierDsDayOffFactory courierDsDayOffFactory;
    private final TestOrderFactory orderFactory;

    private final TestableClock clock;
    private final DsCalendarReceiver calendarReceiver;

    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    @Test
    void getCalendarForTwoWeeks() {
        PickupPoint pickupPoint = createPickupPoint();

        // this is impossible to do via api, but need for test if rows have been inserted manually
        pickupPoint.getSchedule().getScheduleDays().remove(2);

        //start calculating from Friday
        LocalDateTime current = LocalDateTime.of(2020, 9, 4, 8, 0, 0);
        ZoneOffset zone = ZoneOffset.ofHours(3);
        clock.setFixed(current.toInstant(zone), zone);

        List<DateBool> actual = calendarReceiver.getCalendar(pickupPoint);

        List<DateBool> expected = List.of(
                buildDateBool(2020, 9, 4, DEFAULT_TIME_FROM, pickupPoint, true),
                buildDateBool(2020, 9, 5, DEFAULT_TIME_FROM, pickupPoint, false),
                buildDateBool(2020, 9, 6, DEFAULT_TIME_FROM, pickupPoint, false),
                buildDateBool(2020, 9, 7, DEFAULT_TIME_FROM, pickupPoint, true),
                buildDateBool(2020, 9, 8, LocalTime.of(7, 30), pickupPoint, true),
                buildDateBool(2020, 9, 9, DEFAULT_CALENDAR_DAY_TIME, pickupPoint, false),
                buildDateBool(2020, 9, 10, DEFAULT_TIME_FROM, pickupPoint, true),
                buildDateBool(2020, 9, 11, DEFAULT_TIME_FROM, pickupPoint, true),
                buildDateBool(2020, 9, 12, DEFAULT_TIME_FROM, pickupPoint, false),
                buildDateBool(2020, 9, 13, DEFAULT_TIME_FROM, pickupPoint, false),
                buildDateBool(2020, 9, 14, DEFAULT_TIME_FROM, pickupPoint, true),
                buildDateBool(2020, 9, 15, LocalTime.of(7, 30), pickupPoint, true),
                buildDateBool(2020, 9, 16, DEFAULT_CALENDAR_DAY_TIME, pickupPoint, false),
                buildDateBool(2020, 9, 17, DEFAULT_TIME_FROM, pickupPoint, true)
        );

        assertThat(actual).containsAll(expected);
    }

    @Test
    void checkFirstAndLastCalendarDays() {
        PickupPoint pickupPoint = createPickupPoint();

        //start calculating from Saturday
        LocalDateTime current = LocalDateTime.of(2020, 8, 1, 8, 0, 0);
        ZoneOffset zone = ZoneOffset.ofHours(3);
        clock.setFixed(current.toInstant(zone), zone);

        List<DateBool> actual = calendarReceiver.getCalendar(pickupPoint);

        assertThat(actual).hasSize(CALENDAR_GENERATION_PERIOD_IN_DAYS + 1);

        List<DateBool> expected = List.of(
                buildDateBool(2020, 8, 1, DEFAULT_TIME_FROM, pickupPoint, false),
                buildDateBool(2020, 8, 31, DEFAULT_TIME_FROM, pickupPoint, true)
        );

        assertThat(actual).containsAll(expected);

        List<DateBool> notIncluded = List.of(
                buildDateBool(2020, 7, 31, DEFAULT_TIME_FROM, pickupPoint, true),
                buildDateBool(2020, 9, 1, DEFAULT_TIME_FROM, pickupPoint, true)
        );

        assertThat(actual).doesNotContainAnyElementsOf(notIncluded);
    }

    @Test
    void getCalendarForTwoWeekWithCapacity() {
        configurationGlobalCommandService.setValue(NEW_CAPACITY_CALCULATION_ENABLED, true);
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        pickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .capacity(1)
                        .storagePeriod(3)
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
                                                .isWorkingDay(false)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SUNDAY)
                                                .isWorkingDay(false)
                                                .build()
                                ))
                                .build())
                        .build()
        );

        //start calculating from Monday
        LocalDateTime current = LocalDateTime.of(2019, 4, 1, 8, 0, 0);
        ZoneOffset zone = ZoneOffset.ofHours(3);

        clock.setFixed(current.toInstant(zone), zone);

        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .deliveryDate(LocalDate.of(2019, 4, 2))
                        .build())
                .build());
        orderFactory.receiveOrder(order.getId());

        List<DateBool> actual = calendarReceiver.getCalendar(pickupPoint);

        List<DateBool> expected = List.of(
                buildDateBool(2019, 4, 1, DEFAULT_TIME_FROM, pickupPoint, false),
                buildDateBool(2019, 4, 2, DEFAULT_TIME_FROM, pickupPoint, false),
                buildDateBool(2019, 4, 3, DEFAULT_TIME_FROM, pickupPoint, false),
                buildDateBool(2019, 4, 4, DEFAULT_TIME_FROM, pickupPoint, false),
                buildDateBool(2019, 4, 5, DEFAULT_TIME_FROM, pickupPoint, true),
                buildDateBool(2019, 4, 6, DEFAULT_TIME_FROM, pickupPoint, false),
                buildDateBool(2019, 4, 7, DEFAULT_TIME_FROM, pickupPoint, false)
        );

        assertThat(actual).containsAll(expected);
    }

    @Test
    void getCalendarForTwoWeeksWithDayOffs() {
        PickupPoint pickupPoint = createPickupPoint();

        //start calculating from Friday
        LocalDateTime current = LocalDateTime.of(2020, 9, 4, 8, 0, 0);
        ZoneOffset zone = ZoneOffset.ofHours(3);
        clock.setFixed(current.toInstant(zone), zone);

        LocalDate sinceDate = current.toLocalDate();

        pickupPointCourierMappingFactory.create(
                TestPickupPointCourierMappingFactory.PickupPointCourierMappingTestParamsBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .build());

        // 2020-09-05 day off
        courierDsDayOffFactory.create(TestCourierDsDayOffFactory.CourierDsDayOffParams.builder()
                .courierDeliveryServiceId(DEFAULT_COURIER_DELIVERY_SERVICE_ID)
                .dayOff(sinceDate.plusDays(1))
                .build());

        // 2020-09-06 day off
        courierDsDayOffFactory.create(TestCourierDsDayOffFactory.CourierDsDayOffParams.builder()
                .courierDeliveryServiceId(DEFAULT_COURIER_DELIVERY_SERVICE_ID)
                .dayOff(sinceDate.plusDays(2))
                .build());

        // 2020-09-09 day off
        courierDsDayOffFactory.create(TestCourierDsDayOffFactory.CourierDsDayOffParams.builder()
                .courierDeliveryServiceId(DEFAULT_COURIER_DELIVERY_SERVICE_ID)
                .dayOff(sinceDate.plusDays(5))
                .build());

        // 2020-09-11 day off
        courierDsDayOffFactory.create(TestCourierDsDayOffFactory.CourierDsDayOffParams.builder()
                .courierDeliveryServiceId(DEFAULT_COURIER_DELIVERY_SERVICE_ID)
                .dayOff(sinceDate.plusDays(7))
                .build());

        // 2020-09-14 day off
        courierDsDayOffFactory.create(TestCourierDsDayOffFactory.CourierDsDayOffParams.builder()
                .courierDeliveryServiceId(DEFAULT_COURIER_DELIVERY_SERVICE_ID)
                .dayOff(sinceDate.plusDays(10))
                .build());

        List<DateBool> actual = calendarReceiver.getCalendar(pickupPoint);

        List<DateBool> expected = List.of(
                buildDateBool(2020, 9, 4, DEFAULT_TIME_FROM, pickupPoint, true),
                buildDateBool(2020, 9, 5, DEFAULT_TIME_FROM, pickupPoint, false),
                buildDateBool(2020, 9, 6, DEFAULT_TIME_FROM, pickupPoint, false),
                buildDateBool(2020, 9, 7, DEFAULT_TIME_FROM, pickupPoint, true),
                buildDateBool(2020, 9, 8, LocalTime.of(7, 30), pickupPoint, true),
                buildDateBool(2020, 9, 9, DEFAULT_TIME_FROM, pickupPoint, false),
                buildDateBool(2020, 9, 10, DEFAULT_TIME_FROM, pickupPoint, true),
                buildDateBool(2020, 9, 11, DEFAULT_TIME_FROM, pickupPoint, false),
                buildDateBool(2020, 9, 12, DEFAULT_TIME_FROM, pickupPoint, false),
                buildDateBool(2020, 9, 13, DEFAULT_TIME_FROM, pickupPoint, false),
                buildDateBool(2020, 9, 14, DEFAULT_TIME_FROM, pickupPoint, false),
                buildDateBool(2020, 9, 15, LocalTime.of(7, 30), pickupPoint, true),
                buildDateBool(2020, 9, 16, DEFAULT_TIME_FROM, pickupPoint, true),
                buildDateBool(2020, 9, 17, DEFAULT_TIME_FROM, pickupPoint, true)
        );

        assertThat(actual).containsAll(expected);
    }

    private PickupPoint createPickupPoint() {
        return pickupPointFactory.createPickupPoint(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .schedule(TestPickupPointFactory.PickupPointScheduleTestParams.builder()
                                        .scheduleDays(List.of(
                                                TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                        .dayOfWeek(DayOfWeek.MONDAY)
                                                        .build(),
                                                TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                        .dayOfWeek(DayOfWeek.TUESDAY)
                                                        .timeFrom(LocalTime.of(7, 30))
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
                                                        .isWorkingDay(false)
                                                        .build(),
                                                TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                        .dayOfWeek(DayOfWeek.SUNDAY)
                                                        .isWorkingDay(false)
                                                        .build()
                                        ))
                                        .build())
                                .build())
                        .build()
        );
    }

    private DateBool buildDateBool(
            int year, int month, int dayOfMonth, LocalTime timeFrom, PickupPoint pickupPoint, boolean flag
    ) {
        return new DateBool(DateTime.fromOffsetDateTime(
                OffsetDateTime.of(
                        LocalDateTime.of(LocalDate.of(year, month, dayOfMonth), timeFrom),
                        ZoneOffset.ofHours(pickupPoint.getTimeOffset()))), flag);
    }
}
