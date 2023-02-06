package ru.yandex.market.core.delivery.calendar.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.calendar.DatePeriod;
import ru.yandex.market.core.calendar.export.CalendarExportService;
import ru.yandex.market.core.delivery.calendar.HolidayCalendar;
import ru.yandex.market.core.delivery.calendar.HolidayCalendarGetService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

/**
 * Тест для проверки экспорта календаря регионов ({@link RegionCalendarGetService}).
 *
 * @author ivmelnik
 */
class RegionCalendarTest extends FunctionalTest {

    private static final LocalDate TEST_START_DATE = LocalDate.of(2017, 12, 1);
    private static final int TEST_DAYS = 60;
    private static final DatePeriod TEST_PERIOD = DatePeriod.of(TEST_START_DATE, TEST_DAYS);

    private static final int[] EXPECTED_DAYS_NUMBERS = {1, 2, 7, 8, 9, 15, 16, 22, 23, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 43, 44};


    private static final List<HolidayCalendar> EXPECTED_CALENDARS_WITH_DATA =
            ImmutableList.of(
                    new HolidayCalendar(225, 225,
                            CalendarTestHelper.getLocalDates(TEST_START_DATE, EXPECTED_DAYS_NUMBERS))
            );

    private static final List<HolidayCalendar> EXPECTED_CALENDARS_WITHOUT_DATA =
            ImmutableList.of(
                    new HolidayCalendar(206, 206, Collections.emptyList()),
                    new HolidayCalendar(983, 983, Collections.emptyList()),
                    new HolidayCalendar(102, 102, Collections.emptyList()),
                    new HolidayCalendar(134, 134, Collections.emptyList()),
                    new HolidayCalendar(135, 135, Collections.emptyList()),
                    new HolidayCalendar(137, 137, Collections.emptyList()),
                    new HolidayCalendar(96, 96, Collections.emptyList())
            );


    @Autowired
    private HolidayCalendarGetService regionCalendarGetService;

    private CalendarWriterFactoryMock writerFactoryMock;

    private CalendarExportService calendarExportService;

    @BeforeEach
    void setUp() {
        writerFactoryMock = new CalendarWriterFactoryMock();
        calendarExportService = new DefaultCalendarExportService(writerFactoryMock, regionCalendarGetService);
    }

    @Test
    @DbUnitDataSet(before = {"calendar_common.csv", "region_calendar.csv"})
    void testExportRegionCalendar() throws IOException {
        calendarExportService.export(TEST_PERIOD);
        assertThat(writerFactoryMock.getWriterMock().getPeriod(), is(TEST_PERIOD));

        List<HolidayCalendar> allExpectedCalendars = new ArrayList<>();
        allExpectedCalendars.addAll(EXPECTED_CALENDARS_WITH_DATA);
        allExpectedCalendars.addAll(EXPECTED_CALENDARS_WITHOUT_DATA);

        assertThat(
                writerFactoryMock.getWriterMock().getHolidays(),
                containsInAnyOrder(allExpectedCalendars.toArray(new HolidayCalendar[0]))
        );
    }

}
