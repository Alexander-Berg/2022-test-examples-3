package ru.yandex.market.archive.schema;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static ru.yandex.market.archive.schema.DateSlice.DH;
import static ru.yandex.market.archive.schema.DateSlice.DT;
import static ru.yandex.market.archive.schema.DateSlice.YM;

/**
 * @author snoop
 */
public class DateSliceTest {

    private static void parseFail(DateSlice dateSlice, String str) {
        Assertions.assertThrows(DateTimeException.class, () -> dateSlice.parse(str));
    }

    @Test
    public void format() {
        LocalDateTime dateTime = LocalDateTime.of(2017, Month.APRIL, 23, 17, 58, 43, 123_456_789);

        Assertions.assertEquals("2017-04-23-17", DH.format(dateTime));
        Assertions.assertEquals("2017-04-23", DT.format(dateTime));
        Assertions.assertEquals("2017-04", YM.format(dateTime));
    }

    @Test
    public void parse() {
        List<String> strings = Arrays.asList(
                "2017-04-23-17-58", "2017-04-23-17", "2017-04-23", "2017-04", "2017"
        );

        LocalDateTime dateTime = LocalDateTime.of(2017, Month.APRIL, 23, 17, 0);
        Assertions.assertEquals(dateTime, DH.parse("2017-04-23-17"));
        parseFail(DH, strings.get(0));
        parseFail(DH, strings.get(2));
        parseFail(DH, strings.get(3));
        parseFail(DH, strings.get(4));

        dateTime = dateTime.toLocalDate().atStartOfDay();
        Assertions.assertEquals(dateTime, DT.parse("2017-04-23"));
        parseFail(DT, strings.get(0));
        parseFail(DT, strings.get(1));
        parseFail(DT, strings.get(3));
        parseFail(DT, strings.get(4));

        dateTime = YearMonth.from(dateTime).atDay(1).atStartOfDay();
        Assertions.assertEquals(dateTime, YM.parse("2017-04"));
        parseFail(YM, strings.get(0));
        parseFail(YM, strings.get(1));
        parseFail(YM, strings.get(2));
        parseFail(YM, strings.get(4));
    }

    @Test
    public void parseBest() {
        LocalDateTime dateTime = LocalDateTime.of(2017, Month.APRIL, 23, 17, 0);
        Assertions.assertEquals(dateTime, LocalDateTime.from(DH.parseBest("2017-04-23-17")));

        LocalDate date = dateTime.toLocalDate();
        Assertions.assertEquals(date, LocalDate.from(DH.parseBest("2017-04-23")));
        Assertions.assertEquals(date, LocalDate.from(DT.parseBest("2017-04-23")));

        YearMonth month = YearMonth.from(date);
        Assertions.assertEquals(month, YearMonth.from(DT.parseBest("2017-04")));
        Assertions.assertEquals(month, YearMonth.from(YM.parseBest("2017-04")));

        Year year = Year.from(month);
        Assertions.assertEquals(year, Year.from(YM.parseBest("2017")));
    }

    @Test
    public void isFull() {
        LocalDateTime dateTime = LocalDateTime.of(2017, Month.APRIL, 23, 17, 0);
        Assertions.assertTrue(DH.isFull(dateTime));
        Assertions.assertTrue(DT.isFull(dateTime));
        Assertions.assertTrue(YM.isFull(dateTime));

        LocalDate date = dateTime.toLocalDate();
        Assertions.assertFalse(DH.isFull(date));
        Assertions.assertTrue(DT.isFull(date));
        Assertions.assertTrue(YM.isFull(date));

        YearMonth month = YearMonth.from(date);
        Assertions.assertFalse(DH.isFull(month));
        Assertions.assertFalse(DT.isFull(month));
        Assertions.assertTrue(YM.isFull(month));

        Year year = Year.from(month);
        Assertions.assertFalse(DH.isFull(year));
        Assertions.assertFalse(DT.isFull(year));
        Assertions.assertFalse(YM.isFull(year));
    }

    @Test
    public void toHour() {
        LocalDateTime dateTime = LocalDateTime.of(2017, Month.APRIL, 23, 17, 58);
        Assertions.assertEquals(dateTime.truncatedTo(ChronoUnit.HOURS), DateSlice.toHour(dateTime));
        LocalDate date = dateTime.toLocalDate();
        Assertions.assertEquals(LocalDateTime.of(2017, Month.APRIL, 23, 0, 0), DateSlice.toHour(date));
        YearMonth month = YearMonth.from(date);
        Assertions.assertEquals(LocalDateTime.of(2017, Month.APRIL, 1, 0, 0), DateSlice.toHour(month));
        Year year = Year.from(month);

        Assertions.assertThrows(IllegalArgumentException.class, () ->
                Assertions.assertSame(LocalDateTime.of(2017, Month.JANUARY, 1, 0, 0),
                        DateSlice.toHour(year)));
    }

}
