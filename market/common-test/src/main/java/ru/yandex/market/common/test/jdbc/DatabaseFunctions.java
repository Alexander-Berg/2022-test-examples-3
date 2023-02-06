package ru.yandex.market.common.test.jdbc;

import com.google.common.base.Strings;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * User defined functions/procedures.
 *
 * @author zoom
 */
@ParametersAreNonnullByDefault
public class DatabaseFunctions {

    private static final Map<String, Function<ZonedDateTime, ZonedDateTime>> MODES = new HashMap<>();

    static {
        Function<ZonedDateTime, ZonedDateTime> month = zonedDateTime -> {
            return zonedDateTime.with(TemporalAdjusters.firstDayOfMonth());
        };
        MODES.put("month", month);
        MODES.put("mon", month);
        MODES.put("mm", month);

        Function<ZonedDateTime, ZonedDateTime> iw = zonedDateTime -> {
            return zonedDateTime.minusDays(zonedDateTime.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
        };
        MODES.put("iw", iw);

        Function<ZonedDateTime, ZonedDateTime> day = zonedDateTime -> {
            return zonedDateTime;
        };
        MODES.put("ddd", day);
        MODES.put("dd", day);
    }

    private DatabaseFunctions() {
    }

    /**
     * Округления дат, аналог оракловой функции {@code TRUNC[ATE](Date, Mode)}.
     */
    public static Date myTrunc(final Date date, final String mode) {
        final Function<ZonedDateTime, ZonedDateTime> handler = MODES.get(mode.toLowerCase());
        if (handler == null) {
            throw new UnsupportedOperationException("Implement mode " + mode + " yourself");
        }

        final ZonedDateTime zonedDateTime = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .truncatedTo(ChronoUnit.DAYS);
        return Date.from(handler.apply(zonedDateTime).toInstant());
    }

    public static Date myTrunc(final Date date) {
        return myTrunc(date, "DD");
    }

    /**
     * Заглушка для оркаловой функции XMLTYPE.
     *
     * @param data строковый данные, которые должны парситься в XML.
     * @return строку, которая была передана в параметрах.
     */
    public static String toXMLType(final String data) {
        if (Strings.isNullOrEmpty(data)) {
            throw new IllegalArgumentException("XML data must not be empty");
        }
        return data;
    }

    /**
     * Функция для получения значения атрибута или элемента xml-документа по xpath(замена оракловой EXTRACTVALUE).
     *
     * @param xmlData         данные в xml формате,
     * @param xpathExpression xpath,
     * @return значение атрибута или элемента, соответствующего xpath.
     */
    public static String extractXMLValue(final String xmlData, final String xpathExpression) {
        if (Strings.isNullOrEmpty(xmlData)) {
            throw new IllegalArgumentException("XML data must not be empty");
        }
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlData.getBytes(StandardCharsets.UTF_8)));
            XPath xpath = XPathFactory.newInstance().newXPath();
            return xpath.evaluate(xpathExpression, document);
        } catch (SAXException | IOException | XPathExpressionException | ParserConfigurationException e) {
            throw new RuntimeException(
                    String.format("Failed to extract value. XML data: %s, xpath: %s", xmlData, xpathExpression),
                    e);
        }
    }

    public static double toNumber(String value) {
        return Double.parseDouble(value);
    }
}
