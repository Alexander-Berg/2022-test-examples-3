package ru.yandex.market.mbo.synchronizer.export.storage;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.common.s3.AmazonS3ClientFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

/**
 * @author ayratgdl
 * @date 16.05.17
 */
public class S3DumpStorageCoreServiceTest {
    private static final String DUMP_NAME = "dumpName";
    private static final String SESSION_ID = "20170516_0000";
    private S3DumpStorageCoreService badDumpStorageCodeService;
    private Path tmpDir;

    @Before
    public void setUp() throws IOException {
        AmazonS3ClientFactory nullS3ClientFactory = Mockito.mock(AmazonS3ClientFactory.class);
        Mockito.when(nullS3ClientFactory.getS3Client()).thenReturn(null);

        badDumpStorageCodeService = new S3DumpStorageCoreService();
        badDumpStorageCodeService.setBucketName("bucketName");
        badDumpStorageCodeService.setS3ClientFactory(nullS3ClientFactory);

        tmpDir = Files.createTempDirectory("mbo-test");
        FileUtils.writeStringToFile(tmpDir.resolve("test-file").toFile(), "test data", StandardCharsets.UTF_8);
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(tmpDir.toFile());
    }

    /**
     * Регресионный тест.Проверяем что exception сгенерированный внутри потоков заливающих файлы
     * пробрасывается в основной поток.
     */
    @Test(expected = Exception.class)
    public void throwExceptionOnSaveSessionWhenS3ClientIsBadTest() {
        badDumpStorageCodeService.saveSession(DUMP_NAME, SESSION_ID, tmpDir, false, Collections.emptySet());
    }
}
