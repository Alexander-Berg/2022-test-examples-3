package ru.yandex.market.mbo.export.dumpstorage;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.common.ZooKeeper.ZooKeeperService;
import ru.yandex.market.mbo.gwt.models.MboDumpSessionStatus;
import ru.yandex.market.mbo.utils.ZooKeeperServiceMock;

import java.util.Arrays;
import java.util.List;

public class ZkDumpStorageInfoServiceTest {

    private static final String DUMP_NAME = "test-dump";

    private ZkDumpStorageInfoService dumpStorageInfoService;
    private ZooKeeperService zooKeeperService = new ZooKeeperServiceMock();

    @Before
    public void setUp() {

        // build ZkDumpStorageInfoService
        dumpStorageInfoService = new ZkDumpStorageInfoService();
        dumpStorageInfoService.setZooKeeperService(zooKeeperService);
    }

    @Test
    public void testRefreshRecentSessionIdOnAllOk() throws Exception {
        List<SessionInfo> sessionInfos = Arrays.asList(
            new SessionInfo("20170331_1907", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1908", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1909", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1910", MboDumpSessionStatus.OK)
        );

        setSessionInfos(sessionInfos);

        dumpStorageInfoService.refreshRecentSessionId(DUMP_NAME);

        String expected = "20170331_1910";

        Assert.assertEquals(expected, dumpStorageInfoService.getRecentSessionId(DUMP_NAME));
    }

    @Test
    public void testRefreshRecentSessionIdOnLastFailed() throws Exception {
        List<SessionInfo> sessionInfos = Arrays.asList(
            new SessionInfo("20170331_1907", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1908", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1909", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1910", MboDumpSessionStatus.FAILED)
        );

        setSessionInfos(sessionInfos);

        dumpStorageInfoService.refreshRecentSessionId(DUMP_NAME);

        String expected = "20170331_1909";

        Assert.assertEquals(expected, dumpStorageInfoService.getRecentSessionId(DUMP_NAME));
    }

    @Test
    public void testRefreshRecentSessionIdManyLastFailed() throws Exception {
        List<SessionInfo> sessionInfos = Arrays.asList(
            new SessionInfo("20170331_1907", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1908", MboDumpSessionStatus.FAILED),
            new SessionInfo("20170331_1909", MboDumpSessionStatus.FAILED),
            new SessionInfo("20170331_1910", MboDumpSessionStatus.FAILED)
        );

        setSessionInfos(sessionInfos);

        dumpStorageInfoService.refreshRecentSessionId(DUMP_NAME);

        String expected = "20170331_1907";

        Assert.assertEquals(expected, dumpStorageInfoService.getRecentSessionId(DUMP_NAME));
    }

    @Test
    public void testRefreshRecentSessionIdOnOneLocked() throws Exception {
        List<SessionInfo> sessionInfos = Arrays.asList(
            new SessionInfo("20170331_1907", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1908", MboDumpSessionStatus.OK, false, true),
            new SessionInfo("20170331_1909", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1910", MboDumpSessionStatus.OK)
        );

        setSessionInfos(sessionInfos);

        dumpStorageInfoService.refreshRecentSessionId(DUMP_NAME);

        String expected = "20170331_1908";

        Assert.assertEquals(expected, dumpStorageInfoService.getRecentSessionId(DUMP_NAME));
    }

    @Test
    public void testRefreshRecentSessionIdOnAllFailed() throws Exception {
        List<SessionInfo> sessionInfos = Arrays.asList(
            new SessionInfo("20170331_1907", MboDumpSessionStatus.FAILED),
            new SessionInfo("20170331_1908", MboDumpSessionStatus.FAILED),
            new SessionInfo("20170331_1909", MboDumpSessionStatus.FAILED),
            new SessionInfo("20170331_1910", MboDumpSessionStatus.FAILED)
        );

        setSessionInfos(sessionInfos);

        dumpStorageInfoService.refreshRecentSessionId(DUMP_NAME);

        String expected = "20170331_1910";

        Assert.assertEquals(expected, dumpStorageInfoService.getRecentSessionId(DUMP_NAME));
    }

    @Test(expected = RuntimeException.class)
    public void lockDumpSessionOnLockFailed() throws Exception {
        List<SessionInfo> sessionInfos = Arrays.asList(
            new SessionInfo("20170331_1907", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1908", MboDumpSessionStatus.FAILED, false, true),
            new SessionInfo("20170331_1909", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1910", MboDumpSessionStatus.OK)
        );
        setSessionInfos(sessionInfos);
    }

    @Test
    public void testUnlockDump() throws Exception {
        List<SessionInfo> sessionInfos = Arrays.asList(
            new SessionInfo("20170331_1907", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1908", MboDumpSessionStatus.OK, false, true),
            new SessionInfo("20170331_1909", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1910", MboDumpSessionStatus.FAILED)
        );
        setSessionInfos(sessionInfos);
        Assert.assertEquals("20170331_1908", dumpStorageInfoService.getLockedDumpSessionId(DUMP_NAME));
        dumpStorageInfoService.unlockDump(DUMP_NAME);
        Assert.assertEquals("", dumpStorageInfoService.getLockedDumpSessionId(DUMP_NAME));
    }

    private void setSessionInfos(List<SessionInfo> sessionInfos) {
        for (SessionInfo sessionInfo : sessionInfos) {
            dumpStorageInfoService.updateSessionInfo(DUMP_NAME, sessionInfo.getSessionId(), sessionInfo.getStatus());
            if (sessionInfo.isLocked()) {
                dumpStorageInfoService.lockDumpSession(DUMP_NAME, sessionInfo.getSessionId());
            }
        }
    }
}
