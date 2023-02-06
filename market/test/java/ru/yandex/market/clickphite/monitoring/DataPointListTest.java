package ru.yandex.market.clickphite.monitoring;

import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Iterables;
import org.junit.Test;

import ru.yandex.market.health.configs.clickphite.monitoring.DataPoint;
import ru.yandex.market.health.configs.clickphite.monitoring.DataPointList;
import ru.yandex.market.health.configs.clickphite.monitoring.MonitoringStatusAndCause;
import ru.yandex.market.monitoring.MonitoringStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 10.10.16
 */
public class DataPointListTest {
    private static final int TIME_STAMP_SECONDS = 1475585400;
    private static final int PERIOD_SECONDS = 300;

    @Test
    public void addOneCriticalValue() throws Exception {
        DataPointList sut = new DataPointList(120, PERIOD_SECONDS);

        sut.addValue(TIME_STAMP_SECONDS, 0).setStatus(DataPoint.Status.CRITICAL);

        MonitoringStatusAndCause status = sut.getStatus(10, -1, TIME_STAMP_SECONDS);
        assertEquals(MonitoringStatus.CRITICAL, status.getStatus());
        List<DataPoint> dataPoints = status.getDataPoints();

        assertNotNull(dataPoints);
        assertEquals(1, dataPoints.size());

        DataPoint dataPoint = Iterables.getOnlyElement(dataPoints);
        assertEquals(TIME_STAMP_SECONDS, dataPoint.getTimestampSeconds());
        assertEquals(0, dataPoint.getValue(), 0.001d);
    }

    @Test
    public void addThreeWarnValues() throws Exception {
        DataPointList sut = new DataPointList(120, PERIOD_SECONDS);

        sut.addValue(TIME_STAMP_SECONDS, 0).setStatus(DataPoint.Status.OK);
        sut.addValue(TIME_STAMP_SECONDS + PERIOD_SECONDS, 0).setStatus(DataPoint.Status.WARN);
        sut.addValue(TIME_STAMP_SECONDS + 2 * PERIOD_SECONDS, 0).setStatus(DataPoint.Status.WARN);

        int maxTimestampSeconds = TIME_STAMP_SECONDS + 3 * PERIOD_SECONDS;
        sut.addValue(maxTimestampSeconds, 0).setStatus(DataPoint.Status.WARN);

        int warnsToCrit = 3;
        MonitoringStatusAndCause statusAndCause = sut.getStatus(10, warnsToCrit, maxTimestampSeconds);
        MonitoringStatus status = statusAndCause.getStatus();
        assertEquals(MonitoringStatus.CRITICAL, status);

        List<DataPoint> dataPoints = statusAndCause.getDataPoints();
        dataPoints.sort(Comparator.comparing(DataPoint::getTimestampSeconds));

        assertNotNull(dataPoints);
        assertEquals(3, dataPoints.size());

        DataPoint dataPoint = dataPoints.get(0);
        assertEquals(TIME_STAMP_SECONDS + PERIOD_SECONDS, dataPoint.getTimestampSeconds());
        assertEquals(0, dataPoint.getValue(), 0.001d);
    }

}
