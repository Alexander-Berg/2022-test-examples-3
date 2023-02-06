package ru.yandex.market.billing.tasks.calendar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.Month;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import junit.framework.AssertionFailedError;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.xml.sax.SAXException;

import ru.yandex.market.core.calendar.DatePeriod;
import ru.yandex.market.core.delivery.calendar.HolidayCalendar;
import ru.yandex.market.core.delivery.calendar.impl.CalendarTestHelper;
import ru.yandex.market.core.delivery.calendar.impl.XmlHolidayCalendarWriter;
import ru.yandex.market.notification.service.provider.content.BaseXslContentProvider;

/**
 * Тест для {@link XmlHolidayCalendarWriter}.
 * <p>
 * Проверки формирования xml по разным календарям.
 */
public class XmlHolidayCalendarWriterTest extends XMLAssert {

    private static final LocalDate START_DATE = LocalDate.of(2010, Month.APRIL, 2);
    private static final int DAYS = 7;
    private static final DatePeriod PERIOD = DatePeriod.of(START_DATE, DAYS);

    private static void assertEqual(String expected, String actual) throws IOException, SAXException {
        try {
            XMLUnit.setIgnoreWhitespace(true);
            assertXMLEqual("", expected, actual);
        } catch (AssertionFailedError e) {
            System.out.println("Expected: " + format(expected));
            System.out.println("Actual: " + format(actual));
            throw e;
        }
    }

    public static String format(String xml) {

        try {
            Source xmlInput = new StreamSource(new StringReader(xml));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = BaseXslContentProvider.createTransformerFactory();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (Exception e) {
            throw new RuntimeException(e); // simple exception handling, please review it
        }
    }

    @Test
    public void holidayCalendar() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLOutputFactory xof = XMLOutputFactory.newInstance();
        XMLStreamWriter xmlWriter = xof.createXMLStreamWriter(out, "UTF-8");
        XmlHolidayCalendarWriter writer = new XmlHolidayCalendarWriter("shops", "shop", PERIOD, xmlWriter);
        writer.before();
        writer.addCalendar(new HolidayCalendar(1, 102, CalendarTestHelper.getLocalDates(START_DATE, 0, 2, 4)));
        writer.addCalendar(new HolidayCalendar(1, 103, CalendarTestHelper.getLocalDates(START_DATE, 3, 5, 7)));
        writer.addCalendar(new HolidayCalendar(2, 104, CalendarTestHelper.getLocalDates(START_DATE, 0, 2, 9)));
        writer.addCalendar(new HolidayCalendar(3, 105, 123L, CalendarTestHelper.getLocalDates(START_DATE, 1, 3, 5)));
        writer.after();
        writer.close();
        assertEqual(
                "<calendars start-date=\"02.04.2010\" depth-days=\"7\">\n" +
                        "    <shops>\n" +
                        "        <shop id=\"1\" default-calendar=\"102\">\n" +
                        "            <calendar id=\"102\" holidays=\"0,2,4\"/>\n" +
                        "            <calendar id=\"103\" holidays=\"3,5,7\"/>\n" +
                        "        </shop>\n" +
                        "        <shop id=\"2\" default-calendar=\"104\">\n" +
                        "            <calendar id=\"104\" holidays=\"0,2\"/>\n" +  //9 will be cut because depth-days=7
                        "        </shop>\n" +
                        "        <shop id=\"3\" default-calendar=\"105\">\n" +
                        "            <calendar id=\"105\" holidays=\"1,3,5\" lms-partner-id=\"123\"/>\n" +
                        "        </shop>\n" +
                        "    </shops>\n" +
                        "</calendars>", out.toString());
    }

}
