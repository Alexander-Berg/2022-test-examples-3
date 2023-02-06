package ru.yandex.market.mbo.tms.export;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.amazonaws.services.s3.model.ObjectListing;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.mbo.common.ZooKeeper.ZooKeeperService;
import ru.yandex.market.mbo.common.s3.AmazonS3ClientFactory;
import ru.yandex.market.mbo.export.dumpstorage.DumpStorageInfoServiceImpl;
import ru.yandex.market.mbo.export.dumpstorage.SessionInfo;
import ru.yandex.market.mbo.export.dumpstorage.YtSessionService;
import ru.yandex.market.mbo.export.dumpstorage.ZkDumpStorageInfoService;
import ru.yandex.market.mbo.gwt.models.MboDumpSessionStatus;
import ru.yandex.market.mbo.s3.AmazonS3Mock;
import ru.yandex.market.mbo.synchronizer.export.storage.DumpStorageService;
import ru.yandex.market.mbo.synchronizer.export.storage.ReplicationService;
import ru.yandex.market.mbo.synchronizer.export.storage.S3DumpStorageCoreService;
import ru.yandex.market.mbo.synchronizer.export.storage.SessionFolderMaker;
import ru.yandex.market.mbo.utils.DateFormatUtils;
import ru.yandex.market.mbo.utils.ZooKeeperServiceMock;
import ru.yandex.market.mbo.yt.TestYt;

/**
 * @author moskovkin@yandex-team.ru
 * @since 31.03.2017
 */
public class RotateDumpsInStorageExecutorTest {
    private static final Logger log = LoggerFactory.getLogger(RotateDumpsInStorageExecutorTest.class);

    private static final String BUCKET_NAME = "mbo-dump";
    private static final String SESSION_ID_PREFIX = "20190722_130";
    private static final String DUMP_NAME = "stuff";
    private static final String YT_PATH = "//" + DUMP_NAME;
    private static final String SUCCESSFUL_REPLICATED = "successful_replicated";

    private static final int BATCH_SIZE = 5;
    private static final int DAYS_TO_KEEP_FAILED = 1;
    private static final int DAYS_TO_KEEP_DISABLED = 2;
    private static final int OK_SESSIONS_TO_KEEP = 5;
    private static final int OK_SESSIONS_TO_ARCHIVE = 3;
    private static final int TOTAL_NUMBER_OF_SESSIONS = OK_SESSIONS_TO_KEEP + OK_SESSIONS_TO_ARCHIVE;

    private RotateDumpsInStorageExecutor rotateExecutor;
    private DumpStorageInfoServiceImpl dumpStorageInfoService;
    private DumpStorageService dumpStorageService;
    private ZooKeeperService zooKeeperService;
    private ReplicationService replicationService;
    private Yt yt;
    private Yt ytReplicated;

    private AmazonS3Mock amazonS3;
    private SessionFolderMaker sessionCreator;
    private S3DumpStorageCoreService s3StorageCoreService;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        amazonS3 = Mockito.spy(AmazonS3Mock.class);
        ((AmazonS3Mock) amazonS3).setBatchSize(BATCH_SIZE);
        AmazonS3ClientFactory alwaysNewS3ClientFactory = Mockito.mock(AmazonS3ClientFactory.class);
        Mockito.when(alwaysNewS3ClientFactory.getS3Client()).thenAnswer(__ -> amazonS3);

        yt = new TestYt();
        ytReplicated = new TestYt();

        YtSessionService ytSessionService = new YtSessionService();
        ytSessionService.setYtRootPath(YT_PATH);
        ytSessionService.setYt(yt);

        YtSessionService ytReplicatedSessionService = new YtSessionService();
        ytReplicatedSessionService.setYtRootPath(YT_PATH);
        ytReplicatedSessionService.setYt(ytReplicated);

        zooKeeperService = new ZooKeeperServiceMock();

        ZkDumpStorageInfoService zkDumpStorageInfoService = new ZkDumpStorageInfoService();
        zkDumpStorageInfoService.setZooKeeperService(zooKeeperService);

        dumpStorageInfoService = new DumpStorageInfoServiceImpl(
            zkDumpStorageInfoService,
            ytSessionService,
            ytReplicatedSessionService
        );

        s3StorageCoreService = Mockito.spy(S3DumpStorageCoreService.class);
        s3StorageCoreService.setS3ClientFactory(alwaysNewS3ClientFactory);
        s3StorageCoreService.setBucketName(BUCKET_NAME);

        Path tmpFolder = folder.newFolder("dump-tmp-folder").toPath();

        replicationService = Mockito.mock(ReplicationService.class);

        dumpStorageService = new DumpStorageService(s3StorageCoreService, dumpStorageInfoService, ytSessionService,
            replicationService, tmpFolder);

        rotateExecutor = new RotateDumpsInStorageExecutor();
        rotateExecutor.setDumpStorage(dumpStorageService);
        rotateExecutor.setArchiveInS3(true);
        rotateExecutor.setExtendedSave(false);
        rotateExecutor.setCountLastOkSessionsSave(OK_SESSIONS_TO_KEEP);

        sessionCreator = new SessionFolderMaker(folder.newFolder("mbo-test-uploads"));
    }

    @Test
    public void testDeleteSession() throws Exception {

        createSessionsInS3AndYt(DUMP_NAME, TOTAL_NUMBER_OF_SESSIONS);
        Assert.assertTrue(dumpStorageService.isExistSession(DUMP_NAME, SESSION_ID_PREFIX + 1));
        rotateExecutor.doRealJob(null);
        Assert.assertFalse(dumpStorageService.isExistSession(DUMP_NAME, SESSION_ID_PREFIX + 1));
    }

    @Test
    public void testCreateArchiveS3() throws Exception {
        createSessionsInS3AndYt(DUMP_NAME, TOTAL_NUMBER_OF_SESSIONS);

        rotateExecutor.doRealJob(null);

        List<String> archiveInS3 = dumpStorageService.listSessionsArchives(DUMP_NAME);
        Assertions.assertThat(archiveInS3).containsExactlyInAnyOrder(
            SESSION_ID_PREFIX + "1",
            SESSION_ID_PREFIX + "2",
            SESSION_ID_PREFIX + "3"
        );
    }

    @Test
    public void testRemoveOld() throws Exception {
        List<SessionInfo> sessionInfos = Arrays.asList(
            new SessionInfo("20170331_1901", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1902", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1903", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1904", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1905", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1906", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1907", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1908", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1909", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1910", MboDumpSessionStatus.OK)
        );

        setSessionInfos(sessionInfos);

        rotateExecutor.setArchiveInS3(false);
        rotateExecutor.doRealJob(null);
        rotateExecutor.setArchiveInS3(true);

        List<SessionInfo> expected = Arrays.asList(
            new SessionInfo("20170331_1906", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1907", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1908", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1909", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1910", MboDumpSessionStatus.OK)
        );

        Assert.assertEquals(expected, dumpStorageInfoService.getSessionInfos(DUMP_NAME));
    }

    @Test
    public void testRemoveOldWithLocked() throws Exception {
        List<SessionInfo> sessionInfos = Arrays.asList(
            new SessionInfo("20170331_1901", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1902", MboDumpSessionStatus.OK, false, true),
            new SessionInfo("20170331_1903", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1904", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1905", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1906", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1907", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1908", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1909", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1910", MboDumpSessionStatus.OK)
        );

        setSessionInfos(sessionInfos);

        Assert.assertEquals(true, yt.cypress().exists(YPath.simple(YT_PATH).child("20170331_1901")));
        Assert.assertEquals(true, yt.cypress().exists(YPath.simple(YT_PATH).child("20170331_1902")));
        Assert.assertEquals(true, yt.cypress().exists(YPath.simple(YT_PATH).child("20170331_1903")));
        Assert.assertEquals(true, yt.cypress().exists(YPath.simple(YT_PATH).child("20170331_1910")));

        rotateExecutor.setArchiveInS3(false);
        rotateExecutor.doRealJob(null);
        rotateExecutor.setArchiveInS3(true);

        Assert.assertEquals(false, yt.cypress().exists(YPath.simple(YT_PATH).child("20170331_1901")));
        Assert.assertEquals(true, yt.cypress().exists(YPath.simple(YT_PATH).child("20170331_1902")));
        Assert.assertEquals(false, yt.cypress().exists(YPath.simple(YT_PATH).child("20170331_1903")));
        Assert.assertEquals(true, yt.cypress().exists(YPath.simple(YT_PATH).child("20170331_1910")));

        List<SessionInfo> expected = Arrays.asList(
            new SessionInfo("20170331_1902", MboDumpSessionStatus.OK, false, true),
            new SessionInfo("20170331_1906", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1907", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1908", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1909", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1910", MboDumpSessionStatus.OK)
        );

        Assert.assertEquals(expected, dumpStorageInfoService.getSessionInfos(DUMP_NAME));
    }

    @Test
    public void testRemoveOldWithLockedInLast() throws Exception {
        List<SessionInfo> sessionInfos = Arrays.asList(
            new SessionInfo("20170331_1901", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1902", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1903", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1904", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1905", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1906", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1907", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1908", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1909", MboDumpSessionStatus.OK, false, true),
            new SessionInfo("20170331_1910", MboDumpSessionStatus.OK)
        );

        setSessionInfos(sessionInfos);

        rotateExecutor.setArchiveInS3(false);
        rotateExecutor.doRealJob(null);
        rotateExecutor.setArchiveInS3(true);

        List<SessionInfo> expected = Arrays.asList(
            new SessionInfo("20170331_1906", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1907", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1908", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1909", MboDumpSessionStatus.OK, false, true),
            new SessionInfo("20170331_1910", MboDumpSessionStatus.OK)
        );

        Assert.assertEquals(expected, dumpStorageInfoService.getSessionInfos(DUMP_NAME));
    }

    @Test
    public void testLeaveAllAsIs() throws Exception {
        List<SessionInfo> initialStateOfSessions = Arrays.asList(
            new SessionInfo("20170331_1907", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1908", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1909", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1910", MboDumpSessionStatus.OK)
        );
        setSessionInfos(initialStateOfSessions);

        rotateExecutor.setArchiveInS3(false);
        rotateExecutor.doRealJob(null);
        rotateExecutor.setArchiveInS3(true);

        List<SessionInfo> finalStateOfSessions = Arrays.asList(
            new SessionInfo("20170331_1907", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1908", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1909", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1910", MboDumpSessionStatus.OK)
        );
        Assert.assertEquals(finalStateOfSessions, dumpStorageInfoService.getSessionInfos(DUMP_NAME));
    }

    @Test
    public void testLeaveAllAsIsWithLocked() throws Exception {
        List<SessionInfo> initialStateOfSessions = Arrays.asList(
            new SessionInfo("20170331_1907", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1908", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1909", MboDumpSessionStatus.OK, false, true),
            new SessionInfo("20170331_1910", MboDumpSessionStatus.OK)
        );
        setSessionInfos(initialStateOfSessions);

        rotateExecutor.setArchiveInS3(false);
        rotateExecutor.doRealJob(null);
        rotateExecutor.setArchiveInS3(true);

        List<SessionInfo> finalStateOfSessions = Arrays.asList(
            new SessionInfo("20170331_1907", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1908", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1909", MboDumpSessionStatus.OK, false, true),
            new SessionInfo("20170331_1910", MboDumpSessionStatus.OK)
        );
        Assert.assertEquals(finalStateOfSessions, dumpStorageInfoService.getSessionInfos(DUMP_NAME));
    }

    @Test
    public void testDealWithBrokenDumps() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime someDaysAgoFailed = now.minusDays(DAYS_TO_KEEP_FAILED);
        String someDaysAgoFailedSession = DateFormatUtils.DEFAULT_SESSION_FORMAT.format(someDaysAgoFailed);

        LocalDateTime manyDaysAgoFailed = now.minusDays(DAYS_TO_KEEP_FAILED + 2);
        String manyDaysAgoFailedSession = DateFormatUtils.DEFAULT_SESSION_FORMAT.format(manyDaysAgoFailed);

        now = now.plusHours(1);

        LocalDateTime someDaysAgoDisabled = now.minusDays(DAYS_TO_KEEP_DISABLED);
        String someDaysAgoDisabledSession = DateFormatUtils.DEFAULT_SESSION_FORMAT.format(someDaysAgoDisabled);

        LocalDateTime manyDaysAgoDisabled = now.minusDays(DAYS_TO_KEEP_DISABLED + 2);
        String manyDaysAgoDisabledSession = DateFormatUtils.DEFAULT_SESSION_FORMAT.format(manyDaysAgoDisabled);

        List<SessionInfo> initialStateOfSessions = Arrays.asList(
            new SessionInfo(someDaysAgoFailedSession, MboDumpSessionStatus.FAILED),
            new SessionInfo(manyDaysAgoFailedSession, MboDumpSessionStatus.FAILED),
            new SessionInfo(someDaysAgoDisabledSession, MboDumpSessionStatus.DISABLED),
            new SessionInfo(manyDaysAgoDisabledSession, MboDumpSessionStatus.DISABLED),
            new SessionInfo("20170331_1902", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1903", MboDumpSessionStatus.OK, false, true),
            new SessionInfo("20170331_1904", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1905", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1906", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1907", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1908", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1909", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1910", MboDumpSessionStatus.OK)
        );
        setSessionInfos(initialStateOfSessions);

        rotateExecutor.setArchiveInS3(false);
        rotateExecutor.doRealJob(null);
        rotateExecutor.setArchiveInS3(true);

        List<SessionInfo> finalStateOfSessions = new ArrayList<>(Arrays.asList(
            new SessionInfo(someDaysAgoFailedSession, MboDumpSessionStatus.FAILED),
            new SessionInfo(someDaysAgoDisabledSession, MboDumpSessionStatus.DISABLED),
            new SessionInfo("20170331_1903", MboDumpSessionStatus.OK, false, true),
            new SessionInfo("20170331_1906", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1907", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1908", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1909", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1910", MboDumpSessionStatus.OK)
        ));
        finalStateOfSessions.sort(Comparator.naturalOrder());

        Assert.assertEquals(finalStateOfSessions, dumpStorageInfoService.getSessionInfos(DUMP_NAME));
    }

    @Test
    public void testDoNotTouchUnknownStatus() throws Exception {
        zooKeeperService.create(zooKeeperService.pathBy(DUMP_NAME));
        zooKeeperService.write(zooKeeperService.pathBy(DUMP_NAME, "18120320_1910"), "42");

        rotateExecutor.setArchiveInS3(false);
        rotateExecutor.doRealJob(null);
        rotateExecutor.setArchiveInS3(true);

        Assert.assertEquals(
            "42",
            zooKeeperService.read(zooKeeperService.pathBy(DUMP_NAME, "18120320_1910"))
        );
    }

    @Test
    public void testRemoveOldSessionFromYt() throws Exception {
        List<SessionInfo> sessionInfos = Arrays.asList(
            new SessionInfo("20170331_1901", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1902", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1907", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1908", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1909", MboDumpSessionStatus.OK, false, true),
            new SessionInfo("20170331_1910", MboDumpSessionStatus.OK)
        );

        setSessionInfos(sessionInfos);

        addSessionAtYT("20170331_1800");

        Assert.assertEquals(true, yt.cypress().exists(YPath.simple(YT_PATH).child("20170331_1901")));
        Assert.assertEquals(true, yt.cypress().exists(YPath.simple(YT_PATH).child("20170331_1902")));
        Assert.assertEquals(true, yt.cypress().exists(YPath.simple(YT_PATH).child("20170331_1907")));
        Assert.assertEquals(true, yt.cypress().exists(YPath.simple(YT_PATH).child("20170331_1908")));
        Assert.assertEquals(true, yt.cypress().exists(YPath.simple(YT_PATH).child("20170331_1909")));
        Assert.assertEquals(true, yt.cypress().exists(YPath.simple(YT_PATH).child("20170331_1910")));
        Assert.assertEquals(true, yt.cypress().exists(YPath.simple(YT_PATH).child("20170331_1800")));

        rotateExecutor.setArchiveInS3(false);
        rotateExecutor.doRealJob(null);
        rotateExecutor.setArchiveInS3(true);

        Assert.assertEquals(false, yt.cypress().exists(YPath.simple(YT_PATH).child("20170331_1901")));
        Assert.assertEquals(true, yt.cypress().exists(YPath.simple(YT_PATH).child("20170331_1902")));
        Assert.assertEquals(true, yt.cypress().exists(YPath.simple(YT_PATH).child("20170331_1907")));
        Assert.assertEquals(true, yt.cypress().exists(YPath.simple(YT_PATH).child("20170331_1908")));
        Assert.assertEquals(true, yt.cypress().exists(YPath.simple(YT_PATH).child("20170331_1909")));
        Assert.assertEquals(true, yt.cypress().exists(YPath.simple(YT_PATH).child("20170331_1910")));
        Assert.assertEquals(false, yt.cypress().exists(YPath.simple(YT_PATH).child("20170331_1800")));
    }

    @Test
    public void recentNotRemoved() throws Exception {
        List<SessionInfo> sessionInfos = Collections.singletonList(
            new SessionInfo("20170331_1901", MboDumpSessionStatus.OK)
        );

        setSessionInfos(sessionInfos);

        addSessionAtYT("20170331_1800");

        Assert.assertTrue(yt.cypress().exists(YPath.simple(YT_PATH).child("recent")));

        rotateExecutor.setArchiveInS3(false);
        rotateExecutor.doRealJob(null);
        rotateExecutor.setArchiveInS3(true);


        Assert.assertTrue(yt.cypress().exists(YPath.simple(YT_PATH).child("recent")));
    }

    @Test
    public void checkIfCopyToArchivesInBatches() throws Exception {
        int numberOfObjects = BATCH_SIZE * 2 + 1;
        int numbOfBatches = (int) Math.ceil((double) numberOfObjects / BATCH_SIZE);
        String sessionId = "1000";
        for (int i = 0; i < numberOfObjects; i++) {
            String name = String.valueOf(i);
            putDumpArchiveS3(DUMP_NAME, sessionId, name);
        }
        String sessionPrefix = DUMP_NAME + "/" + sessionId + "/";
        // If we call doRealJob it is hard to count numbers of calls
        s3StorageCoreService.copyToArchives(sessionPrefix);
        // Checks if listObjects was called 1 time
        Mockito.verify(amazonS3, Mockito.times(1))
            .listObjects(Mockito.anyString(), Mockito.anyString());
        // Checks if listNextBatchOfObjects was called [numbOfBatches - 1] times
        Mockito.verify(amazonS3, Mockito.times(numbOfBatches - 1))
            .listNextBatchOfObjects(Mockito.any(ObjectListing.class));
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void shoulCopyOkSessionToArchivesWithCorrectPath() throws Exception {
        List<SessionInfo> sessionInfos = Arrays.asList(
            new SessionInfo("20170331_1901", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1902", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1907", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1908", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1909", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1910", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1915", MboDumpSessionStatus.OK)
        );

        setSessionInfos(sessionInfos);

        List<String> names = IntStream.range(0, 20).mapToObj(String::valueOf).collect(Collectors.toList());
        for (SessionInfo sessionInfo : sessionInfos) {
            for (String name : names) {
                putDumpArchiveS3(DUMP_NAME, sessionInfo.getSessionId(), name);
            }
        }
        // contains all sessions with all files
        sessionInfos.forEach(sessionInfo ->
            Assertions.assertThat(amazonS3.getAllKeys())
                .containsAll(names.stream()
                    .map(s -> DUMP_NAME + "/" + sessionInfo.getSessionId() + "/" + s + ".gz")
                    .collect(Collectors.toList())));


        // If we call doRealJob it is hard to count numbers of calls
        rotateExecutor.setArchiveInS3(true);
        rotateExecutor.doRealJob(null);

        // still contains files for latest sessions
        sessionInfos.stream().filter(sessionInfo ->
            !sessionInfo.getSessionId().equals("20170331_1901") &&
                !sessionInfo.getSessionId().equals("20170331_1902"))
            .forEach(sessionInfo ->
                Assertions.assertThat(amazonS3.getAllKeys())
                    .containsAll(names.stream()
                        .map(s -> DUMP_NAME + "/" + sessionInfo.getSessionId() + "/" + s + ".gz")
                        .collect(Collectors.toList())));

        // old sessions moved to archives
        Stream.of("20170331_1901", "20170331_1902")
            .forEach(sessionInfo -> {
                Assertions.assertThat(amazonS3.getAllKeys())
                    .doesNotContainAnyElementsOf(names.stream()
                        .map(s -> DUMP_NAME + "/" + sessionInfo + "/" + s + ".gz")
                        .collect(Collectors.toList()));
                Assertions.assertThat(amazonS3.getAllKeys())
                    .containsAll(names.stream()
                        .map(s -> "archive/" + DUMP_NAME + "/" + sessionInfo + "/" + s + ".gz")
                        .collect(Collectors.toList()));
            });

    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testOkSessionSaveStrategy() throws Exception {
        List<SessionInfo> sessionInfos = Arrays.asList(
            new SessionInfo("20170331_1907", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1908", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1909", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1910", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1915", MboDumpSessionStatus.OK),
            new SessionInfo("20170331_1920", MboDumpSessionStatus.OK),

            new SessionInfo("20170330_1810", MboDumpSessionStatus.OK),
            new SessionInfo("20170330_1815", MboDumpSessionStatus.OK),

            new SessionInfo("20170329_1810", MboDumpSessionStatus.OK),
            new SessionInfo("20170329_1815", MboDumpSessionStatus.OK),

            new SessionInfo("20170328_1810", MboDumpSessionStatus.OK),
            new SessionInfo("20170328_1815", MboDumpSessionStatus.OK),

            new SessionInfo("20170327_1810", MboDumpSessionStatus.OK),
            new SessionInfo("20170327_1815", MboDumpSessionStatus.OK),

            new SessionInfo("20170305_1815", MboDumpSessionStatus.OK)
        );

        setSessionInfos(sessionInfos);

        List<String> names = IntStream.range(0, 20).mapToObj(String::valueOf).collect(Collectors.toList());
        for (SessionInfo sessionInfo : sessionInfos) {
            for (String name : names) {
                putDumpArchiveS3(DUMP_NAME, sessionInfo.getSessionId(), name);
            }
        }
        // contains all sessions with all files
        sessionInfos.forEach(sessionInfo ->
            Assertions.assertThat(amazonS3.getAllKeys())
                .containsAll(names.stream()
                    .map(s -> DUMP_NAME + "/" + sessionInfo.getSessionId() + "/" + s + ".gz")
                    .collect(Collectors.toList())));


        // If we call doRealJob it is hard to count numbers of calls
        rotateExecutor.setArchiveInS3(true);
        rotateExecutor.setExtendedSave(true);
        rotateExecutor.setOkSessionsDaysToKeep(5);
        rotateExecutor.doRealJob(null);
        rotateExecutor.setExtendedSave(false);

        List<String> removedSessions = Arrays.asList("20170331_1907", "20170330_1810", "20170329_1810",
            "20170328_1810", "20170327_1810", "20170305_1815");

        // still contains files for latest sessions
        sessionInfos.stream().filter(sessionInfo ->
            !removedSessions.contains(sessionInfo.getSessionId()))
            .forEach(sessionInfo ->
                Assertions.assertThat(amazonS3.getAllKeys())
                    .containsAll(names.stream()
                        .map(s -> DUMP_NAME + "/" + sessionInfo.getSessionId() + "/" + s + ".gz")
                        .collect(Collectors.toList())));

        // old sessions moved to archives
        removedSessions
            .forEach(sessionInfo -> {
                Assertions.assertThat(amazonS3.getAllKeys())
                    .doesNotContainAnyElementsOf(names.stream()
                        .map(s -> DUMP_NAME + "/" + sessionInfo + "/" + s + ".gz")
                        .collect(Collectors.toList()));
                Assertions.assertThat(amazonS3.getAllKeys())
                    .containsAll(names.stream()
                        .map(s -> "archive/" + DUMP_NAME + "/" + sessionInfo + "/" + s + ".gz")
                        .collect(Collectors.toList()));
            });

    }

    private void putDumpArchiveS3(String dumpName, String sessionId, String fileName) throws IOException {
        String key = dumpName + "/" + sessionId + "/" + fileName + ".gz";
        File sessionFolder = sessionCreator.createSessionFolder(sessionId).toFile();
        File archive = File.createTempFile("test-", sessionId, sessionFolder);
        amazonS3.putObject(BUCKET_NAME, key, archive);
    }

    private void setSessionInfos(List<SessionInfo> sessionInfos) {
        for (SessionInfo sessionInfo : sessionInfos) {
            addSessionAtYT(sessionInfo.getSessionId());
            dumpStorageInfoService.updateSessionInfo(DUMP_NAME, sessionInfo.getSessionId(), sessionInfo.getStatus());
            if (sessionInfo.isLocked()) {
                dumpStorageInfoService.lockDumpSession(DUMP_NAME, sessionInfo.getSessionId());
            }
        }
    }

    private void addSessionAtYT(String sessionId) {
        yt.cypress().create(YPath.simple(YT_PATH).child(sessionId), CypressNodeType.MAP);
        ytReplicated.cypress().create(YPath.simple(YT_PATH).child(sessionId), CypressNodeType.MAP);
        ytReplicated.cypress().set(YPath.simple(YT_PATH).child(sessionId).attribute(SUCCESSFUL_REPLICATED), true);
    }

    private void createSessionsInS3AndYt(String dumpName, int count) throws IOException {
        for (int i = 0; i < count; i++) {
            yt.cypress().create(YPath.simple(YT_PATH + "/" + SESSION_ID_PREFIX + (i + 1)), CypressNodeType.MAP);
            Path sessionFolder = createSessionInS3(dumpName, SESSION_ID_PREFIX + (i + 1));
            log.info("created session: " + sessionFolder);
        }
    }

    private Path createSessionInS3(String dumpName, String sessionId) throws IOException {
        Path sessionFolder = sessionCreator.createSessionFolder(sessionId);
        dumpStorageService.saveSession(dumpName, sessionId, sessionFolder, false, Collections.emptySet());
        return sessionFolder;
    }
}
