package ru.yandex.market.mbo.core.dashboard;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.common.util.monitoring.MonitoringMessage;
import ru.yandex.common.util.monitoring.MonitoringStatus;
import ru.yandex.market.mbo.export.dumpstorage.DumpStorageInfoService;
import ru.yandex.market.mbo.gwt.models.dashboard.DumpGroupData;
import ru.yandex.market.mbo.gwt.models.dashboard.DumpGroupData.Status;
import ru.yandex.market.mbo.gwt.models.dashboard.DumpGroupData.Type;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ru.yandex.market.mbo.core.dashboard.DumpGroupStat.getLastSuccessTimeError;
import static ru.yandex.market.mbo.core.dashboard.DumpGroupStat.getLastSuccessTimeThreshold;

@SuppressWarnings("checkstyle:magicnumber")
public class DumpGroupStatTest {
    private final Map<Type, DumpGroupStat.Stat> statMap = DumpGroupStat.analyze(Arrays.asList(
        dump(1, Type.STUFF, Status.OK, 100, 0),
        build(dump(2, Type.STUFF, Status.FAILED, 110, 10), d -> d.setRestarted(true)),
        dump(3, Type.CLUSTERS, Status.FAILED, 100, 0),
        dump(4, Type.CLUSTERS, Status.FAILED, 100, 10),
        dump(5, Type.CLUSTERS, Status.OK, 100, 20),
        dump(8, Type.FAST, Status.OK_BUT_SOME_FAILED, 100, 0),
        dump(9, Type.FAST, Status.OK_BUT_SOME_FAILED, 100, 10)
    ));

    private static final Type[] ALL_TYPES = new Type[]{Type.STUFF, Type.CLUSTERS, Type.FAST};

    private static final Instant NOW = Instant.now();

    private DumpHistoryDaoMock dao;
    private DumpStorageInfoService dumpStorageInfoService;
    private DumpGroupStat stat;

    @Before
    public void setUp() {
        dao = new DumpHistoryDaoMock();
        dumpStorageInfoService = Mockito.mock(DumpStorageInfoService.class);
        stat = new DumpGroupStat(dao, dumpStorageInfoService);
    }

    @Test
    public void testRestartsNotCounted() {
        assertEquals(0, statMap.get(Type.STUFF).getConsecutiveFails().size());
    }

    @Test
    public void testFailsNotCountedInMedianTime() {
        assertEquals(100.0, statMap.get(Type.STUFF).getBaseTime(), 0.0001);
    }

    @Test
    public void testConsecutiveFails() {
        assertEquals(0, statMap.get(Type.CLUSTERS).getConsecutiveFails().size());
        assertEquals(2, statMap.get(Type.FAST).getConsecutiveFails().size());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testLastSuccess() {
        assertEquals("session-5", statMap.get(Type.CLUSTERS).getLastSuccess().getSessionName());
        assertNull(statMap.get(Type.FAST).getLastSuccess());
    }

    @Test
    public void testStat() {
        assertEquals(2, statMap.get(Type.CLUSTERS).getCountByStatus().get(Status.FAILED).intValue());
        assertEquals(1, statMap.get(Type.CLUSTERS).getCountByStatus().get(Status.OK).intValue());
    }

    @Test
    public void testExportsCount() {
        assertEquals(3, statMap.get(Type.CLUSTERS).getExportsCount());
    }

    @Test
    public void testFormatting() {
        MonitoringMessage message = DumpGroupStat.formatMessage(Type.STUFF, this.statMap.get(Type.STUFF));
        assertMessage(message, MonitoringMessage.OK);

        message = DumpGroupStat.formatMessage(Type.FAST, this.statMap.get(Type.FAST));
        assertMessage(message, MonitoringStatus.WARN, "Failed FAST: session-9 - OK_BUT_SOME_FAILED +1");
    }

    @Test
    public void testCheckPartialExport() {
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF").setStatus(Status.FAILED));
        dao.addData(new DumpGroupData()
            .setType("extractFastExecutor").setSessionName("TEST").setStatus(Status.OK));

        assertMessage(
            stat.checkFailedExport(),
            MonitoringStatus.WARN,
            "Failed STUFF: TEST_STUFF - FAILED"
        );
        assertMessage(
            stat.checkFailedExport(Type.FAST, Type.STUFF),
            MonitoringStatus.WARN,
            "Failed STUFF: TEST_STUFF - FAILED"
        );
        assertMessage(
            stat.checkFailedExport(Type.STUFF),
            MonitoringStatus.WARN,
            "Failed STUFF: TEST_STUFF - FAILED"
        );
        assertMessage(
            stat.checkFailedExport(Type.FAST),
            MonitoringMessage.OK
        );
    }

    @Test
    public void testCheckTime() {
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF").setStatus(Status.FAILED));
        dao.addData(new DumpGroupData()
            .setType("extractFastExecutor").setSessionName("TEST").setStatus(Status.OK));

        // Dummy test that it just works and doesn't fail
        assertMessage(stat.checkTime(Type.STUFF), MonitoringMessage.OK);
    }

    @Test
    public void testCheckFrozenSession() {
        Mockito.when(dumpStorageInfoService.getFrozenSession(Mockito.any())).thenReturn("");

        assertMessage(
            stat.checkFrozenSession(Type.CLUSTERS, Type.FAST, Type.STUFF),
            MonitoringMessage.OK
        );

        Mockito.when(dumpStorageInfoService.getFrozenSession("stuff")).thenReturn("2019-06-06");
        assertMessage(
            stat.checkFrozenSession(Type.CLUSTERS, Type.FAST, Type.STUFF),
            MonitoringStatus.WARN,
            "stuff:2019-06-06"
        );
    }

    @Test
    public void testCheckLastSuccessTimeAllNoGood() {
        assertMessage(
            stat.checkLastSuccessTime(NOW),
            MonitoringStatus.CRITICAL, getLastSuccessTimeError(Type.STUFF),
            getLastSuccessTimeError(Type.CLUSTERS), getLastSuccessTimeError(Type.FAST)
        );
    }

    @Test
    public void testCheckLastSuccessTimeAllGoodExceptCMS() {
        // we don't track CMS
        dao.addAllData(goodStuffData(), goodClustersData(), goodFastData());
        dao.addData(new DumpGroupData()
            .setType("extractCmsExecutor").setSessionName("TEST_CMS").setStatus(Status.FAILED));
        assertMessage(
            stat.checkLastSuccessTime(NOW),
            MonitoringMessage.OK
        );
    }

    @Test
    public void testCheckLastSuccessTimeAllGoodExceptStuffWithoutFinishTime() {
        // should not occur in real world, but check anyway
        dao.addAllData(goodClustersData(), goodFastData());
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF").setStatus(Status.FAILED));
        assertMessage(
            stat.checkLastSuccessTime(NOW),
            MonitoringStatus.CRITICAL,
            getLastSuccessTimeError(Type.STUFF)
        );
    }

    @Test
    public void testCheckLastSuccessTimeRecentOkAfterThreshold() {
        // All good, recent OK stuff threshold - 1 hours ago - no error
        dao.addAllData(goodClustersData(), goodFastData());
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF")
            .setFinishTime(getFinishTime(Type.STUFF, -1))
            .setStatus(Status.OK));
        assertMessage(
            stat.checkLastSuccessTime(NOW),
            MonitoringMessage.OK
        );
    }

    @Test
    public void testCheckLastSuccessTimeRecentOkBeforeThreshold() {
        // All good, recent OK stuff threshold + 1 hours ago - error
        dao.addAllData(goodClustersData(), goodFastData());
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF")
            .setFinishTime(getFinishTime(Type.STUFF, 1))
            .setStatus(Status.OK));
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF")
            .setFinishTime(getFinishTime(Type.STUFF, -1))
            .setStatus(Status.FAILED));
        assertMessage(
            stat.checkLastSuccessTime(NOW),
            MonitoringStatus.CRITICAL,
            getLastSuccessTimeError(Type.STUFF)
        );

        // All good, recent OK clusters threshold + 1 hours ago - error
        dao.clear();
        dao.addAllData(goodStuffData(), goodFastData());
        dao.addData(new DumpGroupData()
            .setType("extractClustersExecutor").setSessionName("TEST_CLUSTERS")
            .setFinishTime(getFinishTime(Type.CLUSTERS, 1))
            .setStatus(Status.OK));
        assertMessage(
            stat.checkLastSuccessTime(NOW),
            MonitoringStatus.CRITICAL,
            getLastSuccessTimeError(Type.CLUSTERS)
        );

        // All good, recent OK fast threshold + 1 hours ago - error
        dao.clear();
        dao.addAllData(goodStuffData(), goodClustersData());
        dao.addData(new DumpGroupData()
            .setType("extractFastExecutor").setSessionName("TEST_FAST")
            .setFinishTime(getFinishTime(Type.FAST, 1))
            .setStatus(Status.OK));
        dao.addData(new DumpGroupData()
            .setType("extractFastExecutor").setSessionName("TEST_FAST")
            .setFinishTime(getFinishTime(Type.FAST, -1))
            .setStatus(Status.FAILED));
        assertMessage(
            stat.checkLastSuccessTime(NOW),
            MonitoringStatus.CRITICAL,
            getLastSuccessTimeError(Type.FAST)
        );
    }

    @Test
    public void testCheckFailedExportAllOk() {
        dao.addAllData(goodStuffData(), goodClustersData(), goodFastData());
        assertMessage(
            stat.checkFailedExport(ALL_TYPES),
            MonitoringMessage.OK
        );
    }

    @Test
    public void testCheckFailedExportSingleFailed() {
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF")
            .setFinishTime(getFinishTime(Type.STUFF))
            .setStatus(Status.FAILED));
        assertMessage(
            stat.checkFailedExport(Type.STUFF),
            MonitoringStatus.WARN
        );
    }

    @Test
    public void testCheckFailedExportMultipleConsecutiveFailed() {
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF1")
            .setFinishTime(getFinishTime(Type.STUFF))
            .setStatus(Status.FAILED));
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF2")
            .setFinishTime(getFinishTime(Type.STUFF))
            .setStatus(Status.FAILED));
        assertMessage(
            stat.checkFailedExport(Type.STUFF),
            MonitoringStatus.CRITICAL
        );
    }

    @Test
    public void testCheckFailedExportLessThan3ConsecutiveOkButSomeFailed() {
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF1")
            .setFinishTime(getFinishTime(Type.STUFF))
            .setStatus(Status.OK_BUT_SOME_FAILED));
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF2")
            .setFinishTime(getFinishTime(Type.STUFF))
            .setStatus(Status.OK_BUT_SOME_FAILED));
        assertMessage(
            stat.checkFailedExport(Type.STUFF),
            MonitoringStatus.WARN
        );
    }

    @Test
    public void testCheckFailedExportMoreThan3ConsecutiveOkButSomeFailed() {
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF1")
            .setFinishTime(getFinishTime(Type.STUFF))
            .setStatus(Status.OK_BUT_SOME_FAILED));
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF2")
            .setFinishTime(getFinishTime(Type.STUFF))
            .setStatus(Status.OK_BUT_SOME_FAILED));
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF3")
            .setFinishTime(getFinishTime(Type.STUFF))
            .setStatus(Status.OK_BUT_SOME_FAILED));
        assertMessage(
            stat.checkFailedExport(Type.STUFF),
            MonitoringStatus.CRITICAL
        );
    }

    @Test
    public void testCheckFailedExportFailedAndOkButSomeFailed() {
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF1")
            .setFinishTime(getFinishTime(Type.STUFF))
            .setStatus(Status.FAILED));
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF2")
            .setFinishTime(getFinishTime(Type.STUFF))
            .setStatus(Status.OK_BUT_SOME_FAILED));
        assertMessage(
            stat.checkFailedExport(Type.STUFF),
            MonitoringStatus.CRITICAL
        );
    }

    @Test
    public void testCheckFailedExportSingleFailedOfDifferentTypes() {
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF")
            .setFinishTime(getFinishTime(Type.STUFF))
            .setStatus(Status.FAILED));
        dao.addData(new DumpGroupData()
            .setType("extractFastExecutor").setSessionName("TEST_FAST")
            .setFinishTime(getFinishTime(Type.FAST))
            .setStatus(Status.FAILED));
        assertMessage(
            stat.checkFailedExport(Type.STUFF, Type.FAST),
            MonitoringStatus.WARN
        );
    }

    @Test
    public void testCheckFailedExportMultipleFailedOfOneTypeAndSingleFailedOfAnotherType() {
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF1")
            .setFinishTime(getFinishTime(Type.STUFF))
            .setStatus(Status.FAILED));
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF2")
            .setFinishTime(getFinishTime(Type.STUFF))
            .setStatus(Status.FAILED));
        dao.addData(new DumpGroupData()
            .setType("extractFastExecutor").setSessionName("TEST_FAST")
            .setFinishTime(getFinishTime(Type.FAST))
            .setStatus(Status.FAILED));
        assertMessage(
            stat.checkFailedExport(Type.STUFF, Type.FAST),
            MonitoringStatus.CRITICAL
        );
    }

    @Test
    public void testCheckFailedExportFailedAndRestarted() {
        dao.clear();
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF1")
            .setFinishTime(getFinishTime(Type.STUFF))
            .setStatus(Status.FAILED));
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF2")
            .setFinishTime(getFinishTime(Type.STUFF))
            .setRestarted(true)
            .setStatus(Status.FAILED));
        assertMessage(
            stat.checkFailedExport(Type.STUFF),
            MonitoringStatus.WARN
        );
    }

    @Test
    public void testCheckFailedExportFailedOkFailedAgain() {
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF1")
            .setFinishTime(getFinishTime(Type.STUFF))
            .setStatus(Status.FAILED));
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF2")
            .setFinishTime(getFinishTime(Type.STUFF))
            .setRestarted(true)
            .setStatus(Status.OK));
        dao.addData(new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF3")
            .setFinishTime(getFinishTime(Type.STUFF))
            .setRestarted(true)
            .setStatus(Status.FAILED));
        assertMessage(
            stat.checkFailedExport(Type.STUFF),
            MonitoringStatus.WARN
        );
    }

    private static DumpGroupData goodStuffData() {
        return new DumpGroupData()
            .setType("extractStuffExecutor").setSessionName("TEST_STUFF").setStatus(Status.OK)
            .setFinishTime(Date.from(NOW.minus(2, ChronoUnit.HOURS)));
    }

    private static DumpGroupData goodFastData() {
        return new DumpGroupData()
            .setType("extractFastExecutor").setSessionName("TEST_FAST").setStatus(Status.OK)
            .setFinishTime(Date.from(NOW.minus(2, ChronoUnit.HOURS)));
    }

    private static DumpGroupData goodClustersData() {
        return new DumpGroupData()
            .setType("extractClustersExecutor").setSessionName("TEST_CLUSTERS").setStatus(Status.OK)
            .setFinishTime(Date.from(NOW.minus(2, ChronoUnit.HOURS)));
    }

    private DumpGroupData dump(int n, Type type, Status status, int duration, long timeOffset) {
        return build(new DumpGroupData(), d -> {
            d.setType(type.getTmsExecutorName());
            d.setStatus(status);
            d.setDuration(duration);
            d.setSessionName("session-" + n);

            long finished = System.currentTimeMillis() + timeOffset;
            d.setCreatedTime(new Date(finished - duration));
            d.setFinishTime(new Date(finished));
        });
    }

    private <T> T build(T item, Consumer<T> builder) {
        builder.accept(item);
        return item;
    }

    private void assertMessage(MonitoringMessage monitoringMessage, MonitoringMessage expectedMessage) {
        assertMessage(monitoringMessage, expectedMessage.getStatus(), expectedMessage.getMessage());
    }

    private void assertMessage(MonitoringMessage monitoringMessage,
                               MonitoringStatus expectedStatus,
                               String... expectedMessages) {
        Assertions.assertThat(monitoringMessage).isNotNull();
        Assertions.assertThat(monitoringMessage.getStatus()).isEqualTo(expectedStatus);
        Assertions.assertThat(monitoringMessage.getMessage()).isNotNull();
        if (expectedMessages.length > 0) {
            Assertions.assertThat(monitoringMessage.getMessage().split(MonitoringUtils.MESSAGE_DELIMITER))
                .containsExactlyInAnyOrder(expectedMessages);
        }
    }

    private Date getFinishTime(Type type, int delta) {
        return Date.from(NOW.minus(getLastSuccessTimeThreshold(type) + delta, ChronoUnit.HOURS));
    }

    private Date getFinishTime(Type type) {
        return getFinishTime(type, 0);
    }
}
