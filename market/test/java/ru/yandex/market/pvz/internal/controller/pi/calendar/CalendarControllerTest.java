package ru.yandex.market.pvz.internal.controller.pi.calendar;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.test.factory.TestBrandRegionFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.DISABLE_DELIVERY_DATES_PICKUP_POINT_CALENDAR_CHECK;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.UNEDITABLE_CALENDAR_DAYS_COUNT;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CalendarControllerTest extends BaseShallowTest {

    private static final Instant NOW = Instant.parse("2021-01-01T12:00:00Z");

    private final TestableClock clock;

    private final TestBrandRegionFactory brandRegionFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    @BeforeEach
    public void setup() {
        configurationGlobalCommandService.setValue(UNEDITABLE_CALENDAR_DAYS_COUNT, 7);
        clock.setFixed(NOW, ZoneOffset.ofHours(PickupPoint.DEFAULT_TIME_OFFSET));
    }

    @Test
    @Disabled
    void getCalendar() throws Exception {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                .builder()
                .params(TestPickupPointFactory.PickupPointTestParams.builder()
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
                        .build())
                .build());
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        OffsetDateTime dateTime = OffsetDateTime.of(LocalDateTime.of(2021, 2, 7, 13, 0), zone);
        clock.setFixed(dateTime.toInstant(), zone);
        //TODO: add official holidays

        mockMvc.perform(
                get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/calendar")
                        .param("date_from", "2021-02-15")
                        .param("date_to", "2021-02-21")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent(
                        "pickup_point/calendar/response_get_calendar.json"), true));
    }

    @Test
    void getCalendarForFullBrand() throws Exception {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();

        brandRegionFactory.createDefaults();

        pickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(), TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
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
                        .build());
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        OffsetDateTime dateTime = OffsetDateTime.of(LocalDateTime.of(2021, 2, 8, 13, 0), zone);
        clock.setFixed(dateTime.toInstant(), zone);

        mockMvc.perform(
                get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/calendar")
                        .param("date_from", "2021-02-15")
                        .param("date_to", "2021-02-21")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent(
                        "pickup_point/calendar/response_get_calendar_full_brand.json"), true));
    }

    @Test
    @Disabled
    void updateCalendar() throws Exception {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                .builder()
                .params(TestPickupPointFactory.PickupPointTestParams.builder()
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
                        .build())
                .build());
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        OffsetDateTime dateTime = OffsetDateTime.of(LocalDateTime.of(2021, 2, 6, 13, 0), zone);
        clock.setFixed(dateTime.toInstant(), zone);

        mockMvc.perform(
                patch("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/calendar")
                        .content(getFileContent("pickup_point/calendar/request_update_override_days.json"))
                        .param("date_from", "2021-02-15")
                        .param("date_to", "2021-02-21")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent(
                        "pickup_point/calendar/response_update_override_days.json"), true));
    }

    @Test
    void adminUpdateCalendar() throws Exception {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                .builder()
                .params(TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
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
                        .build())
                .build());
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        OffsetDateTime dateTime = OffsetDateTime.of(LocalDateTime.of(2021, 2, 8, 13, 0), zone);
        clock.setFixed(dateTime.toInstant(), zone);

        createOrderWithDeliveryDate(pickupPoint, LocalDate.parse("2021-02-17"));

        mockMvc.perform(
                patch("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/calendar/admin")
                        .content(getFileContent("pickup_point/calendar/request_admin_update_override_days.json"))
                        .param("date_from", "2021-02-15")
                        .param("date_to", "2021-02-21")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent(
                        "pickup_point/calendar/response_admin_update_override_days.json"), true));
    }

    @Test
    void getCalendarUneditableIntervals() throws Exception {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        List<Order> toBeCancelled = new ArrayList<>();

        createOrderWithDeliveryDate(pickupPoint, LocalDate.parse("2021-01-08"));
        createOrderWithDeliveryDate(pickupPoint, LocalDate.parse("2021-01-09"));
        createOrderWithDeliveryDate(pickupPoint, LocalDate.parse("2021-01-10"));

        toBeCancelled.add(createOrderWithDeliveryDate(pickupPoint, LocalDate.parse("2021-01-19")));
        createOrderWithDeliveryDate(pickupPoint, LocalDate.parse("2021-01-20"));

        toBeCancelled.add(createOrderWithDeliveryDate(pickupPoint, LocalDate.parse("2021-01-24")));
        createOrderWithDeliveryDate(pickupPoint, LocalDate.parse("2021-01-25"));
        createOrderWithDeliveryDate(pickupPoint, LocalDate.parse("2021-01-26"));
        createOrderWithDeliveryDate(pickupPoint, LocalDate.parse("2021-01-27"));


        toBeCancelled.forEach(o -> orderFactory.cancelOrder(o.getId()));

        mockMvc.perform(
                get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/calendar")
                        .param("date_from", "2021-01-01")
                        .param("date_to", "2021-01-26")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent(
                        "pickup_point/calendar/response_get_calendar_uneditable_intervals.json"), true));
    }

    @Test
    void getCalendarUneditableIntervalsWithDisabledDeliveryDatesCheck() throws Exception {
        configurationGlobalCommandService.setValue(DISABLE_DELIVERY_DATES_PICKUP_POINT_CALENDAR_CHECK, true);
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        List<Order> toBeCancelled = new ArrayList<>();

        createOrderWithDeliveryDate(pickupPoint, LocalDate.parse("2021-01-08"));
        createOrderWithDeliveryDate(pickupPoint, LocalDate.parse("2021-01-09"));
        createOrderWithDeliveryDate(pickupPoint, LocalDate.parse("2021-01-10"));

        toBeCancelled.add(createOrderWithDeliveryDate(pickupPoint, LocalDate.parse("2021-01-19")));
        createOrderWithDeliveryDate(pickupPoint, LocalDate.parse("2021-01-20"));

        toBeCancelled.add(createOrderWithDeliveryDate(pickupPoint, LocalDate.parse("2021-01-24")));
        createOrderWithDeliveryDate(pickupPoint, LocalDate.parse("2021-01-25"));
        createOrderWithDeliveryDate(pickupPoint, LocalDate.parse("2021-01-26"));
        createOrderWithDeliveryDate(pickupPoint, LocalDate.parse("2021-01-27"));


        toBeCancelled.forEach(o -> orderFactory.cancelOrder(o.getId()));

        mockMvc.perform(
                get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/calendar")
                        .param("date_from", "2021-01-01")
                        .param("date_to", "2021-01-26")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent(
                        "pickup_point/calendar" +
                                "/response_get_calendar_uneditable_intervals_with_disabled_delivery_check.json"),
                        true));
    }

    @Test
    void getCalendarFullBrandUnedtiableIntervals() throws Exception {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();

        brandRegionFactory.createDefaults();

        pickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
                        .build()
        );


        createOrderWithDeliveryDate(pickupPoint, LocalDate.parse("2021-01-08"));
        createOrderWithDeliveryDate(pickupPoint, LocalDate.parse("2021-07-12"));


        mockMvc.perform(
                get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/calendar")
                        .param("date_from", "2021-01-01")
                        .param("date_to", "2023-01-01")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent(
                        "pickup_point/calendar/response_get_calendar_full_brand_uneditable_intervals.json"),
                        true));
    }

    @Test
    void getCalendarOverridesNotInUneditableIntervals() throws Exception {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder
                .builder()
                .params(TestPickupPointFactory.PickupPointTestParams.builder()
                        .schedule(TestPickupPointFactory.PickupPointScheduleTestParams.builder()
                                .overrideDays(List.of(
                                        TestPickupPointFactory.PickupPointCalendarOverrideTestParams.builder()
                                                .date(LocalDate.parse("2021-01-08"))
                                                .isHoliday(true)
                                                .build(),
                                        TestPickupPointFactory.PickupPointCalendarOverrideTestParams.builder()
                                                .date(LocalDate.parse("2021-01-09"))
                                                .isHoliday(true)
                                                .build(),
                                        TestPickupPointFactory.PickupPointCalendarOverrideTestParams.builder()
                                                .date(LocalDate.parse("2021-01-10"))
                                                .isHoliday(true)
                                                .build(),
                                        TestPickupPointFactory.PickupPointCalendarOverrideTestParams.builder()
                                                .date(LocalDate.parse("2021-01-11"))
                                                .isHoliday(true)
                                                .build()
                                ))
                                .build())
                        .build())
                .build());

        createOrderWithDeliveryDate(pickupPoint, LocalDate.parse("2021-01-10"));

        mockMvc.perform(
                get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/calendar")
                        .param("date_from", "2021-01-01")
                        .param("date_to", "2023-01-20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent(
                        "pickup_point/calendar/" +
                                "response_get_calendar_overrides_not_in_uneditable_intervals.json"),
                        true));
    }

    private Order createOrderWithDeliveryDate(PickupPoint pickupPoint, LocalDate date) {
        return orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder
                .builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams
                        .builder()
                        .deliveryDate(date)
                        .build())
                .build());
    }
}
