package ru.yandex.market.mbo.tms.export;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.springframework.util.StreamUtils;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.mbo.common.ZooKeeper.ZooKeeperService;
import ru.yandex.market.mbo.common.s3.AmazonS3ClientFactory;
import ru.yandex.market.mbo.core.export.yt.lock.YtLockService;
import ru.yandex.market.mbo.core.export.yt.lock.YtLockServiceMock;
import ru.yandex.market.mbo.export.dumpstorage.SessionInfo;
import ru.yandex.market.mbo.export.dumpstorage.YtSessionService;
import ru.yandex.market.mbo.export.dumpstorage.ZkDumpStorageInfoService;
import ru.yandex.market.mbo.gwt.models.MboDumpSessionStatus;
import ru.yandex.market.mbo.s3.AmazonS3Mock;
import ru.yandex.market.mbo.synchronizer.export.AbstractExtractor;
import ru.yandex.market.mbo.synchronizer.export.BaseExtractor;
import ru.yandex.market.mbo.synchronizer.export.ExportFileValidationException;
import ru.yandex.market.mbo.synchronizer.export.ExportFileValidator;
import ru.yandex.market.mbo.synchronizer.export.ExportRegistry;
import ru.yandex.market.mbo.synchronizer.export.ExporterUtils;
import ru.yandex.market.mbo.synchronizer.export.ExtractorWriterService;
import ru.yandex.market.mbo.synchronizer.export.MD5SUMS;
import ru.yandex.market.mbo.synchronizer.export.Md5Writer;
import ru.yandex.market.mbo.synchronizer.export.OnFailureReaction;
import ru.yandex.market.mbo.synchronizer.export.ParallelExtractor;
import ru.yandex.market.mbo.synchronizer.export.RegistryStatus;
import ru.yandex.market.mbo.synchronizer.export.YtBaseSwitcher;
import ru.yandex.market.mbo.synchronizer.export.YtSwitcher;
import ru.yandex.market.mbo.synchronizer.export.event.support.RegistryEventsListener;
import ru.yandex.market.mbo.synchronizer.export.storage.DumpStorageService;
import ru.yandex.market.mbo.synchronizer.export.storage.ReplicationService;
import ru.yandex.market.mbo.synchronizer.export.storage.S3DumpStorageCoreService;
import ru.yandex.market.mbo.utils.OsUtils;
import ru.yandex.market.mbo.utils.ZooKeeperServiceMock;
import ru.yandex.market.mbo.yt.TestYt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static ru.yandex.market.mbo.synchronizer.export.OnFailureReaction.REGISTER_FAILURE;
import static ru.yandex.market.mbo.synchronizer.export.OnFailureReaction.SWITCH_TO_PREV;
import static ru.yandex.market.mbo.synchronizer.export.OnFailureReaction.SWITCH_TO_PREV_FILES;

// @formatter:off
/**
 * Тест ExportJobExecutorTest.
 *
 * Структура тестовой выгрузки:
 * test-dump
 *   yyyyMMdd_HHmmss_SSS
 *     MD5SUMS.gz
 *     file1.xml.gz
 *     dirA
 *       file2.xml.gz
 *       dirB
 *         file3.xml.gz
 *         file4.xml.gz
 *
 *
 * @author ayratgdl
 * @date 09.04.18
 */
// @formatter:on
public class ExportJobExecutorTest {
    private static final long MILLISECONDS = 10L;
    private static final String DUMP_NAME = "test-dump";
    private static final int COMPRESSION_LEVEL = 5;
    private Path tmpDir;
    private AmazonS3 amazonS3;
    private ReplicationService replicationService;
    private ExportRegistry exportRegistry;
    private ExportJobExecutor exportExecutor;
    private ExtractorsGroup rootGroup;
    private ExtractorsGroup dirAGroup;
    private ExtractorsGroup dirBGroup;
    private TestExtractor extractor1;
    private TestExtractor extractor2;
    private TestExtractor extractor3;
    private TestExtractor extractor4;
    private FailedListener failedListener;

    private YtSessionService ytSessionService;
    private YtSwitchService ytSwitcher;
    private Yt yt;
    private ZkDumpStorageInfoService dumpStorageInfoService;
    private int extractorsNumber = 5;

    private Consumer<CommonExtractorForInterruptionTesting> interruptingConsumer = (extractor) -> {
        try {
            extractor.allExtractorsAlreadyStartedGate.countDown();
            //wait while all extractors would be started
            extractor.allExtractorsAlreadyStartedGate.await();
        } catch (InterruptedException e) {
            // do nothing it's fine
        }
        extractor.isCompletedExceptionally = true;
        throw new RuntimeException();
    };


    private Consumer<CommonExtractorForInterruptionTesting> waitingConsumer = (extractor) -> {
        try {
            extractor.allExtractorsAlreadyStartedGate.countDown();
            //wait while all extractors would be started
            extractor.allExtractorsAlreadyStartedGate.await();

            //wait while some extractor completed exceptionally
            extractor.interruptionGate.await(MILLISECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            extractor.isCompletedExceptionally = true;
            throw new RuntimeException();
        }
        extractor.isCompletedExceptionally = false;
    };
    private JobExecutionContext mockedContext;

    @Before
    public void setUp() throws Exception {
        tmpDir = Files.createTempDirectory("mbo-test-");

        YtLockService ytLockService = new YtLockServiceMock();

        ZooKeeperService zooKeeperService = new ZooKeeperServiceMock();
        dumpStorageInfoService = new ZkDumpStorageInfoService();
        dumpStorageInfoService.setZooKeeperService(zooKeeperService);

        amazonS3 = new AmazonS3Mock();
        replicationService = Mockito.mock(ReplicationService.class);
        AmazonS3ClientFactory alwaysNewS3ClientFactory = Mockito.mock(AmazonS3ClientFactory.class);
        Mockito.when(alwaysNewS3ClientFactory.getS3Client()).thenAnswer(invocation -> amazonS3);
        S3DumpStorageCoreService coreStorageService = new S3DumpStorageCoreService();
        coreStorageService.setS3ClientFactory(alwaysNewS3ClientFactory);
        coreStorageService.setBucketName("dump_bucket");

        yt = new TestYt();

        ytSessionService = new YtSessionService();
        ytSessionService.setYtRootPath("//test");
        ytSessionService.setYt(yt);

        DumpStorageService storageService = new DumpStorageService(coreStorageService, dumpStorageInfoService,
            ytSessionService, replicationService, null);

        extractor1 = new TestExtractor("file1.xml", coreStorageService);
        extractor2 = new TestExtractor("file2.xml", coreStorageService);
        extractor3 = new TestExtractor("file3.xml", coreStorageService);
        extractor4 = new TestExtractor("file4.xml", coreStorageService);

        Field f = TestExtractor.class.getSuperclass().getSuperclass().getDeclaredField("s3DumpStorageCoreService");
        f.setAccessible(true);
        f.set(extractor1, coreStorageService);
        f.set(extractor2, coreStorageService);
        f.set(extractor3, coreStorageService);
        f.set(extractor4, coreStorageService);

        rootGroup = new ExtractorsGroup();
        rootGroup.setExtractors(Collections.singletonList(extractor1));
        dirAGroup = new ExtractorsGroup();
        dirAGroup.setDir("dirA");
        dirAGroup.setExtractors(Collections.singletonList(extractor2));
        rootGroup.setSubgroups(Collections.singletonList(dirAGroup));
        dirBGroup = new ExtractorsGroup();
        dirBGroup.setDir("dirB");
        dirBGroup.setExtractors(Arrays.asList(extractor3, extractor4));
        dirAGroup.setSubgroups(Collections.singletonList(dirBGroup));

        failedListener = new FailedListener();
        exportRegistry = new ExportRegistry();
        exportRegistry.setDumpName(DUMP_NAME);
        exportRegistry.setRootPath(tmpDir.toAbsolutePath().toString());
        exportRegistry.setListeners(Collections.singletonList(failedListener));
        exportRegistry.setFolderNameFormat("yyyyMMdd_HHmmss_SSS");
        exportRegistry.setYtExportPath("//test");
        exportRegistry.afterPropertiesSet();

        ytSwitcher = new YtSwitchService("//test");

        exportExecutor = new ExportJobExecutor();
        exportExecutor.setZooKeeperService(zooKeeperService);
        exportExecutor.setDumpStorageService(storageService);
        exportExecutor.setRegistry(exportRegistry);
        exportExecutor.setName("exportExecutor");
        exportExecutor.setJobName("exportExecutorJob");
        exportExecutor.setRootExtractorsGroup(rootGroup);
        exportExecutor.setYtSwitchService(ytSwitcher);
        exportExecutor.setYtLockService(ytLockService);

        mockedContext = Mockito.mock(JobExecutionContext.class);
        Scheduler mockedScheduler = Mockito.mock(Scheduler.class);
        Mockito.when(mockedContext.getScheduler()).thenReturn(mockedScheduler);
        Mockito.when(mockedScheduler.getContext()).thenReturn(new SchedulerContext());
    }

    @After
    public void tearDown() throws Exception {
        deleteDir(tmpDir);
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("checkstyle:MagicNumber")
    public void throwExceptionOnParallelRunningExport() throws Throwable {
        extractor1.setDurationMilliSec(2000);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(() -> exportExecutor.doJob(mockedContext));
        Thread.sleep(500);
        try {
            executorService.submit(() -> exportExecutor.doJob(mockedContext)).get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test
    public void allExportFilesAreGzipFiles() throws IOException {
        extractor1.setContent("file1.xml content");
        extractor2.setContent("file2.xml content");
        extractor3.setContent("file3.xml content");
        extractor4.setContent("file4.xml content");
        exportExecutor.doJob(mockedContext);
        Assert.assertFalse(failedListener.isAnyFailed());

        String md5SumsExpected = buildMD5SUMS("file1.xml content", "file2.xml content",
            "file3.xml content", "file4.xml content",
            exportRegistry.getFolderName());
        Assert.assertEquals(exportRegistry.getFolderName(), readFile("dump_session_id.gz"));
        Assert.assertEquals(md5SumsExpected, readFile("MD5SUMS.gz"));
        Assert.assertEquals("file1.xml content", readFile("file1.xml.gz"));
        Assert.assertEquals("file2.xml content", readFile("dirA/file2.xml.gz"));
        Assert.assertEquals("file3.xml content", readFile("dirA/dirB/file3.xml.gz"));
        Assert.assertEquals("file4.xml content", readFile("dirA/dirB/file4.xml.gz"));
    }

    @Test
    public void s3KeyForExportFileHaveGzipSuffix() {
        exportExecutor.doJob(mockedContext);
        Assert.assertFalse(failedListener.isAnyFailed());
        String sessionPrefix = exportRegistry.getDumpName() + "/" + exportRegistry.getFolderName();
        Assert.assertTrue(amazonS3.doesObjectExist("dump_bucket", sessionPrefix + "/MD5SUMS.gz"));
        Assert.assertTrue(amazonS3.doesObjectExist("dump_bucket", sessionPrefix + "/file1.xml.gz"));
        Assert.assertTrue(amazonS3.doesObjectExist("dump_bucket", sessionPrefix + "/dirA/file2.xml.gz"));
        Assert.assertTrue(amazonS3.doesObjectExist("dump_bucket", sessionPrefix + "/dirA/dirB/file3.xml.gz"));
        Assert.assertTrue(amazonS3.doesObjectExist("dump_bucket", sessionPrefix + "/dirA/dirB/file4.xml.gz"));
    }

    private String getStringFromS3(String key) {
        try {
            return StreamUtils.copyToString(
                new GZIPInputStream(amazonS3.getObject("", key).getObjectContent()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void s3SwitchFilesTest() throws IOException, InterruptedException {
        if (OsUtils.isWindows()) {
            return;
        }
        dirBGroup.setOnFailureReaction(SWITCH_TO_PREV_FILES);

        extractor1.setContent("SESSION_1: file1.xml content");
        extractor2.setContent("SESSION_1: file2.xml content");
        extractor3.setContent("SESSION_1: file3.xml content");
        extractor4.setContent("SESSION_1: file4.xml content");

        exportExecutor.setIsSaveDirectToS3(true);
        exportExecutor.setIsSwitchToPrevInS3(true);
        exportExecutor.doJob(mockedContext);
        Assert.assertFalse(failedListener.isAnyFailed());

        String firstSessionId = amazonS3.listObjects("", DUMP_NAME).getObjectSummaries().get(0).
            getKey().substring(DUMP_NAME.length() + 1, DUMP_NAME.length() + 20);
        Thread.sleep(100); // гарантируем что у новой сессии будет другой id

        extractor1.setContent("SESSION_2: file1.xml content");
        extractor2.setContent("SESSION_2: file2.xml content");
        extractor3.setContent("SESSION_2: file3.xml content");
        extractor3.failOnValidate(true);
        extractor4.setContent("SESSION_2: file4.xml content");
        exportExecutor.doJob(mockedContext);
        Assert.assertTrue(failedListener.isAnyFailed());

        List<S3ObjectSummary> summaryList = amazonS3.listObjects("", DUMP_NAME).getObjectSummaries();
        String secondSessionId = null;
        int i = 0;
        while (i < summaryList.size()) {
            secondSessionId = summaryList.get(i).getKey().substring(DUMP_NAME.length() + 1, DUMP_NAME.length() + 20);
            if (!secondSessionId.equals(firstSessionId)) {
                break;
            }
            i++;
        }
        String firstPrefix = DUMP_NAME + "/" + firstSessionId + "/";
        String secondPrefix = DUMP_NAME + "/" + secondSessionId + "/";

        String contentFile1FirstSession = getStringFromS3(firstPrefix + "file1.xml.gz");
        String contentFile2FirstSession = getStringFromS3(firstPrefix + "dirA/file2.xml.gz");
        String contentFile3FirstSession = getStringFromS3(firstPrefix + "dirA/dirB/file3.xml.gz");
        String contentFile4FirstSession = getStringFromS3(firstPrefix + "dirA/dirB/file4.xml.gz");

        String contentFile1SecondSession = getStringFromS3(secondPrefix + "file1.xml.gz");
        String contentFile2SecondSession = getStringFromS3(secondPrefix + "dirA/file2.xml.gz");
        String contentFile3SecondSession = getStringFromS3(secondPrefix + "dirA/dirB/file3.xml.gz");
        String contentFile4SecondSession = getStringFromS3(secondPrefix + "dirA/dirB/file4.xml.gz");

        Assert.assertNotEquals(contentFile1FirstSession, contentFile1SecondSession);
        Assert.assertNotEquals(contentFile2FirstSession, contentFile2SecondSession);
        Assert.assertEquals(contentFile3FirstSession, contentFile3SecondSession);
        Assert.assertEquals(contentFile4FirstSession, contentFile4SecondSession);

        Assert.assertEquals("SESSION_2: file1.xml content", contentFile1SecondSession);
        Assert.assertEquals("SESSION_2: file2.xml content", contentFile2SecondSession);
        Assert.assertEquals("SESSION_1: file3.xml content", contentFile3SecondSession);
        Assert.assertEquals("SESSION_1: file4.xml content", contentFile4SecondSession);

        Assert.assertEquals("SESSION_1: file1.xml content", contentFile1FirstSession);
        Assert.assertEquals("SESSION_1: file2.xml content", contentFile2FirstSession);
        Assert.assertEquals("SESSION_1: file3.xml content", contentFile3FirstSession);
        Assert.assertEquals("SESSION_1: file4.xml content", contentFile4FirstSession);

        String md5SumsExpected = buildMD5SUMS(
            "SESSION_2: file1.xml content",
            "SESSION_2: file2.xml content",
            "SESSION_1: file3.xml content",
            "SESSION_1: file4.xml content",
            exportRegistry.getFolderName()
        );
        Assert.assertEquals(md5SumsExpected, readFile("MD5SUMS.gz"));

    }

    @Test
    public void switchToPrevExpectedSwitchDirB() throws IOException, InterruptedException {
        if (OsUtils.isWindows()) {
            return;
        }
        dirBGroup.setOnFailureReaction(SWITCH_TO_PREV);

        extractor1.setContent("SESSION_1: file1.xml content");
        extractor2.setContent("SESSION_1: file2.xml content");
        extractor3.setContent("SESSION_1: file3.xml content");
        extractor4.setContent("SESSION_1: file4.xml content");
        exportExecutor.doJob(mockedContext);
        Assert.assertFalse(failedListener.isAnyFailed());

        // что бы проверить переключение директории выгрузки на предыдущую сессию
        // в случае когда предыдущая сессия есть на диске, удалим ее из s3
        String sessionPrefix = exportRegistry.getDumpName() + "/" + exportRegistry.getFolderName();
        amazonS3.deleteObject("dump_bucket", sessionPrefix + "/MD5SUMS.gz");
        amazonS3.deleteObject("dump_bucket", sessionPrefix + "/file1.xml.gz");
        amazonS3.deleteObject("dump_bucket", sessionPrefix + "/dirA/file2.xml.gz");
        amazonS3.deleteObject("dump_bucket", sessionPrefix + "/dirA/dirB/file3.xml.gz");
        amazonS3.deleteObject("dump_bucket", sessionPrefix + "/dirA/dirB/file3.xml.gz");

        Thread.sleep(100); // гарантируем что у новой сессии будет другой id

        extractor1.setContent("SESSION_2: file1.xml content");
        extractor2.setContent("SESSION_2: file2.xml content");
        extractor3.setContent("SESSION_2: file3.xml content");
        extractor3.failOnValidate(true);
        extractor4.setContent("SESSION_2: file4.xml content");
        exportExecutor.doJob(mockedContext);
        Assert.assertTrue(failedListener.isAnyFailed());

        String md5SumsExpected = buildMD5SUMS(
            "SESSION_2: file1.xml content",
            "SESSION_2: file2.xml content",
            "SESSION_1: file3.xml content",
            "SESSION_1: file4.xml content",
            exportRegistry.getFolderName()
        );
        Assert.assertEquals(md5SumsExpected, readFile("MD5SUMS.gz"));
        Assert.assertEquals("SESSION_2: file1.xml content", readFile("file1.xml.gz"));
        Assert.assertEquals("SESSION_2: file2.xml content", readFile("dirA/file2.xml.gz"));
        Assert.assertEquals("SESSION_1: file3.xml content", readFile("dirA/dirB/file3.xml.gz"));
        Assert.assertEquals("SESSION_1: file4.xml content", readFile("dirA/dirB/file4.xml.gz"));
    }

    @Test
    public void switchToPrevWithDownloadPrevSessionExpectedSwitchDirB() throws IOException, InterruptedException {
        if (OsUtils.isWindows()) {
            return;
        }
        dirBGroup.setOnFailureReaction(SWITCH_TO_PREV);

        extractor1.setContent("SESSION_1: file1.xml content");
        extractor2.setContent("SESSION_1: file2.xml content");
        extractor3.setContent("SESSION_1: file3.xml content");
        extractor4.setContent("SESSION_1: file4.xml content");
        exportExecutor.doJob(mockedContext);
        Assert.assertFalse(failedListener.isAnyFailed());

        // что бы проверить переключение директории выгрузки на предыдущую сессию
        // в случае когда предыдущей сессии нет на диске, удалим ее из диска
        deleteDir(exportRegistry.getRootDir().toPath());

        Thread.sleep(100); // гарантируем что у новой сессии будет другой id

        extractor1.setContent("SESSION_2: file1.xml content");
        extractor2.setContent("SESSION_2: file2.xml content");
        extractor3.setContent("SESSION_2: file3.xml content");
        extractor3.failOnValidate(true);
        extractor4.setContent("SESSION_2: file4.xml content");
        exportExecutor.doJob(mockedContext);
        Assert.assertTrue(failedListener.isAnyFailed());

        String md5SumsExpected = buildMD5SUMS(
            "SESSION_2: file1.xml content",
            "SESSION_2: file2.xml content",
            "SESSION_1: file3.xml content",
            "SESSION_1: file4.xml content",
            exportRegistry.getFolderName()
        );
        Assert.assertEquals(md5SumsExpected, readFile("MD5SUMS.gz"));
        Assert.assertEquals("SESSION_2: file1.xml content", readFile("file1.xml.gz"));
        Assert.assertEquals("SESSION_2: file2.xml content", readFile("dirA/file2.xml.gz"));
        Assert.assertEquals("SESSION_1: file3.xml content", readFile("dirA/dirB/file3.xml.gz"));
        Assert.assertEquals("SESSION_1: file4.xml content", readFile("dirA/dirB/file4.xml.gz"));
    }

    @Test
    public void switchToPrevFilesExpectedSwitchFile3() throws IOException, InterruptedException {
        if (OsUtils.isWindows()) {
            return;
        }
        dirBGroup.setOnFailureReaction(OnFailureReaction.SWITCH_TO_PREV_FILES);

        extractor1.setContent("SESSION_1: file1.xml content");
        extractor2.setContent("SESSION_1: file2.xml content");
        extractor3.setContent("SESSION_1: file3.xml content");
        extractor4.setContent("SESSION_1: file4.xml content");

        YtBaseSwitcher switcher = Mockito.mock(YtBaseSwitcher.class);
        extractor3.setYtSwitcher(switcher);

        exportExecutor.doJob(mockedContext);
        Assert.assertFalse(failedListener.isAnyFailed());

        Thread.sleep(100); // гарантируем что у новой сессии будет другой id

        extractor1.setContent("SESSION_2: file1.xml content");
        extractor2.setContent("SESSION_2: file2.xml content");
        extractor3.setContent("SESSION_2: file3.xml content");
        extractor3.setRegisterFailedFile(true);
        extractor3.failOnValidate(true);
        extractor4.setContent("SESSION_2: file4.xml content");
        exportExecutor.doJob(mockedContext);
        Mockito.verify(switcher, times(1)).switchFiles(any(YPath.class), any(YPath.class));
        Assert.assertTrue(failedListener.isAnyFailed());

        String md5SumsExpected = buildMD5SUMS(
            "SESSION_2: file1.xml content",
            "SESSION_2: file2.xml content",
            "SESSION_1: file3.xml content",
            "SESSION_2: file4.xml content",
            exportRegistry.getFolderName()
        );
        Assert.assertEquals(md5SumsExpected, readFile("MD5SUMS.gz"));
        Assert.assertEquals("SESSION_2: file1.xml content", readFile("file1.xml.gz"));
        Assert.assertEquals("SESSION_2: file2.xml content", readFile("dirA/file2.xml.gz"));
        Assert.assertEquals("SESSION_1: file3.xml content", readFile("dirA/dirB/file3.xml.gz"));
        Assert.assertEquals("SESSION_2: file4.xml content", readFile("dirA/dirB/file4.xml.gz"));
    }

    @Test(expected = RuntimeException.class)
    public void switchToPrevFilesAndFail() throws IOException, InterruptedException {
        if (OsUtils.isWindows()) {
            throw new RuntimeException("Test is ignored on Windows :)))");
        }
        dirBGroup.setOnFailureReaction(OnFailureReaction.SWITCH_TO_PREV_FILES);

        extractor1.setContent("SESSION_1: file1.xml content");
        extractor2.setContent("SESSION_1: file2.xml content");
        extractor3.setContent("SESSION_1: file3.xml content");
        extractor4.setContent("SESSION_1: file4.xml content");
        exportExecutor.doJob(mockedContext);
        Assert.assertFalse(failedListener.isAnyFailed());

        Thread.sleep(100); // гарантируем что у новой сессии будет другой id

        extractor1.setContent("SESSION_2: file1.xml content");
        extractor2.setContent("SESSION_2: file2.xml content");
        extractor3.setContent("SESSION_2: file3.xml content");
        extractor3.setYtSwitcher(new YtSwitcherFailTestImpl());
        extractor3.failOnValidate(true);
        extractor4.setContent("SESSION_2: file4.xml content");
        exportExecutor.doJob(mockedContext);
    }

    @Test
    public void switchToPrevFilesWithFallbackSwitchToPrevExpectedSwitchDirB() throws IOException, InterruptedException {
        if (OsUtils.isWindows()) {
            return;
        }
        dirBGroup.setOnFailureReaction(OnFailureReaction.SWITCH_TO_PREV_FILES);

        extractor1.setContent("SESSION_1: file1.xml content");
        extractor2.setContent("SESSION_1: file2.xml content");
        extractor3.setContent("SESSION_1: file3.xml content");
        extractor4.setContent("SESSION_1: file4.xml content");
        exportExecutor.doJob(mockedContext);
        Assert.assertFalse(failedListener.isAnyFailed());

        Thread.sleep(100); // гарантируем что у новой сессии будет другой id

        extractor1.setContent("SESSION_2: file1.xml content");
        extractor2.setContent("SESSION_2: file2.xml content");
        extractor3.setContent("SESSION_2: file3.xml content");
        extractor3.failOnValidate(true);
        extractor4.setContent("SESSION_2: file4.xml content");
        exportExecutor.doJob(mockedContext);
        Assert.assertTrue(failedListener.isAnyFailed());

        String md5SumsExpected = buildMD5SUMS(
            "SESSION_2: file1.xml content",
            "SESSION_2: file2.xml content",
            "SESSION_1: file3.xml content",
            "SESSION_1: file4.xml content",
            exportRegistry.getFolderName()
        );
        Assert.assertEquals(md5SumsExpected, readFile("MD5SUMS.gz"));
        Assert.assertEquals("SESSION_2: file1.xml content", readFile("file1.xml.gz"));
        Assert.assertEquals("SESSION_2: file2.xml content", readFile("dirA/file2.xml.gz"));
        Assert.assertEquals("SESSION_1: file3.xml content", readFile("dirA/dirB/file3.xml.gz"));
        Assert.assertEquals("SESSION_1: file4.xml content", readFile("dirA/dirB/file4.xml.gz"));
    }

    @Test
    public void failedParallelExtractor() {
        // нужен успешный экспорт, чтобы выгрузка переключилась на него после фейла
        exportExecutor.doJob(mockedContext);

        List<AbstractExtractor> extractors = new ArrayList<>(rootGroup.getExtractors());
        extractors.add(new TestBrokenParallelExtractor("broken"));
        rootGroup.setExtractors(extractors);

        exportExecutor.doJob(mockedContext);
        Assert.assertTrue(failedListener.isFailed("broken"));
    }

    @Test
    @Ignore
    public void failedWithJvmErrorMarkedAsFailure() {
        // нужен успешный экспорт, чтобы выгрузка переключилась на него после фейла
        exportExecutor.doJob(mockedContext);

        List<AbstractExtractor> extractors = new ArrayList<>(rootGroup.getExtractors());
        extractors.clear();
        extractors.add(new OutOfMemoryBrokenParallelExtractor("oom"));
        rootGroup.setExtractors(extractors);

        exportExecutor.doJob(mockedContext);
        Assert.assertTrue(failedListener.isFailed("oom"));
    }

    @Test
    public void markSessionStartedOnStartup() {
        final List<SessionInfo> sessionInfosAtProcess = new ArrayList<>();
        rootGroup.setExtractors(Collections.singletonList(new AbstractExtractor() {
            @Override
            public void perform(String dir) {
                sessionInfosAtProcess.addAll(dumpStorageInfoService.getSessionInfos(DUMP_NAME));
            }

            @Override
            public int filesToBeExtracted() {
                return 0;
            }
        }));


        exportExecutor.doJob(mockedContext);


        List<SessionInfo> sessionInfosAfter = dumpStorageInfoService.getSessionInfos(DUMP_NAME);
        assertThat(sessionInfosAfter).hasSize(1);
        assertThat(sessionInfosAtProcess).hasSize(1);
        assertThat(sessionInfosAtProcess.get(0).getStatus()).isEqualTo(MboDumpSessionStatus.STARTED);
        assertThat(sessionInfosAtProcess.get(0).getSessionId()).isEqualTo(sessionInfosAfter.get(0).getSessionId());
    }

    @Test
    public void allExtractorsStartedAllTasksShouldBeInterrupted() {
        CountDownLatch allExtractorsAlreadyStartedGate = new CountDownLatch(extractorsNumber);

        rootGroup = new ExtractorsGroup();
        rootGroup.setOnFailureReaction(REGISTER_FAILURE);
        CommonExtractorForInterruptionTesting rootGroupExtractor = Mockito.spy(
            new CommonExtractorForInterruptionTesting(waitingConsumer, allExtractorsAlreadyStartedGate));
        CommonExtractorForInterruptionTesting bGroupExtractor = Mockito.spy(
            new CommonExtractorForInterruptionTesting(interruptingConsumer, allExtractorsAlreadyStartedGate));
        CommonExtractorForInterruptionTesting cGroupExtractor = Mockito.spy(
            new CommonExtractorForInterruptionTesting(waitingConsumer, allExtractorsAlreadyStartedGate));
        CommonExtractorForInterruptionTesting dGroupExtractor = Mockito.spy(
            new CommonExtractorForInterruptionTesting(waitingConsumer, allExtractorsAlreadyStartedGate));
        CommonExtractorForInterruptionTesting eGroupExtractor = Mockito.spy(
            new CommonExtractorForInterruptionTesting(waitingConsumer, allExtractorsAlreadyStartedGate));

        rootGroup.setExtractors(Collections.singletonList(rootGroupExtractor));
        ExtractorsGroup bLayer = new ExtractorsGroup();
        bLayer.setOnFailureReaction(REGISTER_FAILURE);
        bLayer.setDir("dirB");
        bLayer.setExtractors(Collections.singletonList(bGroupExtractor));
        ExtractorsGroup cLayer = new ExtractorsGroup();
        cLayer.setOnFailureReaction(REGISTER_FAILURE);
        cLayer.setDir("dirC");
        cLayer.setExtractors(Collections.singletonList(cGroupExtractor));

        ExtractorsGroup dLayer = new ExtractorsGroup();
        dLayer.setOnFailureReaction(SWITCH_TO_PREV);
        dLayer.setDir("dirD");
        dLayer.setExtractors(Collections.singletonList(dGroupExtractor));
        ExtractorsGroup eLayer = new ExtractorsGroup();
        eLayer.setOnFailureReaction(SWITCH_TO_PREV);
        eLayer.setDir("dirE");
        eLayer.setExtractors(Collections.singletonList(eGroupExtractor));

        rootGroup.setSubgroups(Collections.singletonList(bLayer));
        bLayer.setSubgroups(Arrays.asList(cLayer, dLayer));
        cLayer.setSubgroups(Collections.singletonList(eLayer));

        exportExecutor.setRootExtractorsGroup(rootGroup);
        exportExecutor.setPreExecutors(Collections.singletonList(new PreExecutor() {
            @Override
            public void execute(JobExecutionContext context, ExportRegistry registry) {
                try {
                    MD5SUMS.writeMd5Sums(exportRegistry.getRootDir().toPath(), new HashMap<>());
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public Type getType() {
                return null;
            }
        }));

        exportExecutor.doJob(mockedContext);

        assertThat(rootGroupExtractor.isCompletedExceptionally).isTrue();
        assertThat(bGroupExtractor.isCompletedExceptionally).isTrue();
        assertThat(cGroupExtractor.isCompletedExceptionally).isTrue();
        assertThat(dGroupExtractor.isCompletedExceptionally).isTrue();
        assertThat(eGroupExtractor.isCompletedExceptionally).isTrue();
    }

    @Test
    public void interruptionOfNotOnFailureExecutorShouldNotInterruptExecutors() {
        CountDownLatch allExtractorsAlreadyStartedGate = new CountDownLatch(extractorsNumber);

        rootGroup = new ExtractorsGroup();
        rootGroup.setOnFailureReaction(REGISTER_FAILURE);

        CommonExtractorForInterruptionTesting rootGroupExtractor = Mockito.spy(
            new CommonExtractorForInterruptionTesting(waitingConsumer, allExtractorsAlreadyStartedGate));
        CommonExtractorForInterruptionTesting bGroupExtractor = Mockito.spy(
            new CommonExtractorForInterruptionTesting(waitingConsumer, allExtractorsAlreadyStartedGate));
        CommonExtractorForInterruptionTesting cGroupExtractor = Mockito.spy(
            new CommonExtractorForInterruptionTesting(waitingConsumer, allExtractorsAlreadyStartedGate));
        CommonExtractorForInterruptionTesting dGroupExtractor = Mockito.spy(
            new CommonExtractorForInterruptionTesting(interruptingConsumer, allExtractorsAlreadyStartedGate));
        CommonExtractorForInterruptionTesting eGroupExtractor = Mockito.spy(
            new CommonExtractorForInterruptionTesting(waitingConsumer, allExtractorsAlreadyStartedGate));

        rootGroup.setExtractors(Collections.singletonList(
            rootGroupExtractor));
        ExtractorsGroup bLayer = new ExtractorsGroup();
        bLayer.setOnFailureReaction(REGISTER_FAILURE);
        bLayer.setDir("dirB");
        bLayer.setExtractors(Collections.singletonList(bGroupExtractor));
        ExtractorsGroup cLayer = new ExtractorsGroup();
        cLayer.setOnFailureReaction(REGISTER_FAILURE);
        cLayer.setDir("dirC");
        cLayer.setExtractors(Collections.singletonList(cGroupExtractor));
        ExtractorsGroup dLayer = new ExtractorsGroup();
        dLayer.setOnFailureReaction(SWITCH_TO_PREV);
        dLayer.setDir("dirD");
        dLayer.setExtractors(Collections.singletonList(dGroupExtractor));
        ExtractorsGroup eLayer = new ExtractorsGroup();
        eLayer.setOnFailureReaction(SWITCH_TO_PREV);
        eLayer.setDir("dirE");
        eLayer.setExtractors(Collections.singletonList(eGroupExtractor));

        rootGroup.setSubgroups(Collections.singletonList(bLayer));
        bLayer.setSubgroups(Arrays.asList(cLayer, dLayer));
        cLayer.setSubgroups(Collections.singletonList(eLayer));

        exportExecutor.setRootExtractorsGroup(rootGroup);
        exportExecutor.setPreExecutors(Collections.singletonList(new PreExecutor() {
            @Override
            public void execute(JobExecutionContext context, ExportRegistry registry) {
                try {
                    MD5SUMS.writeMd5Sums(exportRegistry.getRootDir().toPath(), new HashMap<>());
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public Type getType() {
                return null;
            }
        }));

        exportExecutor.doJob(mockedContext);

        assertThat(rootGroupExtractor.isCompletedExceptionally).isFalse();
        assertThat(bGroupExtractor.isCompletedExceptionally).isFalse();
        assertThat(cGroupExtractor.isCompletedExceptionally).isFalse();
        assertThat(dGroupExtractor.isCompletedExceptionally).isTrue();
        assertThat(eGroupExtractor.isCompletedExceptionally).isFalse();
    }

    //todo будет справлено в https://st.yandex-team.ru/MBO-20535
    @Ignore
    @Test
    public void interruptedParallelExtractorShouldInterruptOtherExtractors() {
        CountDownLatch allExtractorsAlreadyStartedGate = new CountDownLatch(extractorsNumber);

        TestParallelExtractorAllExtractors rootExtractor =
            new TestParallelExtractorAllExtractors(allExtractorsAlreadyStartedGate);
        CommonExtractorForInterruptionTesting bGroupExtractor = Mockito.spy(
            new CommonExtractorForInterruptionTesting(waitingConsumer, allExtractorsAlreadyStartedGate));
        CommonExtractorForInterruptionTesting cGroupExtractor = Mockito.spy(
            new CommonExtractorForInterruptionTesting(waitingConsumer, allExtractorsAlreadyStartedGate));
        CommonExtractorForInterruptionTesting dGroupExtractor = Mockito.spy(
            new CommonExtractorForInterruptionTesting(waitingConsumer, allExtractorsAlreadyStartedGate));
        CommonExtractorForInterruptionTesting eGroupExtractor = Mockito.spy(
            new CommonExtractorForInterruptionTesting(waitingConsumer, allExtractorsAlreadyStartedGate));

        rootGroup = new ExtractorsGroup();
        rootGroup.setOnFailureReaction(REGISTER_FAILURE);
        rootGroup.setExtractors(Collections.singletonList(rootExtractor));

        ExtractorsGroup bLayer = new ExtractorsGroup();
        bLayer.setOnFailureReaction(REGISTER_FAILURE);
        bLayer.setDir("dirB");
        bLayer.setExtractors(Collections.singletonList(bGroupExtractor));

        ExtractorsGroup cLayer = new ExtractorsGroup();
        cLayer.setOnFailureReaction(REGISTER_FAILURE);
        cLayer.setDir("dirC");
        cLayer.setExtractors(Collections.singletonList(cGroupExtractor));

        ExtractorsGroup dLayer = new ExtractorsGroup();
        dLayer.setOnFailureReaction(SWITCH_TO_PREV);
        dLayer.setDir("dirD");
        dLayer.setExtractors(Collections.singletonList(dGroupExtractor));

        ExtractorsGroup eLayer = new ExtractorsGroup();
        eLayer.setOnFailureReaction(SWITCH_TO_PREV);
        eLayer.setDir("dirE");
        eLayer.setExtractors(Collections.singletonList(eGroupExtractor));

        rootGroup.setSubgroups(Arrays.asList(bLayer, cLayer));
        bLayer.setSubgroups(Collections.singletonList(dLayer));
        cLayer.setSubgroups(Collections.singletonList(eLayer));

        exportExecutor.setRootExtractorsGroup(rootGroup);
        exportExecutor.setPreExecutors(Collections.singletonList(new PreExecutor() {
            @Override
            public void execute(JobExecutionContext context, ExportRegistry registry) {
                try {
                    MD5SUMS.writeMd5Sums(exportRegistry.getRootDir().toPath(), new HashMap<>());
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public Type getType() {
                return null;
            }
        }));

        exportExecutor.doJob(mockedContext);

        assertThat(rootExtractor.countExceptionallyCompletedExtractorsTask.get()).isGreaterThanOrEqualTo(2);
        assertThat(bGroupExtractor.isCompletedExceptionally).isTrue();
        assertThat(cGroupExtractor.isCompletedExceptionally).isTrue();
        assertThat(dGroupExtractor.isCompletedExceptionally).isTrue();
        assertThat(eGroupExtractor.isCompletedExceptionally).isTrue();
    }

    @Test
    public void interruptedParallelExtractorShouldNotInterruptOtherExtractors() {
        CountDownLatch allExtractorsAlreadyStartedGate = new CountDownLatch(extractorsNumber);

        CommonExtractorForInterruptionTesting rootExtractor =
            Mockito.spy(new CommonExtractorForInterruptionTesting(waitingConsumer, allExtractorsAlreadyStartedGate));
        CommonExtractorForInterruptionTesting bGroupExtractor = Mockito.spy(
            new CommonExtractorForInterruptionTesting(waitingConsumer, allExtractorsAlreadyStartedGate));
        CommonExtractorForInterruptionTesting cGroupExtractor = Mockito.spy(
            new CommonExtractorForInterruptionTesting(waitingConsumer, allExtractorsAlreadyStartedGate));
        CommonExtractorForInterruptionTesting dGroupExtractor = Mockito.spy(
            new CommonExtractorForInterruptionTesting(waitingConsumer, allExtractorsAlreadyStartedGate));
        TestParallelExtractorAllExtractors eGroupExtractor = Mockito.spy(
            new TestParallelExtractorAllExtractors(allExtractorsAlreadyStartedGate));

        rootGroup = new ExtractorsGroup();
        rootGroup.setOnFailureReaction(REGISTER_FAILURE);
        rootGroup.setExtractors(Collections.singletonList(rootExtractor));

        ExtractorsGroup bLayer = new ExtractorsGroup();
        bLayer.setOnFailureReaction(REGISTER_FAILURE);
        bLayer.setDir("dirB");
        bLayer.setExtractors(Collections.singletonList(bGroupExtractor));

        ExtractorsGroup cLayer = new ExtractorsGroup();
        cLayer.setOnFailureReaction(REGISTER_FAILURE);
        cLayer.setDir("dirC");
        cLayer.setExtractors(Collections.singletonList(cGroupExtractor));

        ExtractorsGroup dLayer = new ExtractorsGroup();
        dLayer.setOnFailureReaction(SWITCH_TO_PREV);
        dLayer.setDir("dirD");
        dLayer.setExtractors(Collections.singletonList(dGroupExtractor));
        ExtractorsGroup eLayer = new ExtractorsGroup();
        eLayer.setOnFailureReaction(SWITCH_TO_PREV);
        eLayer.setDir("dirE");
        eLayer.setExtractors(Collections.singletonList(eGroupExtractor));

        rootGroup.setSubgroups(Arrays.asList(bLayer, cLayer));
        bLayer.setSubgroups(Collections.singletonList(dLayer));
        cLayer.setSubgroups(Collections.singletonList(eLayer));

        exportExecutor.setRootExtractorsGroup(rootGroup);
        exportExecutor.setPreExecutors(Collections.singletonList(new PreExecutor() {
            @Override
            public void execute(JobExecutionContext context, ExportRegistry registry) {
                try {
                    MD5SUMS.writeMd5Sums(exportRegistry.getRootDir().toPath(), new HashMap<>());
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public Type getType() {
                return null;
            }
        }));

        exportExecutor.doJob(mockedContext);

        assertThat(eGroupExtractor.countExceptionallyCompletedExtractorsTask.get()).isEqualTo(1);
        assertThat(rootExtractor.isCompletedExceptionally).isFalse();
        assertThat(bGroupExtractor.isCompletedExceptionally).isFalse();
        assertThat(cGroupExtractor.isCompletedExceptionally).isFalse();
        assertThat(dGroupExtractor.isCompletedExceptionally).isFalse();
    }

    //todo будет справлено в https://st.yandex-team.ru/MBO-20535
    @Ignore
    @Test
    public void interruptedCommonExtractorShouldInterruptOtherExtractors() {
        CountDownLatch allExtractorsAlreadyStartedGate = new CountDownLatch(extractorsNumber);

        CommonExtractorForInterruptionTesting rootExtractor =
            Mockito.spy(new CommonExtractorForInterruptionTesting(waitingConsumer, allExtractorsAlreadyStartedGate));
        CommonExtractorForInterruptionTesting bGroupExtractor = Mockito.spy(
            new CommonExtractorForInterruptionTesting(interruptingConsumer, allExtractorsAlreadyStartedGate));
        CommonExtractorForInterruptionTesting cGroupExtractor = Mockito.spy(
            new CommonExtractorForInterruptionTesting(waitingConsumer, allExtractorsAlreadyStartedGate));
        CommonExtractorForInterruptionTesting dGroupExtractor = Mockito.spy(
            new CommonExtractorForInterruptionTesting(waitingConsumer, allExtractorsAlreadyStartedGate));
        TestParallelExtractorAllExtractors eGroupExtractor = Mockito.spy(
            new TestParallelExtractorAllExtractors(allExtractorsAlreadyStartedGate, true));

        rootGroup = new ExtractorsGroup();
        rootGroup.setOnFailureReaction(REGISTER_FAILURE);
        rootGroup.setExtractors(Collections.singletonList(rootExtractor));

        ExtractorsGroup bLayer = new ExtractorsGroup();
        bLayer.setOnFailureReaction(REGISTER_FAILURE);
        bLayer.setDir("dirB");
        bLayer.setExtractors(Collections.singletonList(bGroupExtractor));

        ExtractorsGroup cLayer = new ExtractorsGroup();
        cLayer.setOnFailureReaction(REGISTER_FAILURE);
        cLayer.setDir("dirC");
        cLayer.setExtractors(Collections.singletonList(cGroupExtractor));

        ExtractorsGroup dLayer = new ExtractorsGroup();
        dLayer.setOnFailureReaction(SWITCH_TO_PREV);
        dLayer.setDir("dirD");
        dLayer.setExtractors(Collections.singletonList(dGroupExtractor));
        ExtractorsGroup eLayer = new ExtractorsGroup();
        eLayer.setOnFailureReaction(SWITCH_TO_PREV);
        eLayer.setDir("dirE");
        eLayer.setExtractors(Collections.singletonList(eGroupExtractor));

        rootGroup.setSubgroups(Arrays.asList(bLayer, cLayer));
        bLayer.setSubgroups(Collections.singletonList(dLayer));
        cLayer.setSubgroups(Collections.singletonList(eLayer));

        exportExecutor.setRootExtractorsGroup(rootGroup);
        exportExecutor.setPreExecutors(Collections.singletonList(new PreExecutor() {
            @Override
            public void execute(JobExecutionContext context, ExportRegistry registry) {
                try {
                    MD5SUMS.writeMd5Sums(exportRegistry.getRootDir().toPath(), new HashMap<>());
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public Type getType() {
                return null;
            }
        }));

        exportExecutor.doJob(mockedContext);

        assertThat(eGroupExtractor.countExceptionallyCompletedExtractorsTask.get()).isEqualTo(2);
        assertThat(rootExtractor.isCompletedExceptionally).isTrue();
        assertThat(bGroupExtractor.isCompletedExceptionally).isTrue();
        assertThat(cGroupExtractor.isCompletedExceptionally).isTrue();
        assertThat(dGroupExtractor.isCompletedExceptionally).isTrue();
    }

    @Test
    public void interruptedCommonExtractorShouldNotInterruptOtherExtractors() {
        CountDownLatch allExtractorsAlreadyStartedGate = new CountDownLatch(extractorsNumber);

        CommonExtractorForInterruptionTesting rootExtractor =
            Mockito.spy(new CommonExtractorForInterruptionTesting(waitingConsumer, allExtractorsAlreadyStartedGate));
        CommonExtractorForInterruptionTesting bGroupExtractor = Mockito.spy(
            new CommonExtractorForInterruptionTesting(waitingConsumer, allExtractorsAlreadyStartedGate));
        CommonExtractorForInterruptionTesting cGroupExtractor = Mockito.spy(
            new CommonExtractorForInterruptionTesting(waitingConsumer, allExtractorsAlreadyStartedGate));
        CommonExtractorForInterruptionTesting dGroupExtractor = Mockito.spy(
            new CommonExtractorForInterruptionTesting(interruptingConsumer, allExtractorsAlreadyStartedGate));
        TestParallelExtractorAllExtractors eGroupExtractor = Mockito.spy(
            new TestParallelExtractorAllExtractors(allExtractorsAlreadyStartedGate, true));

        rootGroup = new ExtractorsGroup();
        rootGroup.setOnFailureReaction(REGISTER_FAILURE);
        rootGroup.setExtractors(Collections.singletonList(rootExtractor));

        ExtractorsGroup bLayer = new ExtractorsGroup();
        bLayer.setOnFailureReaction(REGISTER_FAILURE);
        bLayer.setDir("dirB");
        bLayer.setExtractors(Collections.singletonList(bGroupExtractor));

        ExtractorsGroup cLayer = new ExtractorsGroup();
        cLayer.setOnFailureReaction(REGISTER_FAILURE);
        cLayer.setDir("dirC");
        cLayer.setExtractors(Collections.singletonList(cGroupExtractor));

        ExtractorsGroup dLayer = new ExtractorsGroup();
        dLayer.setOnFailureReaction(SWITCH_TO_PREV);
        dLayer.setDir("dirD");
        dLayer.setExtractors(Collections.singletonList(dGroupExtractor));
        ExtractorsGroup eLayer = new ExtractorsGroup();
        eLayer.setOnFailureReaction(SWITCH_TO_PREV);
        eLayer.setDir("dirE");
        eLayer.setExtractors(Collections.singletonList(eGroupExtractor));

        rootGroup.setSubgroups(Arrays.asList(bLayer, cLayer));
        bLayer.setSubgroups(Collections.singletonList(dLayer));
        cLayer.setSubgroups(Collections.singletonList(eLayer));

        exportExecutor.setRootExtractorsGroup(rootGroup);
        exportExecutor.setPreExecutors(Collections.singletonList(new PreExecutor() {
            @Override
            public void execute(JobExecutionContext context, ExportRegistry registry) {
                try {
                    MD5SUMS.writeMd5Sums(exportRegistry.getRootDir().toPath(), new HashMap<>());
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public Type getType() {
                return null;
            }
        }));

        exportExecutor.doJob(mockedContext);

        assertThat(eGroupExtractor.countExceptionallyCompletedExtractorsTask.get()).isEqualTo(0);
        assertThat(rootExtractor.isCompletedExceptionally).isFalse();
        assertThat(bGroupExtractor.isCompletedExceptionally).isFalse();
        assertThat(cGroupExtractor.isCompletedExceptionally).isFalse();
        assertThat(dGroupExtractor.isCompletedExceptionally).isTrue();
    }


    private String readFile(String fileName) throws IOException {
        Path file = exportRegistry.getRootDir().toPath().resolve(fileName);
        try (GZIPInputStream input = new GZIPInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            return new String(ByteStreams.toByteArray(input), StandardCharsets.UTF_8);
        }
    }

    private static String buildMD5SUMS(String file1, String file2, String file3, String file4, String sessionid) {
        return calculateMd5(file3) + "  dirA/dirB/file3.xml\n" +
            calculateMd5(file4) + "  dirA/dirB/file4.xml\n" +
            calculateMd5(file2) + "  dirA/file2.xml\n" +
            calculateMd5(sessionid) + "  dump_session_id\n" +
            calculateMd5(file1) + "  file1.xml\n";
    }

    private static String calculateMd5(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(text.getBytes());
            final BigInteger number = new BigInteger(1, digest.digest());
            return String.format("%032x", number);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static void deleteDir(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static class TestExtractor extends BaseExtractor {
        private String fileName;
        private String content;
        private TestExtractorValidator validator;
        private boolean registerFailedFile; // нужно для тестирования  OnFailureReaction.SWITCH_TO_PREV_FILES
        private Integer durationMilliSec;

        TestExtractor(String fileName, S3DumpStorageCoreService s3) {
            this.fileName = fileName;
            this.content = "default content";
            this.validator = new TestExtractorValidator(content);
            setOutputFileName(fileName);
            setName(fileName + "_Extractor");
            setEncoding("utf-8");
            setValidator(validator);
            setCompressionLevel(COMPRESSION_LEVEL);
            setExtractorWriterService(new ExtractorWriterService(s3));
        }

        public void setContent(String content) {
            this.content = content;
            validator.setValidContent(content);
        }

        public void failOnValidate(boolean fail) {
            if (fail) {
                validator.setValidContent(null);
            } else {
                validator.setValidContent(content);
            }
        }

        public void setRegisterFailedFile(boolean registerFailedFile) {
            this.registerFailedFile = registerFailedFile;
        }

        public void setDurationMilliSec(Integer durationMilliSec) {
            this.durationMilliSec = durationMilliSec;
        }

        @Override
        public void perform(String dir) throws Exception {
            try {
                Md5Writer writer1;
                try (Md5Writer writer = getWriterInstance(dir)) {
                    writer1 = writer;
                    writer.write(content);
                }

                if (durationMilliSec != null) {
                    Thread.sleep(durationMilliSec);
                }

                registerThis(writer1, dir);
            } catch (Exception e) {
                if (registerFailedFile) {
                    getRegistry().registerFailedFile(getName(), dir + fileName, e);
                }
                throw e;
            }
        }
    }

    private static class TestBrokenParallelExtractor extends ParallelExtractor {

        private TestBrokenParallelExtractor(String name) {
            super();
            setName(name);
        }

        @Override
        protected Collection<Callable<Void>> getTasks(String dir) {
            return Collections.singletonList(() -> null);
        }

        @Override
        protected void extractionFinished(Object o) {
            throw new RuntimeException("test");
        }

        @Override
        public int filesToBeExtracted() {
            return 0;
        }
    }

    private static class OutOfMemoryBrokenParallelExtractor extends ParallelExtractor {

        private OutOfMemoryBrokenParallelExtractor(String name) {
            super();
            setName(name);
        }

        @Override
        protected Collection<Callable<Void>> getTasks(String dir) {
            return Collections.singletonList(() -> {
                byte[] oom = new byte[Integer.MAX_VALUE];
                return null;
            });
        }

        @Override
        protected void extractionFinished(Object o) {
        }

        @Override
        public int filesToBeExtracted() {
            return 0;
        }
    }

    private static class TestExtractorValidator implements ExportFileValidator {
        private String validContent;

        TestExtractorValidator(String validContent) {
            this.validContent = validContent;
        }

        public void setValidContent(String validContent) {
            this.validContent = validContent;
        }

        @Override
        public boolean validate(String fQN) throws ExportFileValidationException {
            if (validContent == null) {
                return false;
            }
            try (Reader reader = ExporterUtils.getReader(new File(fQN))) {
                String actualContent = CharStreams.toString(reader);
                return Objects.equals(validContent, actualContent);
            } catch (IOException e) {
                throw new ExportFileValidationException(e);
            }
        }

        @Override
        public boolean validateS3(S3DumpStorageCoreService s3DumpStorageCoreService, String keyName) {
            return validContent != null;
        }
    }

    private static class FailedListener implements RegistryEventsListener {

        private Set<String> failedWorkers = new HashSet<>();
        private Set<String> finishedWorkers = new HashSet<>();

        public boolean isAnyFailed() {
            return !failedWorkers.isEmpty();
        }

        public boolean isFailed(String workerName) {
            return failedWorkers.contains(workerName) && !finishedWorkers.contains(workerName);
        }

        @Override
        public void fileRegistered(String worker, String fileName, String md5) {

        }

        @Override
        public void processStarted(RegistryStatus status) {

        }

        @Override
        public void processFinished(RegistryStatus status) {

        }

        @Override
        public void workerStarted(String worker) {

        }

        @Override
        public void workerFinished(String worker) {
            finishedWorkers.add(worker);
        }

        @Override
        public void workerFailed(String worker, Exception exception) {
            failedWorkers.add(worker);
        }

        @Override
        public void writeAdditionalInfo(String worker, String info) {

        }
    }

    private static class YtSwitcherFailTestImpl implements YtSwitcher {

        @Override
        public void addFailedCategory(Long hid) {

        }

        @Override
        public void switchFiles(YPath recentSession, YPath currentSession) {
            throw new RuntimeException("Something happened!");
        }
    }


    private static class CommonExtractorForInterruptionTesting extends AbstractExtractor {

        boolean isCompletedExceptionally;
        CountDownLatch interruptionGate = new CountDownLatch(1);

        CountDownLatch allExtractorsAlreadyStartedGate;
        private Consumer<CommonExtractorForInterruptionTesting> performMethod;

        CommonExtractorForInterruptionTesting(Consumer<CommonExtractorForInterruptionTesting> performMethod,
                                              CountDownLatch allExtractorsAlreadyStartedGate) {
            this.allExtractorsAlreadyStartedGate = allExtractorsAlreadyStartedGate;
            this.performMethod = performMethod;
        }

        @Override
        public void perform(String dir) throws Exception {
            performMethod.accept(this);
        }

        @Override
        public int filesToBeExtracted() {
            return 0;
        }
    }


    private static class TestParallelExtractorAllExtractors extends ParallelExtractor {

        private CountDownLatch interruptionGate2 = new CountDownLatch(1);
        private CountDownLatch allExtractorsAlreadyStarted;
        private Collection<? extends Callable<Void>> tasks;

        AtomicInteger countExceptionallyCompletedExtractorsTask = new AtomicInteger(0);
        AtomicInteger countSuccessfullyCompletedExtractorsTask = new AtomicInteger(0);

        private Callable<Void> taskA = () -> {
            allExtractorsAlreadyStarted.countDown();
            allExtractorsAlreadyStarted.await();

            countExceptionallyCompletedExtractorsTask.getAndIncrement();
            throw new RuntimeException();
        };

        private Callable<Void> taskB = () -> {
            allExtractorsAlreadyStarted.countDown();
            allExtractorsAlreadyStarted.await();

            try {
                interruptionGate2.await(MILLISECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (Thread.currentThread().isInterrupted()) {
                countExceptionallyCompletedExtractorsTask.getAndIncrement();
                throw new RuntimeException();
            } else {
                countSuccessfullyCompletedExtractorsTask.getAndIncrement();
            }
            return null;
        };

        TestParallelExtractorAllExtractors(CountDownLatch allExtractorsAlreadyStarted) {
            this(allExtractorsAlreadyStarted, false);
        }

        TestParallelExtractorAllExtractors(CountDownLatch allExtractorsAlreadyStarted, boolean isWaiting) {
            this.allExtractorsAlreadyStarted = allExtractorsAlreadyStarted;
            this.tasks = isWaiting ? Arrays.asList(taskB, taskB) : Arrays.asList(taskA, taskB);
        }

        @Override
        protected Collection<? extends Callable<Void>> getTasks(String dir) {
            return this.tasks;
        }
    }


}
