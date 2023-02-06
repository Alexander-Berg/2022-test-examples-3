package ru.yandex.market.core.delivery.calendar.impl;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.calendar.Calendar;
import ru.yandex.market.core.calendar.CalendarOwnerType;
import ru.yandex.market.core.calendar.CalendarService;
import ru.yandex.market.core.calendar.CalendarType;
import ru.yandex.market.core.calendar.DailyCalendarService;
import ru.yandex.market.core.calendar.DatePeriod;
import ru.yandex.market.core.calendar.DayType;
import ru.yandex.market.core.calendar.OwnerMultiKey;
import ru.yandex.market.core.calendar.OwnerMultiKeyCalendar;
import ru.yandex.market.core.calendar.OwnerMultiKeyCalendarService;
import ru.yandex.market.core.calendar.WeeklyScheduleService;
import ru.yandex.market.core.calendar.CalendarProperties;
import ru.yandex.market.core.calendar.CalendarPropertiesRepository;
import ru.yandex.market.core.delivery.calendar.DeliveryCalendar;
import ru.yandex.market.core.delivery.calendar.DeliveryCalendarRepository;
import ru.yandex.market.core.delivery.calendar.DeliveryHolidayResolver;
import ru.yandex.market.core.delivery.calendar.DeliveryServiceDataSupplier;
import ru.yandex.market.core.delivery.calendar.HolidayCalendar;
import ru.yandex.market.core.supplier.dao.PartnerFulfillmentLinkDao;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Denis Chernyshov <zoom@yandex-team.ru>
 */
class DefaultDeliveryCalendarServiceTest {

    private static final int PERIOD_LENGTH = 10;

    private static final LocalDate NOW = LocalDate.of(2020, Month.JANUARY, 1);
    private static final LocalDate[] DAYS = createDays(NOW);


    @Test
    void shouldWorkWithoutData() {
        final DatePeriod period = DatePeriod.of(NOW, PERIOD_LENGTH);
        final DeliveryCalendarRepository deliveryCalendarRepository = mock(DeliveryCalendarRepository.class);
        doAnswer(invocation -> {
            assertEquals(period, invocation.getArgument(0));
            return null;
        }).when(deliveryCalendarRepository).getHolidays(any(), any());

        final CalendarPropertiesRepository calendarPropertiesRepository = mock(CalendarPropertiesRepository.class);
        when(calendarPropertiesRepository.findBy(eq(100))).thenReturn(new CalendarProperties(false));

        final DailyCalendarService dailyCalendarService = getDailyCalendarService();
        doReturn(getDefaultCalendar())
                .when(dailyCalendarService)
                .getDaysWithOwnerByCalendarType(eq(CalendarType.REGION_HOLIDAYS), eq(period));

        final DeliveryServiceDataSupplier dataSupplier = mock(DeliveryServiceDataSupplier.class);

        final DefaultDeliveryCalendarService service =
                new DefaultDeliveryCalendarService(
                        deliveryCalendarRepository,
                        calendarPropertiesRepository,
                        getCalendarService(),
                        getOwnerMultiKeyCalendarService(),
                        dailyCalendarService,
                        getWeeklyScheduleService(),
                        getDeliveryHolidayPositiveResolver(),
                        dataSupplier,
                        mockPartnerFulfillmentLinkDao(),
                        null);

        final List<HolidayCalendar> holidays = new ArrayList<>();
        service.getHolidayCalendars(period, holidays::add);
        assertEquals(emptyList(), holidays);
    }

    @Test
    void shouldWorkWithDeliveryServiceIdParam() {
        final DatePeriod period = DatePeriod.of(NOW, PERIOD_LENGTH);
        final DeliveryCalendarRepository deliveryCalendarRepository = mock(DeliveryCalendarRepository.class);

        final CalendarPropertiesRepository calendarPropertiesRepository = mock(CalendarPropertiesRepository.class);
        when(calendarPropertiesRepository.findBy(eq(200))).thenReturn(new CalendarProperties(false));

        final PartnerFulfillmentLinkDao partnerFulfillmentLinkDao = mock(PartnerFulfillmentLinkDao.class);
        when(partnerFulfillmentLinkDao.getDeliveryServiceToPartner(anySet())).thenReturn(Map.of());

        final DefaultDeliveryCalendarService service =
                new DefaultDeliveryCalendarService(
                        deliveryCalendarRepository,
                        calendarPropertiesRepository,
                        getCalendarService(),
                        getOwnerMultiKeyCalendarService(),
                        getDailyCalendarService(),
                        getWeeklyScheduleService(),
                        getDeliveryHolidayPositiveResolver(),
                        mock(DeliveryServiceDataSupplier.class, CALLS_REAL_METHODS),
                        partnerFulfillmentLinkDao,
                        null);

        final DeliveryCalendar calendar = service.getCalendar(100, 1, period);
        assertEquals(CalendarType.SHOP_SERVICE_DELIVERY, calendar.getType());
    }

    @Nonnull
    private DeliveryHolidayResolver getDeliveryHolidayPositiveResolver() {
        final DeliveryHolidayResolver resolver = mock(DeliveryHolidayResolver.class);
        when(resolver.isHoliday(anyBoolean(), anyBoolean(), any(), anyBoolean())).thenReturn(true);
        when(resolver.isRegionHoliday(any())).thenReturn(true);
        when(resolver.getHolidayDayTypes()).thenReturn(Set.of());
        return resolver;
    }

    private WeeklyScheduleService getWeeklyScheduleService() {
        final WeeklyScheduleService mock = mock(WeeklyScheduleService.class, CALLS_REAL_METHODS);
        doReturn(emptyList()).when(mock).getDays(eq(100));
        doReturn(emptyList()).when(mock).getDays(eq(200));
        return mock;
    }

    private DailyCalendarService getDailyCalendarService() {
        final DailyCalendarService mock = mock(DailyCalendarService.class, CALLS_REAL_METHODS);
        doReturn(emptyList()).when(mock).getDays(eq(100), any(DatePeriod.class));
        doReturn(emptyList()).when(mock).getDays(eq(200), any(DatePeriod.class));
        return mock;
    }

    private CalendarService getCalendarService() {
        final CalendarService mock = mock(CalendarService.class, CALLS_REAL_METHODS);
        doReturn(emptyList()).when(mock).getCalendars(CalendarType.REGION_DELIVERY);
        doReturn(Collections.singletonList(new Calendar(100, CalendarType.DEFAULT_DELIVERY, 0, null)))
                .when(mock)
                .getCalendars(CalendarType.DEFAULT_DELIVERY);
        return mock;
    }

    private OwnerMultiKeyCalendarService getOwnerMultiKeyCalendarService() {
        final OwnerMultiKeyCalendarService mock = mock(OwnerMultiKeyCalendarService.class, CALLS_REAL_METHODS);

        final OwnerMultiKey multiKey = new OwnerMultiKey(
                Pair.of(CalendarOwnerType.SHOP, 100L), Pair.of(CalendarOwnerType.DELIVERY_SERVICE, 1L));

        doReturn(new OwnerMultiKeyCalendar(200, CalendarType.SHOP_SERVICE_DELIVERY, multiKey)).when(mock).
                findOwnerCalendar(multiKey, CalendarType.SHOP_SERVICE_DELIVERY);
        return mock;
    }

    @Nonnull
    private Map<Long, Map<LocalDate, DayType>> getDefaultCalendar() {
        // Calendar
        final Map<Long, Map<LocalDate, DayType>> regionCalendars = new HashMap<>();
        final Map<LocalDate, DayType> regionCalendar = new HashMap<>();
        regionCalendar.put(DAYS[0], DayType.REGION_MOVED_WEEKEND);
        regionCalendar.put(DAYS[1], DayType.REGION_HOLIDAY);
        regionCalendars.put(225L, regionCalendar);
        return regionCalendars;
    }


    @Test
    void testShopWithoutRegionHolidays() {
        final DatePeriod period = DatePeriod.of(NOW, PERIOD_LENGTH);

        final DeliveryServiceDataSupplier dataSupplier = mock(DeliveryServiceDataSupplier.class, CALLS_REAL_METHODS);
        final DefaultDeliveryCalendarService service =
                new DefaultDeliveryCalendarService(
                        null,
                        null,
                        getCalendarService(),
                        null,
                        getDailyCalendarService(),
                        null,
                        getDeliveryHolidayPositiveResolver(),
                        dataSupplier,
                        mockPartnerFulfillmentLinkDao(),
                        null);

        doReturn(OptionalLong.of(96L)).when(dataSupplier).getShopRegionId(eq(100L));

        final Collection<LocalDate> calendar = service.getShopRegionHolidays(100, period);
        assertTrue(calendar.isEmpty());
    }

    private static LocalDate[] createDays(final LocalDate now) {
        final LocalDate[] days = new LocalDate[PERIOD_LENGTH];
        for (int i = 0; i < days.length; i++) {
            days[i] = now.plusDays(i);
        }
        return days;
    }

    private PartnerFulfillmentLinkDao mockPartnerFulfillmentLinkDao() {
        final PartnerFulfillmentLinkDao partnerFulfillmentLinkDao = mock(PartnerFulfillmentLinkDao.class);
        when(partnerFulfillmentLinkDao.getDeliveryServiceToPartner(anySet())).thenReturn(Map.of());
        return partnerFulfillmentLinkDao;
    }

}
