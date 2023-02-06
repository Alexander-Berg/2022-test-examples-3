package ru.yandex.market.core.delivery.calendar.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.calendar.DatePeriod;
import ru.yandex.market.core.calendar.export.CalendarExportService;
import ru.yandex.market.core.delivery.calendar.HolidayCalendar;
import ru.yandex.market.core.delivery.calendar.HolidayCalendarGetService;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест для проверки экспорта календаря доставки.
 *
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class DeliveryCalendarTest extends FunctionalTest {

    @Autowired
    @Qualifier("deliveryCalendarService")
    private HolidayCalendarGetService deliveryCalendarService;

    private CalendarWriterFactoryMock writerFactoryMock;

    private CalendarExportService calendarExportService;

    private static final int DAY_COUNT = 55;
    private static final LocalDate BEGIN_DATE = LocalDate.now().plusDays(-10);
    private static final DatePeriod PERIOD = DatePeriod.of(BEGIN_DATE, DAY_COUNT);

    @BeforeEach
    void setUp() {
        writerFactoryMock = new CalendarWriterFactoryMock();
        calendarExportService = new DefaultCalendarExportService(writerFactoryMock, deliveryCalendarService);
    }

    /**
     * Проверяем, что текущая дата праздничный день.
     */
    @Test
    @DbUnitDataSet(before = "DeliveryCalendarTest.testCurrentDayHoliday.csv")
    void testCurrentDayHoliday() throws IOException {
        var currentDay = LocalDate.now();
        DatePeriod period = DatePeriod.of(currentDay, 1);
        calendarExportService.export(period);
        assertThat(writerFactoryMock.getWriterMock().getPeriod()).isEqualTo(period);
        assertThat(writerFactoryMock.getWriterMock().getHolidays())
                .contains(new HolidayCalendar(774, 6, List.of(currentDay)));
    }

    /**
     * Проверяем, что от текущей даты и следующие 7 дней праздничные дни
     */
    @Test
    @DbUnitDataSet(before = "DeliveryCalendarTest.testSevenHolidays.csv")
    void testSevenHolidays() throws IOException {
        var numberDays = 7;
        LocalDate beginDate = LocalDate.now();
        DatePeriod period = DatePeriod.of(beginDate, 15);
        calendarExportService.export(period);
        assertThat(writerFactoryMock.getWriterMock().getPeriod()).isEqualTo(period);
        assertThat(writerFactoryMock.getWriterMock().getHolidays())
                .contains(new HolidayCalendar(774, 6,
                                IntStream.range(0, numberDays).mapToObj(beginDate::plusDays).collect(Collectors.toList())
                        )
                );
    }

    /**
     * Проверяем, что праздничные дни можно выгружать частично
     */
    @Test
    @DbUnitDataSet(before = "DeliveryCalendarTest.testSevenHolidays.csv")
    void testSevenHolidaysPart() throws IOException {
        var numberDays = 7;
        LocalDate beginDate = LocalDate.now().plusDays(-4);
        DatePeriod period = DatePeriod.of(beginDate, numberDays);
        calendarExportService.export(period);
        assertThat(writerFactoryMock.getWriterMock().getPeriod()).isEqualTo(period);
        assertThat(writerFactoryMock.getWriterMock().getHolidays())
                .contains(new HolidayCalendar(774, 6,
                                IntStream.range(4, numberDays).mapToObj(beginDate::plusDays).collect(Collectors.toList())
                        )
                );
    }

    /**
     * Проверка того, что период выборки может оказаться без выходных
     */
    @Test
    @DbUnitDataSet(before = "DeliveryCalendarTest.testEmptyHolidays.csv")
    void testEmptyHolidays() throws IOException {
        LocalDate beginDate = LocalDate.now();
        DatePeriod period = DatePeriod.of(beginDate, 2);
        calendarExportService.export(DatePeriod.of(beginDate, 2));
        assertThat(writerFactoryMock.getWriterMock().getPeriod()).isEqualTo(period);
        assertThat(writerFactoryMock.getWriterMock().getHolidays())
                .contains(new HolidayCalendar(774, 6, Collections.emptyList()));
    }

    /**
     * Проверка того, любой день недели можем задать циклично выходным днем
     * (в тесте четверг)
     */
    @Test
    @DbUnitDataSet(before = "DeliveryCalendarTest.testCycleHoliday.csv")
    void testCycleHoliday() throws IOException {
        calendarExportService.export(PERIOD);
        assertThat(writerFactoryMock.getWriterMock().getPeriod()).isEqualTo(PERIOD);
        var expectedDateHolidays =
                Stream.iterate(BEGIN_DATE, date -> date.plusDays(1))
                        .limit(DAY_COUNT)
                        .filter(date -> date.getDayOfWeek().equals(DayOfWeek.THURSDAY))
                        .collect(Collectors.toList());
        var expectedHolidayCalendar = new HolidayCalendar(774, 6, expectedDateHolidays);
        assertThat(writerFactoryMock.getWriterMock().getHolidays()).contains(expectedHolidayCalendar);
    }

    /**
     * Проверка того, любой день недели можем задать циклично выходным днем
     * (в тесте четверг)
     * Но получилось так, что в ближайший четверг, магазин решил поработать
     */
    @Test
    @DbUnitDataSet(before = "DeliveryCalendarTest.testCycleHolidayButOneDayException.csv")
    void testCycleHolidayButOneDayException() throws IOException {
        calendarExportService.export(PERIOD);
        assertThat(writerFactoryMock.getWriterMock().getPeriod()).isEqualTo(PERIOD);
        var expectedDateHolidays =
                Stream.iterate(BEGIN_DATE, date -> date.plusDays(1))
                        .limit(DAY_COUNT)
                        .filter(date -> date.getDayOfWeek().equals(DayOfWeek.THURSDAY))
                        .collect(Collectors.toList());
        var nearestThursday = LocalDate.now();
        while (!nearestThursday.getDayOfWeek().equals(DayOfWeek.THURSDAY)) {
            nearestThursday = nearestThursday.plusDays(1);
        }
        expectedDateHolidays.remove(nearestThursday);

        var expectedHolidayCalendar = new HolidayCalendar(774, 6, expectedDateHolidays);
        assertThat(writerFactoryMock.getWriterMock().getHolidays()).contains(expectedHolidayCalendar);
    }

    /**
     * Магазин указал "вожу в праздники"
     * Праздниками указан текущий день и следующие 7 дней
     * Ожидается, что список выходных дней у магазина будет пустым
     */
    @Test
    @DbUnitDataSet(before = "DeliveryCalendarTest.testWorkingInHolidays.csv")
    void testWorkingInHolidays() throws IOException {
        LocalDate beginDate = LocalDate.now();
        DatePeriod period = DatePeriod.of(beginDate, 15);
        calendarExportService.export(DatePeriod.of(beginDate, 15));
        assertThat(writerFactoryMock.getWriterMock().getPeriod()).isEqualTo(period);
        assertThat(writerFactoryMock.getWriterMock().getHolidays())
                .contains(new HolidayCalendar(774, 6, List.of()));
    }

    /**
     * Магазин указал "вожу в праздники", но, например, решил отдохнуть завтра
     * Праздниками указан текущий день и следующие 7 дней
     * Ожидается, что в списке выходных дней будет только завтрашний день
     */
    @Test
    @DbUnitDataSet(before = "DeliveryCalendarTest.testWorkingInHolidaysButOneDayReallyHoliday.csv")
    void testWorkingInHolidaysButOneDayReallyHoliday() throws IOException {
        LocalDate beginDate = LocalDate.now();
        DatePeriod period = DatePeriod.of(beginDate, 15);
        calendarExportService.export(DatePeriod.of(beginDate, 15));
        assertThat(writerFactoryMock.getWriterMock().getPeriod()).isEqualTo(period);
        assertThat(writerFactoryMock.getWriterMock().getHolidays())
                .contains(new HolidayCalendar(774, 6, List.of(LocalDate.now().plusDays(1))));
    }
}
