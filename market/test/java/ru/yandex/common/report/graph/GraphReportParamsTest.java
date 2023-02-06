package ru.yandex.common.report.graph;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.common.report.Const;
import ru.yandex.common.report.graph.model.GraphReportParams;
import ru.yandex.common.report.graph.model.timeperiod.PeriodManager;
import ru.yandex.common.util.parameters.ParametersSource;
import ru.yandex.common.util.parameters.ParametersSourceImpl;

/**
 * @author Sergey Simonchik ssimonchik@yandex-team.ru
 */
public class GraphReportParamsTest {

    private Date crDate(String source) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return sdf.parse(source);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testMinute1() throws ParseException {
        ParametersSource paramSource = new ParametersSourceImpl();
        paramSource.setParam(Const.GRAPH_REPORT__FROM, "2007-04-21 15:06:40");
        paramSource.setParam(Const.GRAPH_REPORT__TO, "2007-04-21 15:10:51");
        paramSource.setParam(Const.GRAPH_REPORT__PERIOD, "MI");

        GraphReportParams params = new GraphReportParams(paramSource);
        Map<String, Object> map = new HashMap<>();
        params.fillNamedParameterMap(map);
        Assert.assertEquals(crDate("2007-04-21 15:06:00"), map.get("from_date"));
        Assert.assertEquals(crDate("2007-04-21 15:11:00"), map.get("to_date"));
        Assert.assertEquals("MI", map.get("period"));
        Assert.assertEquals(1 / 1440.0, map.get("days_per_period"));
        Assert.assertEquals(5, map.get("period_count"));
    }

    @Test
    public void testMinute2() throws ParseException {
        ParametersSource paramSource = new ParametersSourceImpl();
        paramSource.setParam(Const.GRAPH_REPORT__FROM, "2007-03-10 17:16:59");
        paramSource.setParam(Const.GRAPH_REPORT__TO, "2007-04-21 15:10:00");
        paramSource.setParam(Const.GRAPH_REPORT__PERIOD, "MI");

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date date1 = df.parse("2007-03-10 17:16");
        Date date2 = df.parse("2007-04-21 15:11");
        final long expectedPointCount = (date2.getTime() - date1.getTime()) / DateUtils.MILLIS_PER_MINUTE;
        System.out.println("period-count = " + expectedPointCount);

        GraphReportParams params = new GraphReportParams(paramSource);
        Map<String, Object> map = new HashMap<>();
        params.fillNamedParameterMap(map);
        Assert.assertEquals(crDate("2007-03-10 17:16:00"), map.get("from_date"));
        Assert.assertEquals(crDate("2007-04-21 15:11:00"), map.get("to_date"));

        Assert.assertEquals(1 / 1440.0, map.get("days_per_period"));
        Assert.assertEquals((int) expectedPointCount, map.get("period_count"));
    }

    @Test
    public void testHour1() throws ParseException {
        ParametersSource paramSource = new ParametersSourceImpl();
        paramSource.setParam(Const.GRAPH_REPORT__FROM, "2007-04-21 10:16:59");
        paramSource.setParam(Const.GRAPH_REPORT__TO, "2007-04-21 17:10:00");
        paramSource.setParam(Const.GRAPH_REPORT__PERIOD, "HH24");

        GraphReportParams params = new GraphReportParams(paramSource);
        Map<String, Object> map = new HashMap<>();
        params.fillNamedParameterMap(map);
        Assert.assertEquals(crDate("2007-04-21 10:00:00"), map.get("from_date"));
        Assert.assertEquals(crDate("2007-04-21 18:00:00"), map.get("to_date"));
        Assert.assertEquals("HH24", map.get("period"));
        Assert.assertEquals(1 / 24.0, map.get("days_per_period"));
        Assert.assertEquals(8, map.get("period_count"));
    }

    @Test
    public void testHour2() throws ParseException {
        ParametersSource paramSource = new ParametersSourceImpl();
        paramSource.setParam(Const.GRAPH_REPORT__FROM, "2007-03-10 17:16:59");
        paramSource.setParam(Const.GRAPH_REPORT__TO, "2007-04-21 15:10:00");
        paramSource.setParam(Const.GRAPH_REPORT__PERIOD, "HH24");

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH");
        Date date1 = df.parse("2007-03-10 17");
        Date date2 = df.parse("2007-04-21 16");
        final long expectedPointCount = (date2.getTime() - date1.getTime()) / DateUtils.MILLIS_PER_HOUR;
        System.out.println("period-count = " + expectedPointCount);

        GraphReportParams params = new GraphReportParams(paramSource);
        Map<String, Object> map = new HashMap<>();
        params.fillNamedParameterMap(map);
        Assert.assertEquals(crDate("2007-03-10 17:00:00"), map.get("from_date"));
        Assert.assertEquals(crDate("2007-04-21 16:00:00"), map.get("to_date"));

        Assert.assertEquals(1 / 24.0, map.get("days_per_period"));
        Assert.assertEquals((int) expectedPointCount, map.get("period_count"));
    }

    @Test
    public void testDay1() throws ParseException {
        ParametersSource paramSource = new ParametersSourceImpl();
        paramSource.setParam(Const.GRAPH_REPORT__FROM, "2007-04-21 10:16:59");
        paramSource.setParam(Const.GRAPH_REPORT__TO, "2007-04-21 17:10:00");
        paramSource.setParam(Const.GRAPH_REPORT__PERIOD, "DD");

        GraphReportParams params = new GraphReportParams(paramSource);
        Map<String, Object> map = new HashMap<>();
        params.fillNamedParameterMap(map);
        Assert.assertEquals(crDate("2007-04-21 00:00:00"), map.get("from_date"));
        Assert.assertEquals(crDate("2007-04-22 00:00:00"), map.get("to_date"));
        Assert.assertEquals("DD", map.get("period"));
        Assert.assertEquals(1.0, map.get("days_per_period"));
        Assert.assertEquals(1, map.get("period_count"));
    }

    @Test
    public void testDay2() throws ParseException {
        ParametersSource paramSource = new ParametersSourceImpl();
        paramSource.setParam(Const.GRAPH_REPORT__FROM, "2007-03-10 17:16:59");
        paramSource.setParam(Const.GRAPH_REPORT__TO, "2007-04-21 15:10:00");
        paramSource.setParam(Const.GRAPH_REPORT__PERIOD, "DD");

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Date date1 = df.parse("2007-03-10");
        Date date2 = df.parse("2007-04-22");
        final long expectedPointCount = Math.round((date2.getTime() - date1.getTime()) / (double) PeriodManager.DAY_PERIOD.getMillisPerPeriod());
        System.out.println("point-count = " + expectedPointCount);

        GraphReportParams params = new GraphReportParams(paramSource);
        Map<String, Object> map = new HashMap<>();
        params.fillNamedParameterMap(map);
        Assert.assertEquals(crDate("2007-03-10 00:00:00"), map.get("from_date"));
        Assert.assertEquals(crDate("2007-04-22 00:00:00"), map.get("to_date"));

        Assert.assertEquals(1.0, map.get("days_per_period"));
        Assert.assertEquals((int) expectedPointCount, map.get("period_count"));
    }

    @Test
    public void testWeek1() throws ParseException {
        ParametersSource paramSource = new ParametersSourceImpl();
        paramSource.setParam(Const.GRAPH_REPORT__FROM, "2007-04-14 10:16:59");
        paramSource.setParam(Const.GRAPH_REPORT__TO, "2007-04-21 17:10:00");
        paramSource.setParam(Const.GRAPH_REPORT__PERIOD, "WW");

        GraphReportParams params = new GraphReportParams(paramSource);
        Map<String, Object> map = new HashMap<>();
        params.fillNamedParameterMap(map);
        Assert.assertEquals(crDate("2007-04-09 00:00:00"), map.get("from_date"));
        Assert.assertEquals(crDate("2007-04-23 00:00:00"), map.get("to_date"));
        Assert.assertEquals("WW", map.get("period"));
        Assert.assertEquals(7.0, map.get("days_per_period"));
        Assert.assertEquals(2, map.get("period_count"));
    }

    @Test
    public void testWeek2() throws ParseException {
        ParametersSource paramSource = new ParametersSourceImpl();
        paramSource.setParam(Const.GRAPH_REPORT__FROM, "2006-03-10 17:16:59");
        paramSource.setParam(Const.GRAPH_REPORT__TO, "2007-04-21 15:10:00");
        paramSource.setParam(Const.GRAPH_REPORT__PERIOD, "WW");

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Date date1 = df.parse("2006-03-06");
        Date date2 = df.parse("2007-04-22");
        final long expectedPointCount = Math.round((date2.getTime() - date1.getTime()) / (double) PeriodManager.WEEK_PERIOD.getMillisPerPeriod());
        System.out.println("point-count = " + expectedPointCount);

        GraphReportParams params = new GraphReportParams(paramSource);
        Map<String, Object> map = new HashMap<>();
        params.fillNamedParameterMap(map);
        Assert.assertEquals(crDate("2006-03-06 00:00:00"), map.get("from_date"));
        Assert.assertEquals(crDate("2007-04-23 00:00:00"), map.get("to_date"));

        Assert.assertEquals(7.0, map.get("days_per_period"));
        Assert.assertEquals((int) expectedPointCount, map.get("period_count"));
    }

    @Test
    public void testMonth1() throws ParseException {
        ParametersSource paramSource = new ParametersSourceImpl();
        paramSource.setParam(Const.GRAPH_REPORT__FROM, "2007-04-14 10:16:59");
        paramSource.setParam(Const.GRAPH_REPORT__TO, "2007-04-21 17:10:00");
        paramSource.setParam(Const.GRAPH_REPORT__PERIOD, "MM");

        GraphReportParams params = new GraphReportParams(paramSource);
        Map<String, Object> map = new HashMap<>();
        params.fillNamedParameterMap(map);
        Assert.assertEquals(crDate("2007-04-01 00:00:00"), map.get("from_date"));
        Assert.assertEquals(crDate("2007-05-01 00:00:00"), map.get("to_date"));
        Assert.assertEquals("MM", map.get("period"));
        Assert.assertEquals(1, map.get("period_count"));
    }

    @Test
    public void testMonth2() throws ParseException {
        ParametersSource paramSource = new ParametersSourceImpl();
        paramSource.setParam(Const.GRAPH_REPORT__FROM, "2006-04-14 10:16:59");
        paramSource.setParam(Const.GRAPH_REPORT__TO, "2007-04-21 17:10:00");
        paramSource.setParam(Const.GRAPH_REPORT__PERIOD, "MM");

        GraphReportParams params = new GraphReportParams(paramSource);
        Map<String, Object> map = new HashMap<>();
        params.fillNamedParameterMap(map);
        Assert.assertEquals(crDate("2006-04-01 00:00:00"), map.get("from_date"));
        Assert.assertEquals(crDate("2007-05-01 00:00:00"), map.get("to_date"));
    }

    @Test
    public void testMonth3() throws ParseException {
        ParametersSource paramSource = new ParametersSourceImpl();
        paramSource.setParam(Const.GRAPH_REPORT__FROM, "2007-03-24 15:15:04");
        paramSource.setParam(Const.GRAPH_REPORT__TO, "2007-04-23 16:15:04");
        paramSource.setParam(Const.GRAPH_REPORT__PERIOD, "MM");

        GraphReportParams params = new GraphReportParams(paramSource);
        Map<String, Object> map = new HashMap<>();
        params.fillNamedParameterMap(map);
        Assert.assertEquals(crDate("2007-03-01 00:00:00"), map.get("from_date"));
        Assert.assertEquals(crDate("2007-05-01 00:00:00"), map.get("to_date"));
        Assert.assertEquals(2, map.get("period_count"));
    }

    @Test
    public void testQuarter1() throws ParseException {
        ParametersSource paramSource = new ParametersSourceImpl();
        paramSource.setParam(Const.GRAPH_REPORT__FROM, "2007-04-14 10:16:59");
        paramSource.setParam(Const.GRAPH_REPORT__TO, "2007-04-21 17:10:00");
        paramSource.setParam(Const.GRAPH_REPORT__PERIOD, "Q");

        GraphReportParams params = new GraphReportParams(paramSource);
        Map<String, Object> map = new HashMap<>();
        params.fillNamedParameterMap(map);
        Assert.assertEquals(crDate("2007-04-01 00:00:00"), map.get("from_date"));
        Assert.assertEquals(crDate("2007-07-01 00:00:00"), map.get("to_date"));
        Assert.assertEquals("Q", map.get("period"));
        // Assert.assertEquals("90",
        // vr.getVariableExpression("days-per-period"));
        Assert.assertEquals(1, map.get("period_count"));
    }

    @Test
    public void testQuarter2() throws ParseException {
        ParametersSource paramSource = new ParametersSourceImpl();
        paramSource.setParam(Const.GRAPH_REPORT__FROM, "2006-04-14 10:16:59");
        paramSource.setParam(Const.GRAPH_REPORT__TO, "2007-04-21 17:10:00");
        paramSource.setParam(Const.GRAPH_REPORT__PERIOD, "Q");

        GraphReportParams params = new GraphReportParams(paramSource);
        Map<String, Object> map = new HashMap<>();
        params.fillNamedParameterMap(map);
        Assert.assertEquals(crDate("2006-04-01 00:00:00"), map.get("from_date"));
        Assert.assertEquals(crDate("2007-07-01 00:00:00"), map.get("to_date"));
    }

    @Test
    public void testYear1() throws ParseException {
        ParametersSource paramSource = new ParametersSourceImpl();
        paramSource.setParam(Const.GRAPH_REPORT__FROM, "2007-04-14 10:16:59");
        paramSource.setParam(Const.GRAPH_REPORT__TO, "2007-04-21 17:10:00");
        paramSource.setParam(Const.GRAPH_REPORT__PERIOD, "YYYY");

        GraphReportParams params = new GraphReportParams(paramSource);
        Map<String, Object> map = new HashMap<>();
        params.fillNamedParameterMap(map);
        Assert.assertEquals(crDate("2007-01-01 00:00:00"), map.get("from_date"));
        Assert.assertEquals(crDate("2008-01-01 00:00:00"), map.get("to_date"));
        Assert.assertEquals("YYYY", map.get("period"));
        Assert.assertEquals(365.0, map.get("days_per_period"));
        Assert.assertEquals(1, map.get("period_count"));
    }

    @Test
    public void testYear2() throws ParseException {
        ParametersSource paramSource = new ParametersSourceImpl();
        paramSource.setParam(Const.GRAPH_REPORT__FROM, "2006-04-14 10:16:59");
        paramSource.setParam(Const.GRAPH_REPORT__TO, "2007-04-21 17:10:00");
        paramSource.setParam(Const.GRAPH_REPORT__PERIOD, "YYYY");

        GraphReportParams params = new GraphReportParams(paramSource);
        Map<String, Object> map = new HashMap<>();
        params.fillNamedParameterMap(map);
        Assert.assertEquals(crDate("2006-01-01 00:00:00"), map.get("from_date"));
        Assert.assertEquals(crDate("2008-01-01 00:00:00"), map.get("to_date"));

        Assert.assertEquals(365.0, map.get("days_per_period"));
        Assert.assertEquals(2, map.get("period_count"));
    }

    @Test
    public void testMonthPeriodCount1() throws ParseException {
        ParametersSource paramSource = new ParametersSourceImpl();
        paramSource.setParam(Const.GRAPH_REPORT__TO, "2007-04-24");
        paramSource.setParam(Const.GRAPH_REPORT__PERIOD, "MM");
        paramSource.setParam(Const.GRAPH_REPORT__PERIOD_COUNT, "1");

        GraphReportParams params = new GraphReportParams(paramSource);
        Map<String, Object> map = new HashMap<>();
        params.fillNamedParameterMap(map);
        Assert.assertEquals(crDate("2007-04-01 00:00:00"), map.get("from_date"));
        Assert.assertEquals(crDate("2007-05-01 00:00:00"), map.get("to_date"));

        Assert.assertEquals(1, map.get("period_count"));
    }

    @Test
    public void testMonthPeriodCount2() throws ParseException {
        ParametersSource paramSource = new ParametersSourceImpl();
        paramSource.setParam(Const.GRAPH_REPORT__TO, "2007-04-24");
        paramSource.setParam(Const.GRAPH_REPORT__PERIOD, "MM");
        paramSource.setParam(Const.GRAPH_REPORT__PERIOD_COUNT, "2");

        GraphReportParams params = new GraphReportParams(paramSource);
        Map<String, Object> map = new HashMap<>();
        params.fillNamedParameterMap(map);
        Assert.assertEquals(crDate("2007-03-01 00:00:00"), map.get("from_date"));
        Assert.assertEquals(crDate("2007-05-01 00:00:00"), map.get("to_date"));

        Assert.assertEquals(2, map.get("period_count"));
    }

}
