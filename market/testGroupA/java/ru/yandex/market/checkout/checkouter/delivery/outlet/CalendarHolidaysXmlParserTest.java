package ru.yandex.market.checkout.checkouter.delivery.outlet;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

public class CalendarHolidaysXmlParserTest {

    private static final String CALENDAR_HOLIDAYS_XML = "<CalendarHolidays>" +
            "<Date>2022-05-01</Date>" +
            "<Date>2022-05-02</Date>" +
            "<Date>2022-05-03</Date>" +
            "<Date>2022-05-09</Date>" +
            "<Date>2022-05-10</Date>" +
            "<Date>2022-06-12</Date>" +
            "<Date>2022-06-13</Date>" +
            "<StartDate>2022-04-29</StartDate>" +
            "<EndDate>2022-07-27</EndDate>" +
            "</CalendarHolidays>";

    private static final String EMPTY_CALENDAR_HOLIDAYS_XML = "<CalendarHolidays>" +
            "<StartDate>2022-04-29</StartDate>" +
            "<EndDate>2022-07-27</EndDate>" +
            "</CalendarHolidays>";

    private static final String INVALID_FORMAT_CALENDAR_HOLIDAYS_XML = "<CalendarHolidays>" +
            "<Date>2022/12/12</Date>" +
            "<Date>2022-05-02</Date>" +
            "<Date>2022-05-03</Date>" +
            "<Date>2022-05-09</Date>" +
            "<Date>2022-05-10</Date>" +
            "<Date>2022-06-12</Date>" +
            "<Date>2022-06-13</Date>" +
            "<StartDate>2022-04-29</StartDate>" +
            "<EndDate>2022-07-27</EndDate>" +
            "</CalendarHolidays>";

    @Test
    public void parseCalendarHolidaysXml() throws IOException, SAXException {
        CalendarHolidaysXmlParser parser = new CalendarHolidaysXmlParser();
        parser.parseXmlReader(new StringReader(CALENDAR_HOLIDAYS_XML));
        var holidays = parser.getHolidays();
        assertThat(holidays, containsInAnyOrder(
                LocalDate.parse("2022-05-01"),
                LocalDate.parse("2022-05-02"),
                LocalDate.parse("2022-05-03"),
                LocalDate.parse("2022-05-09"),
                LocalDate.parse("2022-05-10"),
                LocalDate.parse("2022-06-12"),
                LocalDate.parse("2022-06-13")
        ));
    }

    @Test
    public void parseEmptyCalendarHolidays() throws IOException, SAXException {
        CalendarHolidaysXmlParser parser = new CalendarHolidaysXmlParser();
        parser.parseXmlReader(new StringReader(EMPTY_CALENDAR_HOLIDAYS_XML));
        var holidays = parser.getHolidays();
        assertThat(holidays, hasSize(0));
    }

    @Test
    public void parseInvalidFormatCalendarHolidaysXml() throws IOException, SAXException {
        CalendarHolidaysXmlParser parser = new CalendarHolidaysXmlParser();
        try {
            parser.parseXmlReader(new StringReader(INVALID_FORMAT_CALENDAR_HOLIDAYS_XML));
            Assertions.fail("Test should throw exception!");
        } catch (Exception e) {
        }
    }

}
