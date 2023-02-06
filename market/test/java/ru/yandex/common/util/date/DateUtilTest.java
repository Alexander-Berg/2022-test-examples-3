/**
 * Created by IntelliJ IDEA.
 * User: AShevenkov
 * Date: 10.12.2007
 * Time: 20:34:30
 * To change this template use File | Settings | File Templates.
 */
package ru.yandex.common.util.date;

import junit.framework.TestCase;
import org.apache.commons.collections.ListUtils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author ashevenkov
 */
public class DateUtilTest extends TestCase {

    public void testBetween() throws Exception {
        Date from = DateUtil.DOT_DATE_FORMAT.parse("01.03.2007");
        Date to = DateUtil.DOT_DATE_FORMAT.parse("03.03.2007");
        List<Date> db = DateUtil.getDaysBetween(from, to);
        assertEquals(db.size(), 3);
        ListUtils.isEqualList(db, Arrays.asList(from, DateUtil.DOT_DATE_FORMAT.parse("02.03.2007"), to));
    }

    public void testTruncQuater() throws Exception {
        Date date = DateUtil.truncDayByQuaterHour(new SimpleDateFormat("dd.mm.yyyy HH:mm:ss").parse("01.01.2008 05:17:20"));
        assertEquals(date, new SimpleDateFormat("dd.mm.yyyy HH:mm:ss").parse("01.01.2008 05:15:00"));
    }
}
