package ru.yandex.market.bidding.engine.tasks;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.TimeZone;

import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.bidding.engine.statistics.AuctionPerformanceStatistics;
import ru.yandex.market.bidding.model.Place;
import ru.yandex.market.metrics.TimeLineHistogram;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: snoop
 * Date: 8/18/15
 * Time: 8:31 PM
 */
public class AuctionPerformanceReportTest {

    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Europe/Moscow");

    @Before
    public void setUp() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone(DEFAULT_ZONE_ID));
    }

    @Test
    public void testWrite() throws Exception {
        StringWriter sw = new StringWriter();
        AuctionPerformanceReport report = new AuctionPerformanceReport();
        AuctionPerformanceStatistics aps = mock(AuctionPerformanceStatistics.class);
        final AuctionPerformanceStatistics.Count totalCount = count(10);
        when(aps.total()).thenReturn(totalCount);
        final AuctionPerformanceStatistics.Count appliedCount = count(5);
        when(aps.applied()).thenReturn(appliedCount);
        final AuctionPerformanceStatistics.Count failedCount = count(2);
        when(aps.failed()).thenReturn(failedCount);
        final AuctionPerformanceStatistics.Count modifiedCount = count(3);
        when(aps.modified()).thenReturn(modifiedCount);
        final TimeLineHistogram histogram = histogram(Arrays.asList(Pair.of(5, 10), Pair.of(7, 20)));
        when(aps.histogram(true, Place.SEARCH)).thenReturn(histogram);
        final TimeLineHistogram effectiveHistogram = histogram(Arrays.asList(Pair.of(5, 3), Pair.of(7, 4)));
        when(aps.histogramEffective(true, Place.SEARCH)).thenReturn(effectiveHistogram);
        final TimeLineHistogram gradualHistogram = histogram(Arrays.asList(Pair.of(15, 10), Pair.of(17, 20)));
        when(aps.histogramGradient(true, Place.SEARCH)).thenReturn(gradualHistogram);
        final TimeLineHistogram empty = empty();
        for (Place place : EnumSet.complementOf(EnumSet.of(Place.SEARCH))) {
            when(aps.histogram(true, place)).thenReturn(empty);
            when(aps.histogramEffective(true, place)).thenReturn(empty);
            when(aps.histogramGradient(true, place)).thenReturn(empty);
        }
        for (Place place : EnumSet.allOf(Place.class)) {
            when(aps.histogram(false, place)).thenReturn(empty);
            when(aps.histogramEffective(false, place)).thenReturn(empty);
            when(aps.histogramGradient(false, place)).thenReturn(empty);
        }
        report.write(sw, aps, LocalDateTime.of(2015, Month.AUGUST, 10, 17, 45, 05));
        String text = getResourceText("report_kpi");
        assertEquals(text, sw.toString());
    }

    private String getResourceText(String report) throws IOException {
        final URL url = Resources.getResource(AuctionPerformanceReportTest.class, report + ".messages");
        return Resources.toString(url, Charset.forName("UTF-8"));
    }

    private TimeLineHistogram empty() {
        return histogram(Collections.emptyList());
    }

    private TimeLineHistogram histogram(List<Pair<Integer, Integer>> data) {
        TimeLineHistogram tld = mock(TimeLineHistogram.class);
        when(tld.result(false)).thenReturn(data);
        return tld;
    }

    private AuctionPerformanceStatistics.Count count(int value) {
        AuctionPerformanceStatistics.Count count = mock(AuctionPerformanceStatistics.Count.class);
        when(count.value()).thenReturn(value);
        return count;
    }
}
