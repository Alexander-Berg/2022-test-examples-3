package ru.yandex.market.core.outlet.calendar.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.ThrowsException;

import ru.yandex.market.core.backa.persist.addr.Coordinates;
import ru.yandex.market.core.calendar.Calendar;
import ru.yandex.market.core.calendar.CalendarService;
import ru.yandex.market.core.calendar.CalendarType;
import ru.yandex.market.core.calendar.DailyCalendarService;
import ru.yandex.market.core.calendar.DatePeriod;
import ru.yandex.market.core.calendar.Day;
import ru.yandex.market.core.calendar.DayType;
import ru.yandex.market.core.delivery.calendar.DeliveryHolidayResolver;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.geobase.model.Region;
import ru.yandex.market.core.geobase.model.RegionType;
import ru.yandex.market.core.outlet.GeoInfo;
import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.core.outlet.OutletType;
import ru.yandex.market.core.outlet.calendar.OutletCalendarInfo;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyZeroInteractions;

public class DefaultOutletCalendarServiceTest {
    private DefaultOutletCalendarService outletCalendarService;
    private CalendarService calendarService;
    private DailyCalendarService dailyCalendarService;
    private DeliveryHolidayResolver deliveryHolidayResolver;
    private RegionService regionService;

    @Before
    public void setUp() throws Exception {
        calendarService = Mockito.mock(CalendarService.class, new ThrowsException(new RuntimeException("mock")));
        dailyCalendarService = Mockito.mock(DailyCalendarService.class, new ThrowsException(new RuntimeException("mock")));
        deliveryHolidayResolver = Mockito.mock(DeliveryHolidayResolver.class, new ThrowsException(new RuntimeException("mock")));
        regionService = Mockito.mock(RegionService.class, new ThrowsException(new RuntimeException("mock")));

        outletCalendarService = new DefaultOutletCalendarService(
                calendarService,
                dailyCalendarService,
                deliveryHolidayResolver,
                regionService);
    }

    @Test
    public void getOutletCalendarInfoFactoryTest() {
        Collection<OutletInfo> outletInfos = new ArrayList<>();
        OutletInfo outletInfo;
        // default GeoInfo
        outletInfos.add(new OutletInfo(0, 0, OutletType.DEPOT, null, null, null));
        // GeoInfo with null fields
        outletInfos.add(outletInfo = new OutletInfo(0, 0, OutletType.DEPOT, null, null, null));
        outletInfo.setGeoInfo(new GeoInfo(null, null));
        // filled GeoInfo
        outletInfos.add(outletInfo = new OutletInfo(0, 0, OutletType.DEPOT, null, null, null));
        outletInfo.setGeoInfo(new GeoInfo(Coordinates.valueOf("123,321"), 11L));

        doReturn(Collections.emptyList()).when(calendarService).getCalendars(CalendarType.REGION_HOLIDAYS);

        doReturn(null).when(regionService).getRegion(-1L);
        doReturn(new Region(1, null, null, RegionType.COUNTRY)).when(regionService).getRegion(11L);

        Function<OutletInfo, OutletCalendarInfo> calendarByOutlet = outletCalendarService.getOutletCalendarInfoFactory(DatePeriod.of(LocalDate.of(2016, 1, 1), 10));
        List<OutletCalendarInfo> calendars = outletInfos
                .stream()
                .map(calendarByOutlet)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        assertTrue(calendars.isEmpty());
    }

    /**
     * Тест проверяет, что функция, которая возвращается из метода
     * {@link DefaultOutletCalendarService#getOutletCalendarInfoFactory(ru.yandex.market.core.calendar.DatePeriod)}
     * не взаимодействует с базой данных (за исключением прокешированного {@link RegionService}).
     * <p>
     * Эта проверка необходима, так как функция вызвается для каждого аутлета в цикле и поход в базу на каждое
     * обращение приведет к сильному замедлению экспорта аутлетов.
     */
    @Test
    public void testNoDatabaseInteractionInFactoryMethod() {
        doReturn(Collections.singletonList(new Calendar(10, CalendarType.REGION_HOLIDAYS, 3L, null)))
                .when(calendarService).getCalendars(CalendarType.REGION_HOLIDAYS);
        doReturn(Arrays.asList(new Day(LocalDate.now(), DayType.REGION_HOLIDAY), new Day(LocalDate.now().plusDays(1), DayType.REGION_WORKDAY)))
                .when(dailyCalendarService).getDays(eq(10), any(), any());
        doReturn(Collections.emptySet()).when(deliveryHolidayResolver).getHolidayDayTypes();

        Function<OutletInfo, OutletCalendarInfo> factory = outletCalendarService.getOutletCalendarInfoFactory(DatePeriod.of(LocalDate.now(), 10));

        reset(deliveryHolidayResolver, calendarService, dailyCalendarService);

        doReturn(new Region(1L, "город", 2L)).when(regionService).getRegion(anyLong());
        doReturn(new Region(3L, "страна", 4L)).when(regionService).getParentRegion(any(), anyLong());

        factory.apply(new OutletInfo(111L, 774L, OutletType.DEPOT, "test", true, "test-id"));

        verifyZeroInteractions(deliveryHolidayResolver, calendarService, dailyCalendarService);
    }
}
