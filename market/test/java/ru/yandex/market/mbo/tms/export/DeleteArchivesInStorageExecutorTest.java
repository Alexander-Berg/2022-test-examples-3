package ru.yandex.market.mbo.tms.export;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.common.ZooKeeper.ZooKeeperService;
import ru.yandex.market.mbo.common.s3.AmazonS3ClientFactory;
import ru.yandex.market.mbo.export.dumpstorage.ZkDumpStorageInfoService;
import ru.yandex.market.mbo.s3.AmazonS3Mock;
import ru.yandex.market.mbo.synchronizer.export.storage.DumpStorageService;
import ru.yandex.market.mbo.synchronizer.export.storage.ReplicationService;
import ru.yandex.market.mbo.synchronizer.export.storage.S3DumpStorageCoreService;
import ru.yandex.market.mbo.utils.DateFormatUtils;
import ru.yandex.market.mbo.utils.ZooKeeperServiceMock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author vvselishchev@yandex-team.ru
 * @since 18/07/19
 */
public class DeleteArchivesInStorageExecutorTest {

    private static final String BUCKET_NAME = "dump_bucket";
    private static final String ARCHIVE_PREFIX = "archive";
    private static final String CLUSTERS_DUMP_NAME = "clusters";
    private static final String STUFF_DUMP_NAME = "stuff";

    private static final int BATCH_SIZE = 10;
    private static final int MONTHS_TO_DELETE = 3;

    private static final String TMP_DIRECTORY = "mbo-test-";

    private Path tmpDir;
    private DumpStorageService storageService;
    private DeleteArchivesInStorageExecutor executor;
    private AmazonS3 amazonS3;
    private ZkDumpStorageInfoService dumpStorageInfoService;
    private S3DumpStorageCoreService s3StorageCoreService;
    private ReplicationService replicationService;

    @Before
    public void setUp() throws IOException {
        tmpDir = Files.createTempDirectory(TMP_DIRECTORY);

        ZooKeeperService zooKeeperService = new ZooKeeperServiceMock();
        dumpStorageInfoService = Mockito.mock(ZkDumpStorageInfoService.class);
        dumpStorageInfoService.setZooKeeperService(zooKeeperService);
        Mockito.when(dumpStorageInfoService.getDumpNames()).thenReturn(Arrays.asList(
            CLUSTERS_DUMP_NAME,
            STUFF_DUMP_NAME
        ));
        amazonS3 = Mockito.spy(new AmazonS3Mock());
        ((AmazonS3Mock) amazonS3).setBatchSize(BATCH_SIZE);
        AmazonS3ClientFactory alwaysNewS3ClientFactory = Mockito.mock(AmazonS3ClientFactory.class);
        Mockito.when(alwaysNewS3ClientFactory.getS3Client()).thenAnswer(invocation -> amazonS3);
        s3StorageCoreService = new S3DumpStorageCoreService();
        s3StorageCoreService.setS3ClientFactory(alwaysNewS3ClientFactory);
        s3StorageCoreService.setBucketName(BUCKET_NAME);

        replicationService = Mockito.mock(ReplicationService.class);
        storageService = new DumpStorageService(s3StorageCoreService, dumpStorageInfoService,
            null, replicationService, null);
        executor = new DeleteArchivesInStorageExecutor();
        executor.setEnabled(true);
        executor.setPeriodToDeleteInMonths(MONTHS_TO_DELETE);
        executor.setDumpStorage(storageService);
        executor.setDumpInfo(dumpStorageInfoService);
        executor.setLeaveSessions(0);
    }

    private void putDumpArchiveS3(String dumpName, LocalDateTime creationDate) throws IOException {
        putDumpArchiveS3(dumpName, creationDate, "some-file");
    }

    private void putDumpArchiveS3(String dumpName, LocalDateTime creationDate, String fileName) throws IOException {
        String archiveName = getArchiveName(creationDate);
        String key = ARCHIVE_PREFIX + "/" + dumpName + "/" + archiveName + "/" + fileName + ".gz";
        File archive = File.createTempFile("test-", archiveName, tmpDir.toFile());
        amazonS3.putObject(BUCKET_NAME, key, archive);
    }

    private void putEmptyDumpS3(String dumpName) throws IOException {
        String key = ARCHIVE_PREFIX + "/" + dumpName + "/";
        File archive = File.createTempFile("test-", "folder", tmpDir.toFile());
        amazonS3.putObject(BUCKET_NAME, key, archive);
    }

    private String getArchiveName(LocalDateTime someMonthsAgo) {
        return DateFormatUtils.DEFAULT_SESSION_FORMAT.format(someMonthsAgo);
    }

    @After
    public void tearDown() throws Exception {
        deleteDir(tmpDir);
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

    @Test
    public void deleteSingleOldArchiveInOneCluster() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime manyMonthsAgo = now.minusMonths(MONTHS_TO_DELETE + 2);
        putDumpArchiveS3(CLUSTERS_DUMP_NAME, manyMonthsAgo);

        executor.doRealJob(null);

        assertTrue(storageService.listSessionsArchives(CLUSTERS_DUMP_NAME).isEmpty());
    }

    @Test
    public void deleteOldArchiveInOneClusterExactlyNow() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthsAgoSoToDeleteNow = now.minusMonths(MONTHS_TO_DELETE);
        putDumpArchiveS3(CLUSTERS_DUMP_NAME, monthsAgoSoToDeleteNow);

        executor.doRealJob(null);

        assertTrue(storageService.listSessionsArchives(CLUSTERS_DUMP_NAME).isEmpty());
    }

    @Test
    public void deleteOnlyOldArchivesInOneCluster() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime manyMonthsAgo = now.minusMonths(MONTHS_TO_DELETE + 1);
        LocalDateTime evenMoreMonthsAgo = now.minusMonths(MONTHS_TO_DELETE + 2);
        LocalDateTime someMonthsAgo = now.minusMonths(MONTHS_TO_DELETE - 1);
        putDumpArchiveS3(CLUSTERS_DUMP_NAME, someMonthsAgo);
        putDumpArchiveS3(CLUSTERS_DUMP_NAME, manyMonthsAgo);
        putDumpArchiveS3(CLUSTERS_DUMP_NAME, evenMoreMonthsAgo);

        executor.doRealJob(null);

        List<String> expectedArchivesAfterDelete = new ArrayList<>(Collections.singletonList(
            getArchiveName(someMonthsAgo)
        ));

        assertEquals(expectedArchivesAfterDelete, storageService.listSessionsArchives(CLUSTERS_DUMP_NAME));
    }

    @Test
    public void deleteOnlyOldArchivesInSeveralClusters() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime manyMonthsAgo = now.minusMonths(MONTHS_TO_DELETE + 2);
        LocalDateTime someMonthsAgo = now.minusMonths(MONTHS_TO_DELETE - 1);
        putDumpArchiveS3(CLUSTERS_DUMP_NAME, someMonthsAgo);
        putDumpArchiveS3(CLUSTERS_DUMP_NAME, manyMonthsAgo);

        putDumpArchiveS3(STUFF_DUMP_NAME, now);
        putDumpArchiveS3(STUFF_DUMP_NAME, someMonthsAgo);
        putDumpArchiveS3(STUFF_DUMP_NAME, manyMonthsAgo);

        executor.doRealJob(null);

        List<String> expectedArchivesAfterDeleteInClusters = new ArrayList<>(Collections.singletonList(
            getArchiveName(someMonthsAgo)
        ));

        List<String> expectedArchivesAfterDeleteInStuff = new ArrayList<>(Arrays.asList(
            getArchiveName(someMonthsAgo),
            getArchiveName(now)
        ));

        List<String> actualArchivesAfterDeleteInClusters = storageService.listSessionsArchives(CLUSTERS_DUMP_NAME);
        List<String> actualArchivesAfterDeleteInStuff = storageService.listSessionsArchives(STUFF_DUMP_NAME);
        Collections.sort(expectedArchivesAfterDeleteInStuff);
        Collections.sort(actualArchivesAfterDeleteInStuff);

        assertEquals(expectedArchivesAfterDeleteInClusters, actualArchivesAfterDeleteInClusters);
        assertEquals(expectedArchivesAfterDeleteInStuff, actualArchivesAfterDeleteInStuff);
    }


    @Test
    public void noOldArchivesInAnyClusters() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime someMonthsAgo = now.minusMonths(MONTHS_TO_DELETE - 1);
        putDumpArchiveS3(CLUSTERS_DUMP_NAME, someMonthsAgo);

        putDumpArchiveS3(STUFF_DUMP_NAME, now);
        putDumpArchiveS3(STUFF_DUMP_NAME, someMonthsAgo);

        executor.doRealJob(null);

        List<String> expectedArchivesAfterDeleteInClusters = new ArrayList<>(Collections.singletonList(
            getArchiveName(someMonthsAgo)
        ));

        List<String> expectedArchivesAfterDeleteInStuff = new ArrayList<>(Arrays.asList(
            getArchiveName(someMonthsAgo),
            getArchiveName(now)
        ));

        List<String> actualArchivesAfterDeleteInClusters = storageService.listSessionsArchives(CLUSTERS_DUMP_NAME);
        List<String> actualArchivesAfterDeleteInStuff = storageService.listSessionsArchives(STUFF_DUMP_NAME);
        Collections.sort(expectedArchivesAfterDeleteInStuff);
        Collections.sort(actualArchivesAfterDeleteInStuff);

        assertEquals(expectedArchivesAfterDeleteInClusters, actualArchivesAfterDeleteInClusters);
        assertEquals(expectedArchivesAfterDeleteInStuff, actualArchivesAfterDeleteInStuff);
    }

    @Test
    public void noArchivesButDumpFolderExists() throws Exception {
        putEmptyDumpS3(CLUSTERS_DUMP_NAME);
        executor.doRealJob(null);
        assertTrue(storageService.listSessionsArchives(CLUSTERS_DUMP_NAME).isEmpty());
    }

    @Test
    public void checkIfDeleteSessionInBatches() throws Exception {
        //In this test objects are stored (and deleted) only in "clusters" dump storage
        Mockito.when(dumpStorageInfoService.getDumpNames()).thenReturn(Collections.singletonList(
            CLUSTERS_DUMP_NAME
        ));
        int numberOfObjects = BATCH_SIZE * 2 + 1;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime manyMonthsAgo = now.minusMonths(MONTHS_TO_DELETE + 2);
        for (int i = 0; i < numberOfObjects; i++) {
            String name = String.valueOf(i);
            putDumpArchiveS3(CLUSTERS_DUMP_NAME, manyMonthsAgo, name);
        }
        int numbOfBatches = (int) Math.ceil((double) numberOfObjects / BATCH_SIZE);

        s3StorageCoreService.deleteSession(ARCHIVE_PREFIX + "/" + CLUSTERS_DUMP_NAME, getArchiveName(manyMonthsAgo));
        // Checks if listObjects was called 1 time
        Mockito.verify(amazonS3, Mockito.times(1))
            .listObjects(Mockito.anyString(), Mockito.anyString());
        // Checks if listNextBatchOfObjects was called [numbOfBatches - 1] times
        Mockito.verify(amazonS3, Mockito.times(numbOfBatches - 1))
            .listNextBatchOfObjects(Mockito.any(ObjectListing.class));
        // Everything is deleted correctly
        assertTrue(storageService.listSessionsArchives(CLUSTERS_DUMP_NAME).isEmpty());
    }

    @Test
    public void leaveSessionsTestSimple() throws Exception {
        executor.setLeaveSessions(1);
        LocalDateTime now = LocalDateTime.now();
        // Should stay because leaveSessions == 1
        LocalDateTime manyMonthsAgo = now.minusMonths(MONTHS_TO_DELETE + 1);
        LocalDateTime evenMoreMonthsAgo = now.minusMonths(MONTHS_TO_DELETE + 2);
        LocalDateTime evenMoreMonthsAgo2 = now.minusMonths(MONTHS_TO_DELETE + 2 + 1);
        // Should stay because not old enough
        LocalDateTime someMonthsAgo = now.minusMonths(MONTHS_TO_DELETE - 1);
        putDumpArchiveS3(CLUSTERS_DUMP_NAME, someMonthsAgo);
        putDumpArchiveS3(CLUSTERS_DUMP_NAME, manyMonthsAgo);
        putDumpArchiveS3(CLUSTERS_DUMP_NAME, evenMoreMonthsAgo);
        putDumpArchiveS3(CLUSTERS_DUMP_NAME, evenMoreMonthsAgo2);

        executor.doRealJob(null);

        List<String> expectedArchivesAfterDelete = new ArrayList<>(Arrays.asList(getArchiveName(manyMonthsAgo),
            getArchiveName(someMonthsAgo))
        );
        Collections.sort(expectedArchivesAfterDelete);

        assertEquals(expectedArchivesAfterDelete, storageService.listSessionsArchives(CLUSTERS_DUMP_NAME));
    }

    @Test
    public void leaveSessionsTestWithBatches() throws Exception {
        executor.setLeaveSessions(1);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deleteSessionTime = now.minusMonths(MONTHS_TO_DELETE + 2);
        LocalDateTime staySessionTime = now.minusMonths(MONTHS_TO_DELETE + 1);
        // First half of the 1st batch contains of objects for deletion
        for (int i = 0; i < BATCH_SIZE / 2; i++) {
            String name = String.valueOf(i);
            putDumpArchiveS3(CLUSTERS_DUMP_NAME, deleteSessionTime, name);
        }
        // Second half of 1st batch contains of objects that need to stay
        // And also those objects are in the 2nd batch
        for (int i = BATCH_SIZE / 2 + 1; i < BATCH_SIZE + 2; i++) {
            String name = String.valueOf(i);
            putDumpArchiveS3(CLUSTERS_DUMP_NAME, staySessionTime, name);
        }
        executor.doRealJob(null);
        List<String> expectedArchivesAfterDelete = new ArrayList<>(Collections.singletonList(
            getArchiveName(staySessionTime)));
        assertEquals(expectedArchivesAfterDelete, storageService.listSessionsArchives(CLUSTERS_DUMP_NAME));
    }
}
