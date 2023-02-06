package ru.yandex.market.checkout.checkouter.util;

import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DateUtilsTest {

    @Test
    public void testFormatLocalDateSafe() {
        //given:
        LocalDate localDate = LocalDate.of(2020, 3, 8);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        //when:
        String result = DateUtils.formatLocalDateSafe(localDate, formatter);

        //then:
        assertEquals("08-03-2020", result);
    }

    @Test
    public void testFormatLocalDateSafeWithNullDate() {
        //given:
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        //when:
        String result = DateUtils.formatLocalDateSafe(null, formatter);

        //then:
        assertNull(result);
    }

    @Test
    public void testParseLocalDateSafe() {
        //given:
        String dateString = "1956-03-02";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        //when:
        LocalDate result = DateUtils.parseLocalDateSafe(dateString, formatter);

        //then:
        assertEquals(LocalDate.of(1956, 3, 2), result);
    }

    @Test
    public void testParseLocalDateSafeWithNullString() {
        //given:
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        //when:
        LocalDate result = DateUtils.parseLocalDateSafe(null, formatter);

        //then:
        assertNull(result);
    }

    @Test
    public void testDateToLocalDateSafeWithNull() {
        assertNull(DateUtils.dateToLocalDate(null, Clock.systemDefaultZone()));
    }

    @Test
    public void testDateToLocalDate() throws Exception {
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2022-11-09");
        LocalDate localDate = DateUtils.dateToLocalDate(date, Clock.systemDefaultZone());
        assertEquals(LocalDate.of(2022, 11, 9), localDate);
    }
}
