package ru.yandex.market.abo.core.calendar;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.yandex.market.abo.core.calendar.db.CalendarEntry;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Anton Irinev (airinev@yandex-team.ru)
 */
class CalendarParserTest {
    private static final String DATE = "2020-01-01";
    private static final String PREFIX =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><holidays><get-holidays country-id=\"225\" " +
                    "start-date=\"2008-12-09\" end-date=\"2009-01-20\" out-mode=\"all\"><days>";
    private static final String SUFFIX = "</days></get-holidays></holidays>";

    @Test
    void testComplexXml() throws Exception {
        List<CalendarEntry> expectedData = getExpectedData();
        List<CalendarEntry> testData = getTestData();

        assertEquals(33, expectedData.size());
        assertEquals(expectedData.size(), testData.size());

        for (int i = 0; i < expectedData.size(); i++) {
            CalendarEntry expectedEntry = expectedData.get(i);
            CalendarEntry actualEntry = testData.get(i);
            assertEquals(expectedEntry, actualEntry);
        }
    }

    @Test
    void testParsingHolidayWithoutTitle() throws CalendarParseException {
        testXmlWithSingleDayNode("2008-12-13", true, "", "0");
    }

    @Test
    void testParsingHolidayWithTitle() throws CalendarParseException {
        testXmlWithSingleDayNode("2008-12-12", true, "День Конституции", "0");
    }

    @Test
    void testParsingWorkday() throws CalendarParseException {
        testXmlWithSingleDayNode("2008-12-11", false, "", "0");
    }

    @Test
    void testParsingWorkdayWithTitle() throws CalendarParseException {
        testXmlWithSingleDayNode("2009-01-11", false, "Перенос", "0");
    }

    @ParameterizedTest(name = "isHolidayTest_{index}")
    @MethodSource("isHolidayTestMethodSource")
    void isHolidayTest(
            boolean isHoliday, String isTransfer, CalendarParser.DayType dayType, boolean expectedIsHoliday
    ) throws CalendarParseException {
        String dayNode = constructDayNode(isHoliday, isTransfer, dayType);
        var data = dayNode.getBytes(StandardCharsets.UTF_8);
        var byteArrayInputStream = new ByteArrayInputStream(data);

        List<CalendarEntry> entries = CalendarParser.parse(byteArrayInputStream);
        assertEquals(expectedIsHoliday, entries.get(0).isHoliday());
    }

    static Stream<Arguments> isHolidayTestMethodSource() {
        return Stream.of(
                Arguments.of(true, "1", CalendarParser.DayType.WEEKEND, true),
                Arguments.of(false, "0", CalendarParser.DayType.WEEKEND, false),
                Arguments.of(true, "1", CalendarParser.DayType.HOLIDAY, true),
                Arguments.of(true, "0", CalendarParser.DayType.HOLIDAY, true),
                Arguments.of(false, "1", CalendarParser.DayType.WEEKEND, true)
        );
    }

    private static String constructDayNode(boolean isHoliday, String isTransfer, CalendarParser.DayType dayType) {
        String holiday = (isHoliday) ? "1" : "0";
        String node = "<day date=\"" + DATE + "\" is-holiday=\"" + holiday + "\" day-type=\"" + dayType +
                "\" is-transfer=\"" + isTransfer + "\"></day>";
        return PREFIX + node + SUFFIX;
    }

    private static List<CalendarEntry> getExpectedData() throws Exception {
        InputStream stream = CalendarParserTest.class.getResourceAsStream("/calendar/expected_holidays.txt");

        InputStreamReader isr = new InputStreamReader(stream, StandardCharsets.UTF_8);
        List<CalendarEntry> expectedEntries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(isr)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String date = line.substring(0, 10);
                boolean weekend = line.charAt(11) == '1';
                boolean holiday = line.charAt(13) == '1';
                expectedEntries.add(new CalendarEntry(Date.valueOf(date), weekend, holiday, "title"));
            }
        }

        return expectedEntries;
    }

    private static String constructDayNode(String date, boolean isHoliday, String title, String isTransfer) {
        String holiday = (isHoliday) ? "1" : "0";
        String dayType = (isHoliday ? CalendarParser.DayType.HOLIDAY : CalendarParser.DayType.WEEKDAY)
                .name().toLowerCase();
        String node = (title.length() > 0) ?
                "<day date=\"" + date + "\" is-holiday=\"" + holiday + "\" day-type=\"" + dayType + "\" is-transfer=\""
                        + isTransfer + "\">" + title + "</day>" :
                "<day date=\"" + date + "\" is-holiday=\"" + holiday + "\" day-type=\"" + dayType + "\" is-transfer=\""
                        + isTransfer + "\">" + "</day>";

        return PREFIX + node + SUFFIX;
    }

    private static InputStream getStreamForXmlWithSingleDayNode(String date, boolean isHoliday, String title, String isTransfer) {
        String xmlString = constructDayNode(date, isHoliday, title, isTransfer);
        byte[] data = xmlString.getBytes(StandardCharsets.UTF_8);
        return new ByteArrayInputStream(data);
    }

    private static List<CalendarEntry> getTestData() throws Exception {
        InputStream stream = CalendarParserTest.class.getResourceAsStream("/calendar/holidays.xml");
        return CalendarParser.parse(stream);
    }

    private static void testXmlWithSingleDayNode(String date, boolean isHoliday, String title, String isTransfer)
            throws CalendarParseException {
        InputStream stream = getStreamForXmlWithSingleDayNode(date, isHoliday, title, isTransfer);
        List<CalendarEntry> entries = CalendarParser.parse(stream);

        assertEquals(1, entries.size());
        CalendarEntry entry = entries.get(0);

        assertEquals(Date.valueOf(date), entry.getDate());
        assertEquals(isHoliday, entry.isHoliday());
        assertEquals(title, entry.getTitle());
    }
}
