package ru.yandex.market.tsum.clients.calendar;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.tsum.clients.tvm.TvmAuthProvider;

import static org.mockito.Mockito.mock;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 11/11/2016
 */
public class CalendarClientTest {
    @Ignore
    @Test
    public void testGetEvents() throws Exception {
        CalendarClient calendarClient = new CalendarClient(
            "https://calendar-api.tools.yandex.net/internal/",
            null,
            new TvmAuthProvider(
                // clientId и TVM-секрет ЦУМа
                // https://abc.yandex-team.ru/services/tsum/resources/?show-resource=4693908
                2009827,
                "СЕКРЕТ",
                // clientId прода Календаря
                2011072
            ),
            "localhost",
            "127.0.0.1"
        );
        List<CalendarEvent> calendarEvents = calendarClient.getEvents(
            new Date(1476219600000L), new Date(1478811600000L), CalendarId.forLayerId(28568)
        ).get();

    }

    @Test
    public void testCaching() {
        CalendarClientForTest calendarClientForTest = new CalendarClientForTest();
        String sixthAugust = "2019-08-06";

        calendarClientForTest.getHolidays(Instant.parse(sixthAugust + "T13:15:00.00Z"), Instant.parse(sixthAugust +
            "T13:45:00.00Z"));
        calendarClientForTest.getHolidays(Instant.parse(sixthAugust + "T13:05:00.00Z"), Instant.parse(sixthAugust +
            "T13:55:00.00Z"));

        Assert.assertEquals(1, calendarClientForTest.getCacheAsMap().size());
        Assert.assertEquals(
            sixthAugust,
            calendarClientForTest.getCacheAsMap()
                .get(new CalendarClient.DateRangeFormatted(sixthAugust, sixthAugust))
                .getHolidays().get(0).getDate().toString());

        String seventhAugust = "2019-08-07";
        String eightAugust = "2019-08-08";
        calendarClientForTest.getHolidays(Instant.parse(seventhAugust + "T13:05:00.00Z"),
            Instant.parse(eightAugust + "T13:15:00.00Z"));

        Assert.assertEquals(2, calendarClientForTest.getCacheAsMap().size());
        Assert.assertEquals(
            seventhAugust,
            calendarClientForTest.getCacheAsMap()
                .get(new CalendarClient.DateRangeFormatted(seventhAugust, eightAugust))
                .getHolidays().get(0).getDate().toString());
    }

    @Test
    @Ignore
    public void testHolidays() {
        CalendarClient calendarClient = new CalendarClient(
            "https://calendar-api.tools.yandex.net/internal/",
            "https://api.calendar.yandex-team.ru",
            null,
            null,
            null
        );

        Instant from = Instant.parse("2020-12-16T00:00:00.00Z");
        Instant to = Instant.parse("2021-01-16T00:00:00.00Z");
        Holidays holidays = calendarClient.getHolidays(from, to);
        Assert.assertEquals(16, holidays.getHolidays().size());
    }

    private class CalendarClientForTest extends CalendarClient {
        CalendarClientForTest() {
            super(
                "http://localhost:8080/internal/",
                "http://localhost:8080",
                mock(TvmAuthProvider.class),
                "localhost",
                "127.0.0.1"
            );
        }

        @Override
        protected Holidays getHolidaysFromApi(String start, String end) {
            Holidays holidays = new Holidays();
            holidays.setHolidays(Collections.singletonList(new Holidays.Holiday(LocalDate.parse(start), "")));

            return holidays;
        }

        public Map<DateRangeFormatted, Holidays> getCacheAsMap() {
            return holidaysCache.asMap();
        }
    }
}
