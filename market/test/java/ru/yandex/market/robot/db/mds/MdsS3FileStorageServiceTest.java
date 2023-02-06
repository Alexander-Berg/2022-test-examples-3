package ru.yandex.market.robot.db.mds;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

/**
 * Перед запуском нужно указать ir.mds.s3.access.key.id и ir.mds.s3.secret.key.id в test.properties.
 *
 * @author Shamil Ablyazov, <a href="mailto:a-shar@yandex-team.ru"/>.
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:storage-test.xml"})
public class MdsS3FileStorageServiceTest {

    private static final String TEST_FILE_NAME = "test.file";
    private static final String GZIPED_FILE_NAME = "test.file.gz";
    private static final String EXISTS_FILE_NAME = "exists_test.file";
    private static final String BUCKET_NAME = "market-ir-dev";

    @Autowired
    private MdsS3FileStorageService mdsS3FileStorageService;

    @Before
    public void init() throws IOException {
        mdsS3FileStorageService.setBucketName(BUCKET_NAME);
        saveTestFile(EXISTS_FILE_NAME);
    }

    @Test
    public void saveInputStream() throws Exception {
        saveTestFile(TEST_FILE_NAME);
        Assert.assertTrue(mdsS3FileStorageService.fileExists(TEST_FILE_NAME));
        InputStream inputStream = new ByteArrayInputStream(String.join("", Collections.nCopies(1, "hello")).getBytes());
        mdsS3FileStorageService.saveInputStream(inputStream, GZIPED_FILE_NAME, true);
        deleteFileQuiet(GZIPED_FILE_NAME);
    }

    private void saveTestFile(String fileName) throws IOException {
        InputStream inputStream = new ByteArrayInputStream("hello".getBytes());
        mdsS3FileStorageService.saveInputStream(inputStream, fileName);
    }

    @Test
    public void fileExists() {
        Assert.assertTrue(mdsS3FileStorageService.fileExists(EXISTS_FILE_NAME));
        Assert.assertFalse(mdsS3FileStorageService.fileExists(EXISTS_FILE_NAME + ".xxx"));
    }

    @Test
    public void deleteFile() throws Exception {
        saveTestFile(TEST_FILE_NAME);
        mdsS3FileStorageService.deleteFile(TEST_FILE_NAME);
        Assert.assertFalse(mdsS3FileStorageService.fileExists(TEST_FILE_NAME));
    }

    @After
    public void finalize() {
        deleteFileQuiet(TEST_FILE_NAME);
        deleteFileQuiet(EXISTS_FILE_NAME);
    }

    private void deleteFileQuiet(String fileName) {
        try {
            mdsS3FileStorageService.deleteFile(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
