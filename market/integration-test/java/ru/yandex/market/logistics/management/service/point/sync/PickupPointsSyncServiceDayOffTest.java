package ru.yandex.market.logistics.management.service.point.sync;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.geobase.HttpGeobase;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.DateBool;
import ru.yandex.market.logistic.gateway.common.model.delivery.DateTime;
import ru.yandex.market.logistic.gateway.common.model.delivery.Location;
import ru.yandex.market.logistic.gateway.common.model.delivery.PickupPoint;
import ru.yandex.market.logistic.gateway.common.model.delivery.PickupPointType;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval;
import ru.yandex.market.logistic.gateway.common.model.delivery.WorkTime;
import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.util.TestableClock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PickupPointsSyncServiceDayOffTest extends AbstractContextualAspectValidationTest {
    private static final double LAT_KAPOTNYA = 55.631152;
    private static final double LON_KAPOTNYA = 37.799472;
    private static final int REGION_ID_KAPOTNYA = 120545;
    private static final DateTime EXISTING_DAY_OFF = new DateTime("2021-10-10");
    private static final DateTime NEW_DAY_OFF = new DateTime("2021-11-11");
    private static final DateBool EXISTING_CALENDAR = new DateBool(EXISTING_DAY_OFF, false);
    private static final DateBool EXISTING_CALENDAR_SWITCHED_OFF = new DateBool(EXISTING_DAY_OFF, true);
    private static final DateBool NEW_CALENDAR = new DateBool(NEW_DAY_OFF, false);

    @Autowired
    private PickupPointsSyncService pickupPointsSyncService;

    @Autowired
    private DeliveryClient deliveryClient;

    @Autowired
    private HttpGeobase httpGeobase;

    @Autowired
    private TestableClock clock;

    @Test
    @DisplayName("Синхронизирует пвз с проставлением дэйоффа")
    @DatabaseSetup(
        value = "/data/executor/before/sync_partner_pickup_point_for_dayoffs_and_holidays.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        value = "/data/executor/after/sync_partner_pickup_point_for_dayoffs.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void syncPartnerPickupPoints_newDayOff() throws GatewayApiException {
        when(deliveryClient.getReferencePickupPoints(any(), any(), any(), eq(new Partner(1L))))
            .thenReturn(List.of(pickupPointWithDayOffs(List.of(NEW_DAY_OFF))));
        when(httpGeobase.getRegionId(LAT_KAPOTNYA, LON_KAPOTNYA)).thenReturn(REGION_ID_KAPOTNYA);

        pickupPointsSyncService.syncPickupPoints(1L);

        DateTimeInterval calendarInterval = pickupPointsSyncService.getCalendarInterval(clock);
        verify(deliveryClient, times(1))
            .getReferencePickupPoints(isNull(), isNull(), eq(calendarInterval), any());
    }

    @Test
    @DisplayName("Синхронизирует пвз добавляет дэйоффы")
    @DatabaseSetup(
        value = "/data/executor/before/sync_partner_pickup_point_existing_dayoff.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        value = "/data/executor/after/sync_partner_pickup_point_existing_dayoff_add.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void syncPartnerPickupPoints_addDayOffToExisting() throws GatewayApiException {
        when(deliveryClient.getReferencePickupPoints(any(), any(), any(), eq(new Partner(1L))))
            .thenReturn(List.of(pickupPointWithDayOffs(List.of(EXISTING_DAY_OFF, NEW_DAY_OFF))));
        when(httpGeobase.getRegionId(LAT_KAPOTNYA, LON_KAPOTNYA)).thenReturn(REGION_ID_KAPOTNYA);

        pickupPointsSyncService.syncPickupPoints(1L);

        DateTimeInterval calendarInterval = pickupPointsSyncService.getCalendarInterval(clock);
        verify(deliveryClient, times(1))
            .getReferencePickupPoints(isNull(), isNull(), eq(calendarInterval), any());
    }

    @Test
    @DisplayName("Синхронизирует пвз заменяет дэйоффы")
    @DatabaseSetup(
        value = "/data/executor/before/sync_partner_pickup_point_existing_dayoff.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        value = "/data/executor/after/sync_partner_pickup_point_existing_dayoff_replace.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void syncPartnerPickupPoints_replaceDayOffs() throws GatewayApiException {
        when(deliveryClient.getReferencePickupPoints(any(), any(), any(), eq(new Partner(1L))))
            .thenReturn(
                List.of(pickupPointWithDayOffs(
                        List.of(NEW_DAY_OFF)
                    )
                )
            );
        when(httpGeobase.getRegionId(LAT_KAPOTNYA, LON_KAPOTNYA)).thenReturn(REGION_ID_KAPOTNYA);

        pickupPointsSyncService.syncPickupPoints(1L);

        DateTimeInterval calendarInterval = pickupPointsSyncService.getCalendarInterval(clock);
        verify(deliveryClient, times(1))
            .getReferencePickupPoints(isNull(), isNull(), eq(calendarInterval), any());
    }

    @Test
    @DisplayName("Синхронизирует пвз удаляет дэйоффы")
    @DatabaseSetup(
        value = "/data/executor/before/sync_partner_pickup_point_existing_dayoff.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        value = "/data/executor/after/sync_partner_pickup_point_existing_dayoff_deleted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void syncPartnerPickupPoints_deleteDayOffs() throws GatewayApiException {
        when(deliveryClient.getReferencePickupPoints(any(), any(), any(), eq(new Partner(1L))))
            .thenReturn(List.of(pickupPointWithDayOffs(List.of())));
        when(httpGeobase.getRegionId(LAT_KAPOTNYA, LON_KAPOTNYA)).thenReturn(REGION_ID_KAPOTNYA);

        pickupPointsSyncService.syncPickupPoints(1L);

        DateTimeInterval calendarInterval = pickupPointsSyncService.getCalendarInterval(clock);
        verify(deliveryClient, times(1))
            .getReferencePickupPoints(isNull(), isNull(), eq(calendarInterval), any());
    }

    @Test
    @DisplayName("Синхронизирует пвз включает выключенные дэйоффы")
    @DatabaseSetup(
        value = "/data/executor/before/sync_partner_pickup_point_existing_dayoff_swithed_off.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        value = "/data/executor/after/sync_partner_pickup_point_existing_dayoff_add.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void syncPartnerPickupPoints_switchOnExistingDayOffs() throws GatewayApiException {
        when(deliveryClient.getReferencePickupPoints(any(), any(), any(), eq(new Partner(1L))))
            .thenReturn(List.of(pickupPointWithDayOffs(List.of(NEW_DAY_OFF, EXISTING_DAY_OFF))));
        when(httpGeobase.getRegionId(LAT_KAPOTNYA, LON_KAPOTNYA)).thenReturn(REGION_ID_KAPOTNYA);

        pickupPointsSyncService.syncPickupPoints(1L);

        DateTimeInterval calendarInterval = pickupPointsSyncService.getCalendarInterval(clock);
        verify(deliveryClient, times(1))
            .getReferencePickupPoints(isNull(), isNull(), eq(calendarInterval), any());
    }

    @Test
    @DisplayName("Синхронизирует пвз с проставлением выходных из поля holidays")
    @DatabaseSetup(
        value = "/data/executor/before/sync_partner_pickup_point_for_dayoffs_and_holidays.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        value = "/data/executor/after/sync_partner_pickup_point_for_holiday.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void syncPartnerPickupPoints_addHolidays() throws GatewayApiException {
        when(deliveryClient.getReferencePickupPoints(any(), any(), any(), eq(new Partner(1L))))
            .thenReturn(List.of(pickupPointWithHolidaysAndCalendar(List.of(NEW_CALENDAR), null)));
        when(httpGeobase.getRegionId(LAT_KAPOTNYA, LON_KAPOTNYA)).thenReturn(REGION_ID_KAPOTNYA);

        pickupPointsSyncService.syncPickupPoints(1L);

        DateTimeInterval calendarInterval = pickupPointsSyncService.getCalendarInterval(clock);
        verify(deliveryClient, times(1))
            .getReferencePickupPoints(isNull(), isNull(), eq(calendarInterval), any());
    }

    @Test
    @DisplayName("Синхронизирует пвз с проставлением выходных из поля holidays, дополняет календарь")
    @DatabaseSetup(
        value = "/data/executor/before/sync_partner_pickup_point_with_calendar.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        value = "/data/executor/after/sync_partner_pickup_point_for_holiday_added.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void syncPartnerPickupPoints_addHolidays_addHolidayToCalendar() throws GatewayApiException {
        when(deliveryClient.getReferencePickupPoints(any(), any(), any(), eq(new Partner(1L))))
            .thenReturn(List.of(pickupPointWithHolidaysAndCalendar(List.of(NEW_CALENDAR), null)));
        when(httpGeobase.getRegionId(LAT_KAPOTNYA, LON_KAPOTNYA)).thenReturn(REGION_ID_KAPOTNYA);

        pickupPointsSyncService.syncPickupPoints(1L);

        DateTimeInterval calendarInterval = pickupPointsSyncService.getCalendarInterval(clock);
        verify(deliveryClient, times(1))
            .getReferencePickupPoints(isNull(), isNull(), eq(calendarInterval), any());
    }

    @Test
    @DisplayName("Синхронизирует пвз с проставлением выходных из поля holidays, игнорируя calendar_id")
    @DatabaseSetup(
        value = "/data/executor/before/sync_partner_pickup_point_for_dayoffs_and_holidays.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        value = "/data/executor/after/sync_partner_pickup_point_for_holiday.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void syncPartnerPickupPoints_addHolidays_preferHolidaysOverCalendar() throws GatewayApiException {
        when(deliveryClient.getReferencePickupPoints(any(), any(), any(), eq(new Partner(1L))))
            .thenReturn(List.of(pickupPointWithHolidaysAndCalendar(
                List.of(NEW_CALENDAR), List.of(EXISTING_CALENDAR))
            ));
        when(httpGeobase.getRegionId(LAT_KAPOTNYA, LON_KAPOTNYA)).thenReturn(REGION_ID_KAPOTNYA);

        pickupPointsSyncService.syncPickupPoints(1L);

        DateTimeInterval calendarInterval = pickupPointsSyncService.getCalendarInterval(clock);
        verify(deliveryClient, times(1))
            .getReferencePickupPoints(isNull(), isNull(), eq(calendarInterval), any());
    }

    @Test
    @DisplayName("Синхронизирует пвз с проставлением выходных из поля holidays, заменяет календарь")
    @DatabaseSetup(
        value = "/data/executor/before/sync_partner_pickup_point_with_calendar.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        value = "/data/executor/after/sync_partner_pickup_point_for_holiday_switched_off.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void syncPartnerPickupPoints_addHolidays_replaceCalendar() throws GatewayApiException {
        when(deliveryClient.getReferencePickupPoints(any(), any(), any(), eq(new Partner(1L))))
            .thenReturn(
                List.of(pickupPointWithHolidaysAndCalendar(List.of(NEW_CALENDAR, EXISTING_CALENDAR_SWITCHED_OFF), null)
                )
            );
        when(httpGeobase.getRegionId(LAT_KAPOTNYA, LON_KAPOTNYA)).thenReturn(REGION_ID_KAPOTNYA);

        pickupPointsSyncService.syncPickupPoints(1L);

        DateTimeInterval calendarInterval = pickupPointsSyncService.getCalendarInterval(clock);
        verify(deliveryClient, times(1))
            .getReferencePickupPoints(isNull(), isNull(), eq(calendarInterval), any());
    }

    private static PickupPoint pickupPointWithDayOffs(List<DateTime> dayOffs) {
        return PickupPoint.builder()
            .setCode("CODE1")
            .setName("addDayOff")
            .setAddress(getLocation())
            .setPhones(List.of())
            .setActive(true)
            .setInstruction("instruction1")
            .setType(PickupPointType.PICKUP_POINT)
            .setCalendar(List.of())
            .setSchedule(List.of(workTime1()))
            .setDayOffs(dayOffs)
            .build();
    }

    private static PickupPoint pickupPointWithHolidaysAndCalendar(List<DateBool> workDays, List<DateBool> calendar) {
        return PickupPoint.builder()
            .setCode("CODE1")
            .setName("addDayOff")
            .setAddress(getLocation())
            .setPhones(List.of())
            .setActive(true)
            .setInstruction("instruction1")
            .setType(PickupPointType.PICKUP_POINT)
            .setCalendar(List.of())
            .setSchedule(List.of(workTime1()))
            .setCalendar(calendar)
            .setWorkDays(workDays)
            .build();
    }

    private static Location getLocation() {
        return new Location.LocationBuilder("Россия", "Москва", "Московская область")
            .setLat(new BigDecimal(LAT_KAPOTNYA))
            .setLng(new BigDecimal(LON_KAPOTNYA))
            .build();
    }

    @Nonnull
    private static WorkTime workTime1() {
        return new WorkTime(
            1,
            List.of(
                TimeInterval.of(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                TimeInterval.of(LocalTime.of(13, 0), LocalTime.of(19, 0))
            )
        );
    }
}
