package ru.yandex.market.shared.fields.types;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.robot.shared.fields.FieldsInfo;
import ru.yandex.market.robot.shared.fields.types.DateFieldType;

/**
 * @author Dmitriy Kotelnikov <a href="mailto:kotelnikov@yandex-team.ru"/>
 * @date 19.12.11
 */
public class DateFieldTypeTest extends Assert {
    private final DateFieldType dateFieldType =
        new DateFieldType(FieldsInfo.DateType.DATE_TIME);

    @Test
    public void testParse() throws Exception {

        assertEquals(
            "0.0.0 0:50:0",
            dateFieldType.parse(" 0050 ").toString()
        );
        assertEquals(
            "0.0.0 0:5:0",
            dateFieldType.parse(" 0005 ").toString()
        );
        assertEquals(
            "0.0.0 5:45:0",
            dateFieldType.parse("0​5​:4​5​ ч.").toString()
        );
        assertEquals(
            "8.6.2012 20:59:0",
            dateFieldType.parse("20:59 08-06-2012").toString()
        );
        assertEquals(
            "6.3.2012 16:31:0",
            dateFieldType.parse("16:31 | 06 Мар 12").toString()
        );
        assertEquals(
            "2.3.0 0:0:0",
            dateFieldType.parse("2 марта").toString()
        );
        assertEquals(
            "17.2.2012 0:0:0",
            dateFieldType.parse("Дата публикации: 17 февраля 2012").toString()
        );
        assertEquals(
            "9.1.2012 0:0:0",
            dateFieldType.parse("09 января 2012").toString()
        );
        assertEquals(
            "16.12.2011 10:41:9",
            dateFieldType.parse("алалал 16.12.11 10:41:09").toString()
        );
        assertEquals(
            "16.12.2011 0:0:0",
            dateFieldType.parse("лалалал 16.12.11").toString()
        );
        assertEquals(
            "16.3.2011 0:0:0",
            dateFieldType.parse("ааа 16 марта 2011 ббб").toString()
        );
        assertEquals(
            "16.12.2011 14:48:0",
            dateFieldType.parse("14:48 16.12.11", "").toString()
        );
        assertEquals(
            "16.12.1998 14:48:0",
            dateFieldType.parse("14:48 16.12.98", "").toString()
        );
        assertEquals(
            "0.5.2010 0:0:0",
            dateFieldType.parse("май 2010", "").toString()
        );
        assertEquals(
            "0.2.2010 0:0:0",
            dateFieldType.parse("02 2010", "").toString()
        );
        assertEquals(
            "18.4.2009 16:10:23",
            dateFieldType.parse("2009-04-18 16:10:23 ", "").toString()
        );
        assertEquals(
            "28.2.0 0:0:0",
            dateFieldType.parse("Feb 28", "").toString()
        );
        assertEquals(
            "12.3.0 0:0:0",
            dateFieldType.parse("Mar 12", "").toString()
        );
        assertEquals(
            "0.0.0 19:5:0",
            dateFieldType.parse(" 1905 ", "").toString()
        );
        assertEquals(
            "0.0.0 18:30:0",
            dateFieldType.parse(" 630p ", "").toString()
        );
        assertEquals(
            "0.0.0 6:30:0",
            dateFieldType.parse(" 630a ", "").toString()
        );
        assertEquals(
            "26.8.2012 0:0:0",
            dateFieldType.parse(" 26.8.2012 0:0:0 ", "").toString()
        );
        assertEquals(
            "17.11.2016 9:10:0",
            dateFieldType.parse("Dubai Intl Airport Terminal 2 09:10 17 November 2016", "").toString()
        );
    }
}
