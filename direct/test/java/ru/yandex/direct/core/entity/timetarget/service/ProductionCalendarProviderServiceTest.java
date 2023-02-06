package ru.yandex.direct.core.entity.timetarget.service;

import java.time.LocalDate;
import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.timetarget.model.HolidayItem;
import ru.yandex.direct.core.entity.timetarget.repository.ProductionCalendarRepository;
import ru.yandex.direct.libs.timetarget.ProductionCalendar;
import ru.yandex.direct.libs.timetarget.WeekdayType;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ProductionCalendarProviderServiceTest {

    private static final int YEAR = 2017;
    private static final long REGION_ID = GeoTimezoneMappingService.DEFAULT_GEO_TIMEZONE_RUS.getRegionId();

    private static final LocalDate JANUARY_FIRST_SUNDAY = LocalDate.of(YEAR, 1, 1);
    private static final LocalDate JANUARY_SECOND_MONDAY = LocalDate.of(YEAR, 1, 2);
    private static final LocalDate JANUARY_THIRD_TUESDAY = LocalDate.of(YEAR, 1, 3);

    private ProductionCalendarProviderService providerService;
    private ProductionCalendarRepository productionCalendarRepository;

    @Before
    public void setup() throws Exception {
        productionCalendarRepository = mock(ProductionCalendarRepository.class);
        when(productionCalendarRepository.getHolidaysByYear(YEAR))
                .thenReturn(Arrays.asList(
                        new HolidayItem(REGION_ID, JANUARY_FIRST_SUNDAY, HolidayItem.Type.WORKDAY),
                        new HolidayItem(REGION_ID, JANUARY_SECOND_MONDAY, HolidayItem.Type.HOLIDAY)
                ));
        providerService = new ProductionCalendarProviderService(productionCalendarRepository);
    }

    @Test
    public void getProductionCalendar_correctCalendarReturned() throws Exception {
        ProductionCalendar calendar = providerService.getProductionCalendar(YEAR, REGION_ID);

        Assertions.assertThat(calendar.getWeekdayType(JANUARY_FIRST_SUNDAY)).isEqualTo(WeekdayType.WORKING_WEEKEND);
        Assertions.assertThat(calendar.getWeekdayType(JANUARY_SECOND_MONDAY)).isEqualTo(WeekdayType.HOLIDAY);
        Assertions.assertThat(calendar.getWeekdayType(JANUARY_THIRD_TUESDAY)).isEqualTo(WeekdayType.TUESDAY);
    }

    @Test
    public void getProductionCalendar_correctCalendarReturned_whenSeveralRegionsAvailable() throws Exception {
        long otherRegionId = 42L;
        when(productionCalendarRepository.getHolidaysByYear(YEAR))
                .thenReturn(Arrays.asList(
                        new HolidayItem(REGION_ID, JANUARY_FIRST_SUNDAY, HolidayItem.Type.WORKDAY),
                        new HolidayItem(REGION_ID, JANUARY_SECOND_MONDAY, HolidayItem.Type.HOLIDAY),
                        new HolidayItem(otherRegionId, JANUARY_FIRST_SUNDAY, HolidayItem.Type.HOLIDAY),
                        new HolidayItem(otherRegionId, JANUARY_THIRD_TUESDAY, HolidayItem.Type.HOLIDAY)
                ));

        ProductionCalendar calendar = providerService.getProductionCalendar(YEAR, REGION_ID);

        Assertions.assertThat(calendar.getWeekdayType(JANUARY_FIRST_SUNDAY)).isEqualTo(WeekdayType.WORKING_WEEKEND);
        Assertions.assertThat(calendar.getWeekdayType(JANUARY_SECOND_MONDAY)).isEqualTo(WeekdayType.HOLIDAY);
        Assertions.assertThat(calendar.getWeekdayType(JANUARY_THIRD_TUESDAY)).isEqualTo(WeekdayType.TUESDAY);
    }

    @Test
    public void getProductionCalendar_cachingWorks() throws Exception {
        providerService.getProductionCalendar(YEAR, REGION_ID);
        verify(productionCalendarRepository, times(1)).getHolidaysByYear(YEAR);

        providerService.getProductionCalendar(YEAR, REGION_ID);
        verifyNoMoreInteractions(productionCalendarRepository);
    }
}
