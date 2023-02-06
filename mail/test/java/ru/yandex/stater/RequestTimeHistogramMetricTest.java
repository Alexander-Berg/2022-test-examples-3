package ru.yandex.stater;

import java.io.StringReader;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.util.server.JsonStatsConsumer;
import ru.yandex.io.StringBuilderWriter;
import ru.yandex.json.writer.JsonType;
import ru.yandex.json.writer.JsonWriter;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.test.util.TestBase;

public class RequestTimeHistogramMetricTest extends TestBase {
    public RequestTimeHistogramMetricTest() {
        super(false, 0L);
    }

    private String stats(final RequestTimeHistogramMetric metric)
        throws Exception
    {
        StringBuilderWriter sbw = new StringBuilderWriter();
        try (JsonWriter writer = JsonType.NORMAL.create(sbw);
            JsonStatsConsumer statsConsumer = new JsonStatsConsumer(writer))
        {
            metric.stats(statsConsumer, 0);
        }
        return sbw.toString();
    }

    @Test
    public void testBackets() throws Exception {
        long start = 1234567890;

        String config =
            "precise-histogram = true\n"
            + "processing-time-stats = false\n"
            + "histogram-ranges = 0, 50, 100, 200, 500";
        RequestTimeHistogramMetric metric =
            new RequestTimeHistogramMetric(
                new IniConfig(new StringReader(config)));
        Assert.assertEquals(
            "[[\"-times-0ms_ammm\",0],"
            + "[\"-times-50ms_ammm\",0],"
            + "[\"-times-100ms_ammm\",0],"
            + "[\"-times-200ms_ammm\",0],"
            + "[\"-times-500ms_ammm\",0],"
            + "[\"-times-hist_ahhh\",[[0,0],[50,0],[100,0],[200,0],[500,0]]]]",
            stats(metric));

        // As per https://wiki.yandex-team.ru/golovan/userdocs/datatypes/
        // this should match backet with left bound = 100
        metric.accept(
            new RequestInfo(
                start + 150L,
                0,
                start,
                0L,
                0L,
                0L));
        Assert.assertEquals(
            "[[\"-times-0ms_ammm\",0],"
            + "[\"-times-50ms_ammm\",0],"
            + "[\"-times-100ms_ammm\",0],"
            + "[\"-times-200ms_ammm\",1],"
            + "[\"-times-500ms_ammm\",1],"
            + "[\"-times-hist_ahhh\",[[0,0],[50,0],[100,1],[200,0],[500,0]]]]",
            stats(metric));

        // Precise histogram won't account this signal,
        // but -times-hist_ahhh_ahhh and -reqstats-total_ammm will do
        metric.accept(
            new RequestInfo(
                start + 700L,
                0,
                start,
                0L,
                0L,
                0L));
        Assert.assertEquals(
            "[[\"-times-0ms_ammm\",0],"
            + "[\"-times-50ms_ammm\",0],"
            + "[\"-times-100ms_ammm\",0],"
            + "[\"-times-200ms_ammm\",1],"
            + "[\"-times-500ms_ammm\",1],"
            + "[\"-times-hist_ahhh\",[[0,0],[50,0],[100,1],[200,0],[500,1]]]]",
            stats(metric));

        metric.accept(
            new RequestInfo(
                start + 1500L,
                0,
                start,
                0L,
                0L,
                0L));
        Assert.assertEquals(
            "[[\"-times-0ms_ammm\",0],"
            + "[\"-times-50ms_ammm\",0],"
            + "[\"-times-100ms_ammm\",0],"
            + "[\"-times-200ms_ammm\",1],"
            + "[\"-times-500ms_ammm\",1],"
            + "[\"-times-hist_ahhh\",[[0,0],[50,0],[100,1],[200,0],[500,2]]]]",
            stats(metric));

        metric.accept(
            new RequestInfo(
                start,
                0,
                start,
                0L,
                0L,
                0L));
        Assert.assertEquals(
            "[[\"-times-0ms_ammm\",1],"
            + "[\"-times-50ms_ammm\",1],"
            + "[\"-times-100ms_ammm\",1],"
            + "[\"-times-200ms_ammm\",2],"
            + "[\"-times-500ms_ammm\",2],"
            + "[\"-times-hist_ahhh\",[[0,1],[50,0],[100,1],[200,0],[500,2]]]]",
            stats(metric));

        metric.accept(
            new RequestInfo(
                start + 10L,
                0,
                start,
                0L,
                0L,
                0L));
        Assert.assertEquals(
            "[[\"-times-0ms_ammm\",1],"
            + "[\"-times-50ms_ammm\",2],"
            + "[\"-times-100ms_ammm\",2],"
            + "[\"-times-200ms_ammm\",3],"
            + "[\"-times-500ms_ammm\",3],"
            + "[\"-times-hist_ahhh\",[[0,2],[50,0],[100,1],[200,0],[500,2]]]]",
            stats(metric));

        metric.accept(
            new RequestInfo(
                start + 60L,
                0,
                start,
                0L,
                0L,
                0L));
        Assert.assertEquals(
            "[[\"-times-0ms_ammm\",1],"
            + "[\"-times-50ms_ammm\",2],"
            + "[\"-times-100ms_ammm\",3],"
            + "[\"-times-200ms_ammm\",4],"
            + "[\"-times-500ms_ammm\",4],"
            + "[\"-times-hist_ahhh\",[[0,2],[50,1],[100,1],[200,0],[500,2]]]]",
            stats(metric));

        metric.accept(
            new RequestInfo(
                start + 50L,
                0,
                start,
                0L,
                0L,
                0L));
        Assert.assertEquals(
            "[[\"-times-0ms_ammm\",1],"
            + "[\"-times-50ms_ammm\",3],"
            + "[\"-times-100ms_ammm\",4],"
            + "[\"-times-200ms_ammm\",5],"
            + "[\"-times-500ms_ammm\",5],"
            + "[\"-times-hist_ahhh\",[[0,3],[50,1],[100,1],[200,0],[500,2]]]]",
            stats(metric));
    }
}

