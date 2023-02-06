package ru.yandex.market.mstat.planner.controller.rest;

import com.google.common.collect.ImmutableMap;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.temporal.IsoFields;
import java.util.Date;
import java.util.HashMap;

import static org.junit.Assert.*;

public class RequestControllerTest {
//    @Test
//    public void testJobSizeToPeriod() {
//        test("1d");
//        test("2d");
//        test("7d");
//        test("30d");
//        test("1w");
//        test("2w");
//        test("4w");
//        test("1m");
//        test("12m");
//        test("1q");
//        test("4q");
//
//    }
//
//    private void test(String t) {
//        System.out.println(t + "\t" + RequestController.jobSizeToPeriod(t));
//    }
//
//
//    @Test
//    public void testGetEndDate() {
//        // new Date(118, 11, 01) == 2018-12-01
//        testD("2018-12-08", new Date(118, 11, 01), "1w", 1.0);
//        testD("2018-12-04", new Date(118, 11, 01), "1w", 0.5);
//        testD("2018-12-16", new Date(118, 11, 01), "20d", 0.75);
//        testD("2019-01-01", new Date(118, 11, 01), "2m", 0.5);
//        testD("2019-03-01", new Date(118, 11, 01), "1q", 1.0);
//    }
//
//    private void testD(String expected, Date d, String jobSize, double load) {
//        String actual = RequestController.calcDateEnd(d, jobSize, BigDecimal.valueOf(load)).toString();
//        assertEquals(expected, actual);
//    }

    @Test
    public void testGetQ() {
        System.out.println(new java.sql.Date(118, 11, 5).toLocalDate().get(IsoFields.QUARTER_OF_YEAR));
    }

    @Test
    @Ignore
    public void t() {
        HashMap m = new HashMap();
        m.put("a", 1);
        m.put("b", 2);
        m.put("a", 3);
        System.out.println(m);
        new HashMap(m);

        System.out.println(ImmutableMap.builder()
            .putAll(ImmutableMap.of("a", 1, "b", 2))
            .put("a", 3)
            .build());
    }
}
