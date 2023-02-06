package ru.yandex.market.mbo.core.kdepot.saver;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

/**
 * @author amaslak
 */
public class OldestDateParserTest {

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testLocalDateTime() {
        List<String> testData = Arrays.asList(
            "2011-03-31",
            "31.03.2011",

            "2011-03-31 11:31:54.001",
            "2011-03-31 11:31:54.01",
            "2011-03-31 11:31:54.1",
            "2011-03-31 11:31:54",

            "31.03.2011 11:31:54",
            "31.03.2011 11:31:54.1",
            "31.03.2011 11:31:54.01",
            "31.03.2011 11:31:54.001",

            "1301556714100"
        );

        for (String date : testData) {
            LocalDateTime parsed = Helper.parseOldestDate(date);
            System.out.println(date + " => " + parsed + " => " + Helper.formatOldestDate(parsed));

            Assert.assertEquals(parsed.getYear(), 2011);
            Assert.assertEquals(parsed.getMonth(), Month.MARCH);
            Assert.assertEquals(parsed.getDayOfMonth(), 31);

            // accept both msk timezones and local timezone
            Assert.assertTrue(parsed.getHour() == 0
                || parsed.getHour() == 10
                || parsed.getHour() == 11
                || parsed.getHour() == parsed.atZone(ZoneId.systemDefault()).getHour());
            Assert.assertTrue(parsed.getMinute() == 31 || parsed.getMinute() == 0);
            Assert.assertTrue(parsed.getSecond() == 54 || parsed.getSecond() == 0);
        }
    }
}
