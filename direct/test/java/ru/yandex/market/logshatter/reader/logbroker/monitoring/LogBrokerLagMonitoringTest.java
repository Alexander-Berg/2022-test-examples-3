package ru.yandex.market.logshatter.reader.logbroker.monitoring;

import com.github.fakemongo.junit.FongoRule;
import com.google.common.collect.LinkedHashMultimap;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.market.logbroker.pull.LogBrokerOffset;
import ru.yandex.market.logshatter.LogShatterMonitoring;
import ru.yandex.market.logshatter.reader.logbroker.LogbrokerSource;
import ru.yandex.market.logshatter.reader.logbroker.MonitoringLagThreshold;
import ru.yandex.market.logshatter.reader.logbroker.PartitionDao;
import ru.yandex.market.monitoring.MonitoringStatus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.monitoring.MonitoringStatus.CRITICAL;
import static ru.yandex.market.monitoring.MonitoringStatus.OK;
import static ru.yandex.market.monitoring.MonitoringStatus.WARNING;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 17.09.2018
 */
public class LogBrokerLagMonitoringTest {
    @Rule
    public FongoRule fongoRule = new FongoRule();

    private final LinkedHashMultimap<String, LogbrokerSource> identToSources = LinkedHashMultimap.create();
    private final Map<String, MonitoringLagThreshold> identToLagThreshold = new HashMap<>();


    @Test
    public void noOffsetsInDatabase() {
        givenOffsetsInDatabase();
        assertThatLagMonitoringIs(CRITICAL);
    }


    @Test
    public void onePartitionWithOkLag() {
        givenLogBrokerSource("ident1");
        givenOffsetsInDatabase(offset("rt3.vla--ident1--logType1:123", 90, 0, 100, 9));
        assertThatLagMonitoringIs(OK);
    }

    @Test
    public void onePartitionWithWarnLag() {
        givenLogBrokerSource("ident1");
        givenOffsetsInDatabase(offset("rt3.vla--ident1--logType1:123", 70, 0, 100, 29));
        assertThatLagMonitoringIs(WARNING);
    }

    @Test
    public void onePartitionWithCritLag() {
        givenLogBrokerSource("ident1");
        givenOffsetsInDatabase(offset("rt3.vla--ident1--logType1:123", 40, 0, 100, 59));
        assertThatLagMonitoringIs(CRITICAL);
    }


    @Test
    public void threeIdentsWithOkAndWarnAndCrit() {
        givenLogBrokerSource("ident1");
        givenLogBrokerSource("ident2");
        givenLogBrokerSource("ident3");
        givenOffsetsInDatabase(
            offset("rt3.vla--ident1--logType1:123", 90, 0, 100, 9),
            offset("rt3.vla--ident2--logType1:123", 70, 0, 100, 29),
            offset("rt3.vla--ident3--logType1:123", 40, 0, 100, 59)
        );
        assertThatLagMonitoringIs(CRITICAL);
    }


    @Test
    public void identWithTwoPartitionsOk() {
        givenLogBrokerSource("ident1");
        givenOffsetsInDatabase(
            offset("rt3.vla--ident1--logType1:123", 20, 0, 30, 9),
            offset("rt3.vla--ident1--logType1:456", 69, 0, 70, 0)
        );
        assertThatLagMonitoringIs(OK);
    }

    @Test
    public void identWithTwoPartitionsWarn() {
        givenLogBrokerSource("ident1");
        givenOffsetsInDatabase(
            offset("rt3.vla--ident1--logType1:123", 0, 0, 30, 29),
            offset("rt3.vla--ident1--logType1:456", 69, 0, 70, 0)
        );
        assertThatLagMonitoringIs(WARNING);
    }

    @Test
    public void identWithTwoPartitionsCrit() {
        givenLogBrokerSource("ident1");
        givenOffsetsInDatabase(
            offset("rt3.vla--ident1--logType1:123", 20, 0, 30, 9),
            offset("rt3.vla--ident1--logType1:456", 20, 0, 70, 49)
        );
        assertThatLagMonitoringIs(CRITICAL);
    }


    @Test  // MARKETINFRA-1430
    public void partitionWithOffsetLessThanLogStart() {
        givenLogBrokerSource("ident1");
        givenLagThreshold("ident1", 99, 101);
        givenOffsetsInDatabase(
            offset("rt3.vla--ident1--logType1:123", 20, 200, 300, 100)
        );
        assertThatLagMonitoringIs(WARNING);  // Должен получиться лаг 100%
    }


    @Test
    public void partitionThatWeAreNotReading() {
        givenLogBrokerSource("ident1", "logType1");
        givenLogBrokerSource("ident1", "logType2");
        givenOffsetsInDatabase(
            offset("rt3.vla--ident1--logType1:123", 9, 0, 10, 0),
            offset("rt3.vla--ident1--logType2:123", 9, 0, 10, 0),
            offset("rt3.vla--ident1--unknownLogType:123", -1, 0, 80, 80)
        );
        assertThatLagMonitoringIs(OK);
    }


    @Test
    public void partitionWithOneReadMessage() {
        givenLogBrokerSource("ident1");
        givenOffsetsInDatabase(offset("rt3.vla--ident1--logType1:123", 0, 0, 1, 0));
        assertThatLagMonitoringIs(OK);
    }


    @Test
    public void identWithoutOffsets() {
        givenLogBrokerSource("ident1");
        givenOffsetsInDatabase(
            offset("rt3.vla--notIdent1--logType1:123", 90, 0, 100, 9)
        );
        assertThatLagMonitoringIs(CRITICAL);
    }


    // TODO исключение - crit
    // TODO потестить получше anyLogType


    private void givenLogBrokerSource(String ident) {
        identToSources.put(ident, new LogbrokerSource(ident));
    }

    private void givenLogBrokerSource(String ident, String logType) {
        identToSources.put(ident, new LogbrokerSource(ident, logType));
    }

    private void givenLagThreshold(String ident, double warningPercent, double criticalPercent) {
        identToLagThreshold.put(ident, new MonitoringLagThreshold(warningPercent, criticalPercent));
    }

    private void givenOffsetsInDatabase(LogBrokerOffset... offsets) {
        Arrays.stream(offsets).forEach(new PartitionDao(fongoRule.getDatabase())::save);
    }

    private static LogBrokerOffset offset(String partition, long offset, long logStart, long logSize, long lag) {
        assertTrue(
            (lag == logSize - offset - 1)
                || (offset < logStart && lag == logSize - logStart)
        );

        return new LogBrokerOffset(
            partition,
            offset,
            logStart,
            logSize,
            lag,
            null,
            null
        );
    }

    private void assertThatLagMonitoringIs(MonitoringStatus expected) {
        LogShatterMonitoring monitoring = new LogShatterMonitoring();
        run(monitoring);
        assertEquals(expected, monitoring.getClusterCritical().getResult().getStatus());
    }

    private void run(LogShatterMonitoring monitoring) {
        new LogBrokerLagMonitoring(
            new PartitionDao(fongoRule.getDatabase()),
            new MonitoringConfig(
                monitoring,
                new MonitoringLagThreshold(25, 50),
                identToLagThreshold,
                0,
                0
            ),
            monitoring,
            identToSources
        )
            .run();
    }
}
