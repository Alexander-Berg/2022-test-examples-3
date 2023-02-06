package ru.yandex.market.pers.grade.admin.base;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.common.framework.filter.TimeFilter;

/**
 * @author Sergey Simonchik ssimonchik@yandex-team.ru
 */
public class TimeFilterTest {
    @Test
    public void testMisc() {
        isAllPeriod(new TimeFilter(null, null, null, ""));
        isAllPeriod(new TimeFilter(null, null, "", null));
        isAllPeriod(new TimeFilter(null, null, "", ""));
    }

    private void isAllPeriod(TimeFilter timeFilter) {
        Assert.assertTrue(timeFilter.getFromDate().getTime() == 0);
        Assert.assertTrue(timeFilter.getToDate().getTime() >= System.currentTimeMillis());
    }

    @Test
    public void testTimeFilterYYYY_MM_DD_HH_MM_SS() {
        final String from = "2006-02-01 01:20:21";
        final String to = "2006-03-01 11:50:41";
        TimeFilter tf = new TimeFilter(null, null, from, to);
        Assert.assertEquals(from, tf.getFromDateAsString());
        Assert.assertEquals(to, tf.getToDateAsString());
    }

    @Test
    public void testTimeFilterYYYY_MM_DD_HH_MM() {
        TimeFilter tf = new TimeFilter(null, null, "2006-02-01 01:20", "2006-03-01 11:50");
        Assert.assertEquals("2006-02-01 01:20:00", tf.getFromDateAsString());
        Assert.assertEquals("2006-03-01 11:50:59", tf.getToDateAsString());
    }

    @Test
    public void testTimeFilterYYYY_MM_DD_HH() {
        TimeFilter tf = new TimeFilter(null, null, "2006-02-01 01", "2006-03-01 11");
        Assert.assertEquals("2006-02-01 01:00:00", tf.getFromDateAsString());
        Assert.assertEquals("2006-03-01 11:59:59", tf.getToDateAsString());
    }

    @Test
    public void testTimeFilterYYYY_MM_DD() {
        TimeFilter tf = new TimeFilter(null, null, "2006-02-01", "2006-03-01");
        Assert.assertEquals("2006-02-01 00:00:00", tf.getFromDateAsString());
        Assert.assertEquals("2006-03-01 23:59:59", tf.getToDateAsString());
    }

    @Test
    public void testTimeFilterYYYY_MM() {
        TimeFilter tf = new TimeFilter(null, null, "2006-02", "2006-03");
        Assert.assertEquals("2006-02-01 00:00:00", tf.getFromDateAsString());
        Assert.assertEquals("2006-03-31 23:59:59", tf.getToDateAsString());
    }

    @Test
    public void testTimeFilterYYYY() {
        TimeFilter tf = new TimeFilter(null, null, "2006", "2008");
        Assert.assertEquals("2006-01-01 00:00:00", tf.getFromDateAsString());
        Assert.assertEquals("2008-12-31 23:59:59", tf.getToDateAsString());
    }
}
