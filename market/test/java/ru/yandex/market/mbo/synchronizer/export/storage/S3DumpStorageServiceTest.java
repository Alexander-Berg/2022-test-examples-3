package ru.yandex.market.mbo.synchronizer.export.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.zip.GZIPInputStream;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.io.CharStreams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.mbo.common.s3.AmazonS3ClientFactory;
import ru.yandex.market.mbo.export.dumpstorage.DumpStorageInfoServiceImpl;
import ru.yandex.market.mbo.export.dumpstorage.YtSessionService;
import ru.yandex.market.mbo.export.dumpstorage.ZkDumpStorageInfoService;
import ru.yandex.market.mbo.s3.AmazonS3Mock;
import ru.yandex.market.mbo.synchronizer.export.ExporterUtils;
import ru.yandex.market.mbo.synchronizer.export.MD5SUMS;
import ru.yandex.market.mbo.utils.ZooKeeperServiceMock;
import ru.yandex.market.mbo.yt.TestYt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тесты проверяют работу DumpStorageService который использует S3DumpStorageService и ZkDumpStorageInfoService.
 * У S3DumpStorageService замокан AmazonS3 через AmazonS3Mock.
 * У ZkDumpStorageInfoService замокан ZooKeeperService через ZooKeeperServiceMock.
 *
 * @author ayratgdl
 * @date 14.05.17
 */
public class S3DumpStorageServiceTest {
    private static final String BUCKET_NAME = "mbo-dump";
    private static final String DUMP_NAME = "stuff";
    private static final String SESSION_ID = "20170101_0200";

    private DumpStorageService dumpStorageService;
    private AmazonS3 s3Client;

    private SessionFolderMaker sessionCreator;
    private Path downloadsDir;

    private YtSessionService ytSessionService;
    private ReplicationService replicationService;
    private Yt yt;
    private String ytPath = "//" + DUMP_NAME;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        // build S3DumpStorageService
        s3Client = new AmazonS3Mock();
        AmazonS3ClientFactory s3ClientFactory = mock(AmazonS3ClientFactory.class);
        when(s3ClientFactory.getS3Client()).thenReturn(s3Client);

        S3DumpStorageCoreService dumpStorageCoreService = new S3DumpStorageCoreService();
        dumpStorageCoreService.setS3ClientFactory(s3ClientFactory);
        dumpStorageCoreService.setBucketName(BUCKET_NAME);

        // build ZkDumpStorageInfoService
        ZkDumpStorageInfoService dumpStorageInfoService = new ZkDumpStorageInfoService();
        dumpStorageInfoService.setZooKeeperService(new ZooKeeperServiceMock());

        yt = new TestYt();

        ytSessionService = new YtSessionService();
        ytSessionService.setYtRootPath(ytPath);
        ytSessionService.setYt(yt);

        replicationService = Mockito.mock(ReplicationService.class);

        DumpStorageInfoServiceImpl dumpStorageInfoServiceImpl = new DumpStorageInfoServiceImpl(
            dumpStorageInfoService,
            ytSessionService,
            null
        );
        dumpStorageService = new DumpStorageService(dumpStorageCoreService, dumpStorageInfoServiceImpl,
            ytSessionService, replicationService, folder.newFolder("dump-tmp-folder").toPath());

        sessionCreator = new SessionFolderMaker(folder.newFolder("mbo-test-uploads"));
        downloadsDir = folder.newFolder("mbo-test-downloads").toPath();

    }

    /**
     * 1. Заливаем выгрузку в хранилище
     * 2. Скачиваем выгрузку из хранилища
     * 3. Удаляем выгрузку из хранилища
     */
    @Test
    public void putGetDeleteTest() throws IOException {
        Path localFolder = sessionCreator.createSessionFolder(SESSION_ID);
        assertFalse(dumpStorageService.isExistSession(DUMP_NAME, SESSION_ID));

        YPath sessionPath = YPath.simple(ytPath).child(SESSION_ID);
        YPath recentPath = YPath.simple(ytPath).child("recent");

        yt.cypress().create(sessionPath, CypressNodeType.MAP);
        Assert.assertEquals(true, yt.cypress().exists(sessionPath));
        Assert.assertEquals(false, yt.cypress().exists(recentPath));

        dumpStorageService.saveSession(DUMP_NAME, SESSION_ID, localFolder, false, Collections.emptySet());

        assertTrue(dumpStorageService.isExistSession(DUMP_NAME, SESSION_ID));
        Assert.assertEquals(true, yt.cypress().exists(recentPath));

        dumpStorageService.getExtractorsGroup(
            DUMP_NAME, SESSION_ID, "folder1", downloadsDir.toString());
        assertEquals(
            SessionFolderMaker.MD5SUMS_CONTENT,
            readDumpFile(downloadsDir.resolve(SESSION_ID).resolve(MD5SUMS.MD5SUMS_FILE_NAME))
        );
        assertEquals(
            SessionFolderMaker.FILE_CONTENT,
            readDumpFile(downloadsDir.resolve(SESSION_ID).resolve(SessionFolderMaker.FILE_NAME))
        );

        dumpStorageService.deleteSession(DUMP_NAME, SESSION_ID);
        assertFalse(dumpStorageService.isExistSession(DUMP_NAME, SESSION_ID));
    }

    /**
     * Проверяем что каждый файл выгрузки при сохранении в s3 хранится следующим образом:
     * 1. пусть:
     *      название выгрузки test-dump,
     *      id сессии 20170101_0200,
     *      путь до сохроняемого файла внутри выгрузки folder1/folder2/file.txt,
     *    тогда в s3 он будет хранится по ключу test-dump/20170101_0200/folder1/folder2/file.txt.gz
     * 2. внутри s3 файлы выгрузки хранятся сжатыми в gzip
     */
    @Test
    public void howDoesS3StoreFilesTest() throws IOException {
        yt.cypress().create(YPath.simple(ytPath + "/" + SESSION_ID), CypressNodeType.MAP);
        Path sessionFolder = sessionCreator.createSessionFolder(SESSION_ID);
        dumpStorageService.saveSession(DUMP_NAME, SESSION_ID, sessionFolder, false, Collections.emptySet());

        S3Object md5SumsObject = s3Client.getObject(BUCKET_NAME, buildKey(MD5SUMS.MD5SUMS_FILE_NAME));
        assertEquals(SessionFolderMaker.MD5SUMS_CONTENT, getContentFromGzip(md5SumsObject.getObjectContent()));
    }

    /**
     * Проверяем что сохраняем только файлы перечисленные в MD5SUMS и сам MD5SUMS.
     */
    @Test
    public void saveOnlyFilesFromMD5SUMSTest() throws IOException {
        yt.cypress().create(YPath.simple(ytPath + "/" + SESSION_ID), CypressNodeType.MAP);
        Path sessionFolder = sessionCreator.createSessionFolder(SESSION_ID);
        FileUtils.writeStringToFile(sessionFolder.resolve("unwanted-file").toFile(),
            "unwanted", StandardCharsets.UTF_8);

        dumpStorageService.saveSession(DUMP_NAME, SESSION_ID, sessionFolder, false, Collections.emptySet());

        Assert.assertTrue(s3Client.doesObjectExist(BUCKET_NAME, buildKey(MD5SUMS.MD5SUMS_FILE_NAME)));
        Assert.assertTrue(s3Client.doesObjectExist(BUCKET_NAME, buildKey(SessionFolderMaker.FILE_NAME)));
        Assert.assertFalse(s3Client.doesObjectExist(BUCKET_NAME, buildKey("unwanted-file")));
    }

    private String getContentFromGzip(InputStream gzipInput) throws IOException {
        try (InputStream unzipInput = new GZIPInputStream(gzipInput)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            IOUtils.copy(unzipInput, output);
            return new String(output.toByteArray());
        }
    }

    private String buildKey(String fileName) {
        return DUMP_NAME + "/" + SESSION_ID + "/" + fileName.replace('\\', '/') + ".gz";
    }

    private static String readDumpFile(Path file) throws IOException {
        try (Reader reader = ExporterUtils.getReader(file.toFile())) {
            return CharStreams.toString(reader);
        }
    }
}
