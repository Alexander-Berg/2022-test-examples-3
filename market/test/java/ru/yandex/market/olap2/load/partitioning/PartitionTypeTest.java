package ru.yandex.market.olap2.load.partitioning;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PartitionTypeTest {


    private Date dateOf28Februry = date(2020, 2, 28);
    private Date date1stFebruary = date(2020, 2, 1);
    private Date dateOf29Februry = date(2020, 2, 29);


    @Test
    public void testNonePartition() throws ParseException {
        assertNull("Wrong int partition!", PartitionType.NONE.toIntPartition(dateOf28Februry));
        assertEquals("Wrong partition type!", PartitionType.detectType(null), PartitionType.NONE);

        assertNull("Wrong none partition!", PartitionType.NONE.toPartitionEnd(20200228));
        assertTrue("Should be partition start!",
                PartitionType.NONE.isPartitionStart(dateOf28Februry));
        assertNull("Wrong date!", PartitionType.NONE.getDateFrom(20200228));
    }


    @Test
    public void testDailyPartition() throws ParseException {
        assertThat("Wrong int partition!",
                PartitionType.DAY.toIntPartition(dateOf28Februry), is(20200228));
        assertEquals("Wrong partition!", PartitionType.hyphenate(20200228), "2020-02-28");
        assertEquals("Wrong partition type!", PartitionType.detectType(20200228), PartitionType.DAY);

        assertEquals("Wrong day partition!",
                PartitionType.DAY.toPartitionEnd(20200228), dateOf28Februry);
        assertTrue("Should be partition start!",
                PartitionType.DAY.isPartitionStart(dateOf28Februry));
        assertEquals("Wrong date!",
                PartitionType.DAY.getDateFrom(20200228), dateOf28Februry);
    }

    @Test
    public void testMonthlyPartition() throws ParseException {
        assertThat("Wrong int partition!",
                PartitionType.MONTH.toIntPartition(dateOf28Februry), is(202002));
        assertEquals("Wrong partition!", PartitionType.hyphenate(202002), "2020-02");
        assertEquals("Wrong partition type!", PartitionType.detectType(202002), PartitionType.MONTH);

        assertEquals("Wrong month partition!",
                PartitionType.MONTH.toPartitionEnd(202002), dateOf29Februry);
        assertFalse("Should not be partition start!",
                PartitionType.MONTH.isPartitionStart(dateOf28Februry));
        assertTrue("Should be partition start!",
                PartitionType.MONTH.isPartitionStart(date1stFebruary));
        assertEquals("Wrong date!",
                PartitionType.MONTH.getDateFrom(202002), date1stFebruary);
    }


    @Test
    public void testYearlyPartition() throws ParseException {
        assertThat("Wrong int partition!",
                PartitionType.YEAR.toIntPartition(dateOf28Februry), is(2020));
        assertEquals("Wrong partition!", PartitionType.hyphenate(2020), "2020");
        assertEquals("Wrong partition type!", PartitionType.detectType(2020), PartitionType.YEAR);

        assertEquals("Wrong month partition!",
                PartitionType.YEAR.toPartitionEnd(2020), date(2020, 12, 31));
        assertFalse("Should not be partition start!",
                PartitionType.YEAR.isPartitionStart(dateOf28Februry));
        assertTrue("Should be partition start!",
                PartitionType.YEAR.isPartitionStart(date(2020, 1, 1)));
        assertEquals("Wrong date!",
                PartitionType.YEAR.getDateFrom(2020), date(2020, 1, 1));
    }


    @Test
    public void testMonthQuarterPartition() throws ParseException {
        assertThat("Wrong int partition!",
                PartitionType.MONTH_QUARTER.toIntPartition(dateOf28Februry), is(2020024));
        assertEquals("Wrong partition!", PartitionType.hyphenate(2020024), "2020-02-4");
        assertEquals("Wrong partition type!", PartitionType.detectType(2020024), PartitionType.MONTH_QUARTER);

        assertEquals("Wrong month partition!",
                PartitionType.MONTH_QUARTER.toPartitionEnd(2020024),
                dateOf29Februry);
        assertFalse("Should not be partition start!",
                PartitionType.MONTH_QUARTER.isPartitionStart(dateOf28Februry));
        assertTrue("Should be partition start!",
                PartitionType.MONTH_QUARTER.isPartitionStart(date(2020, 2, 25)));

        assertEquals("Wrong date!",
                PartitionType.MONTH_QUARTER.getDateFrom(2020024), date(2020, 2, 29));


        assertEquals("Wrong partition!", PartitionType.hyphenate(2020024), "2020-02-4");
        assertEquals("Wrong partition type!", PartitionType.detectType(2020024), PartitionType.MONTH_QUARTER);
    }

    @Test
    public void testMQPartitionNum() {
        assertThat(PartitionType.getMQPartitionNum(LocalDate.of(2020,2, 1)), is(1));
        assertThat(PartitionType.getMQPartitionNum(LocalDate.of(2020,2, 8)), is(1));
        assertThat(PartitionType.getMQPartitionNum(LocalDate.of(2020,2, 9)), is(2));
        assertThat(PartitionType.getMQPartitionNum(LocalDate.of(2020,2, 16)), is(2));
        assertThat(PartitionType.getMQPartitionNum(LocalDate.of(2020,2, 17)), is(3));
        assertThat(PartitionType.getMQPartitionNum(LocalDate.of(2020,2, 24)), is(3));
        assertThat(PartitionType.getMQPartitionNum(LocalDate.of(2020,2, 25)), is(4));
        assertThat(PartitionType.getMQPartitionNum(LocalDate.of(2020,2, 28)), is(4));
        assertThat(PartitionType.getMQPartitionNum(LocalDate.of(2020,2, 29)), is(4));
        assertThat(PartitionType.getMQPartitionNum(LocalDate.of(2020,1, 30)), is(4));
        assertThat(PartitionType.getMQPartitionNum(LocalDate.of(2020,1, 31)), is(4));
    }

    private Date date(int y, int m, int d) {
        return Date.from(LocalDate.of(y, m, d).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
