package ru.yandex.market.checkout.checkouter.delivery.outlet;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class WorkingDaysXmlParserTest {

    private static final String XML_WITHOUT_INTERVALS = "<WorkingTimes>" +
            "<WorkingTime>" +
            "<WorkingDaysFrom>1</WorkingDaysFrom>" +
            "<WorkingDaysTill>1</WorkingDaysTill>" +
            "<WorkingHoursFrom>00:00</WorkingHoursFrom>" +
            "<WorkingHoursTill>24:00</WorkingHoursTill>" +
            "</WorkingTime>" +
            "<WorkingTime>" +
            "<WorkingDaysFrom>2</WorkingDaysFrom>" +
            "<WorkingDaysTill>2</WorkingDaysTill>" +
            "<WorkingHoursFrom>00:00</WorkingHoursFrom>" +
            "<WorkingHoursTill>24:00</WorkingHoursTill>" +
            "</WorkingTime>" +
            "<WorkingTime>" +
            "<WorkingDaysFrom>4</WorkingDaysFrom>" +
            "<WorkingDaysTill>4</WorkingDaysTill>" +
            "<WorkingHoursFrom>00:00</WorkingHoursFrom>" +
            "<WorkingHoursTill>24:00</WorkingHoursTill>" +
            "</WorkingTime>" +
            "<WorkingTime>" +
            "<WorkingDaysFrom>5</WorkingDaysFrom>" +
            "<WorkingDaysTill>5</WorkingDaysTill>" +
            "<WorkingHoursFrom>00:00</WorkingHoursFrom>" +
            "<WorkingHoursTill>24:00</WorkingHoursTill>" +
            "</WorkingTime>" +
            "<WorkingTime>" +
            "<WorkingDaysFrom>6</WorkingDaysFrom>" +
            "<WorkingDaysTill>6</WorkingDaysTill>" +
            "<WorkingHoursFrom>00:00</WorkingHoursFrom>" +
            "<WorkingHoursTill>24:00</WorkingHoursTill>" +
            "</WorkingTime>" +
            "</WorkingTimes>";

    private static final String XML_WITH_INTERVALS = "<WorkingTimes>" +
            "<WorkingTime>" +
            "<WorkingDaysFrom>1</WorkingDaysFrom>" +
            "<WorkingDaysTill>2</WorkingDaysTill>" +
            "<WorkingHoursFrom>00:00</WorkingHoursFrom>" +
            "<WorkingHoursTill>24:00</WorkingHoursTill>" +
            "</WorkingTime>" +
            "<WorkingTime>" +
            "<WorkingDaysFrom>5</WorkingDaysFrom>" +
            "<WorkingDaysTill>7</WorkingDaysTill>" +
            "<WorkingHoursFrom>00:00</WorkingHoursFrom>" +
            "<WorkingHoursTill>24:00</WorkingHoursTill>" +
            "</WorkingTime>" +
            "</WorkingTimes>";

    private static final String XML_WITH_INTERVAL = "<WorkingTimes>" +
            "<WorkingTime>" +
            "<WorkingDaysFrom>1</WorkingDaysFrom>" +
            "<WorkingDaysTill>5</WorkingDaysTill>" +
            "<WorkingHoursFrom>00:00</WorkingHoursFrom>" +
            "<WorkingHoursTill>24:00</WorkingHoursTill>" +
            "</WorkingTime>" +
            "</WorkingTimes>";

    @Test
    public void parseXmlWithoutIntervals() throws IOException, SAXException {
        WorkingDaysXmlParser parser = new WorkingDaysXmlParser();
        parser.parseXmlReader(new StringReader(XML_WITHOUT_INTERVALS));
        var days = parser.getWorkingDays();
        assertThat(days, containsInAnyOrder(1, 2, 4, 5, 6));
    }

    @Test
    public void parseXmlWithIntervals() throws IOException, SAXException {
        WorkingDaysXmlParser parser = new WorkingDaysXmlParser();
        parser.parseXmlReader(new StringReader(XML_WITH_INTERVALS));
        var days = parser.getWorkingDays();
        assertThat(days, containsInAnyOrder(1, 2, 5, 6, 7));
    }

    @Test
    public void parseXmlWithInterval() throws IOException, SAXException {
        WorkingDaysXmlParser parser = new WorkingDaysXmlParser();
        parser.parseXmlReader(new StringReader(XML_WITH_INTERVAL));
        var days = parser.getWorkingDays();
        assertThat(days, containsInAnyOrder(1, 2, 3, 4, 5));
    }
}
