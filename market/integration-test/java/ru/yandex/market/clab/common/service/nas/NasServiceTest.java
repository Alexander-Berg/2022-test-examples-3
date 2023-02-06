package ru.yandex.market.clab.common.service.nas;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.market.clab.common.config.nas.NasConfig;
import ru.yandex.market.clab.test.BaseIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertArrayEquals;

/**
 * @author anmalysh
 * @since 11/12/2018
 */
@SpringBootTest(classes = {
    NasConfig.class
})
@Ignore("https://st.yandex-team.ru/MBO-23335#5e11f3ee2b10cd7237f8a26c")
public class NasServiceTest extends BaseIntegrationTest {

    private static final String TEST_FILE_RESOURCE = "/nas/test.jpg";
    private static final String TEST_FILE = "file.jpg";
    private static final String TEST_FILE_TO = "tofile.jpg";
    private static final String DIR_NAME = "directory";
    private static final String TO_DIR_NAME = "todirectory";
    private static final String RESTRICTED = "processed";

    @Autowired
    private NasServiceImpl nasService;

    private String rootDir;

    @Before
    public void setUp() {
        rootDir = getRootDir();
        if (!rootDir.equals(RESTRICTED)) {
            nasService.createDir(rootDir);
        }
        List<String> restrictedContents = nasService.list(RESTRICTED);
        if (restrictedContents.contains(DIR_NAME)) {
            nasService.delete(nasService.getRelativePath(RESTRICTED, DIR_NAME));
        }
    }

    @After
    public void tearDown() {
        if (!rootDir.equals(RESTRICTED)) {
            nasService.delete(rootDir);
        }
    }

    @Test
    public void testDirectoryOperations() {
        String dirPath = nasService.getRelativePath(rootDir, DIR_NAME);
        String toDirPath = nasService.getRelativePath(rootDir, TO_DIR_NAME);
        // Create
        nasService.createDir(dirPath);
        List<String> rootContents = nasService.list(rootDir);
        assertThat(rootContents).containsExactly(DIR_NAME);

        // Move
        nasService.move(dirPath, toDirPath);
        rootContents = nasService.list(rootDir);
        assertThat(rootContents).containsExactly(TO_DIR_NAME);

        // Delete
        nasService.delete(toDirPath);
        rootContents = nasService.list(rootDir);
        assertThat(rootContents).isEmpty();
    }

    @Test
    public void testFileOperations() throws IOException {
        String dirPath = nasService.getRelativePath(rootDir, DIR_NAME);
        // Create
        nasService.createDir(dirPath);
        InputStream fileStream = getClass().getResourceAsStream(TEST_FILE_RESOURCE);
        String filePath = nasService.getRelativePath(dirPath, TEST_FILE);
        nasService.createFile(filePath, fileStream);
        List<String> dirContents = nasService.list(dirPath);
        assertThat(dirContents).containsExactly(TEST_FILE);

        // Move
        String toFilePath = nasService.getRelativePath(dirPath, TEST_FILE_TO);
        nasService.move(filePath, toFilePath);
        dirContents = nasService.list(dirPath);
        assertThat(dirContents).containsExactly(TEST_FILE_TO);

        // Read
        fileStream = getClass().getResourceAsStream(TEST_FILE_RESOURCE);
        InputStream nasFileStream = nasService.read(toFilePath);
        byte[] expected = IOUtils.toByteArray(fileStream);
        byte[] actual = IOUtils.toByteArray(nasFileStream);
        assertArrayEquals(expected, actual);

        // Delete
        nasService.delete(toFilePath);
        dirContents = nasService.list(dirPath);
        assertThat(dirContents).isEmpty();
        nasService.delete(dirPath);
    }

    @Test
    public void testRestrictedPath() {
        String restrictedPath = nasService.getRelativePath(RESTRICTED, DIR_NAME);
        assertThatThrownBy(() -> {
            nasService.createDir(restrictedPath);
        }).isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to create directory " + restrictedPath);

        List<String> restrictedContents = nasService.list(RESTRICTED);
        assertThat(restrictedContents).doesNotContain(DIR_NAME);
    }

    @Test
    public void testMoveMissingDir() {
        String dirPath = nasService.getRelativePath(rootDir, DIR_NAME);
        String toDirPath = nasService.getRelativePath(rootDir, TO_DIR_NAME);
        // Create
        nasService.createDir(dirPath);
        List<String> rootContents = nasService.list(rootDir);
        assertThat(rootContents).containsExactly(DIR_NAME);

        // Move
        nasService.move(dirPath, toDirPath);
        rootContents = nasService.list(rootDir);
        assertThat(rootContents).containsExactly(TO_DIR_NAME);

        // Move again
        assertThatThrownBy(() -> {
            nasService.move(dirPath, toDirPath);
        }).isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to move " + dirPath + " to " + toDirPath);

        rootContents = nasService.list(rootDir);
        assertThat(rootContents).containsExactly(TO_DIR_NAME);

        // Delete
        nasService.delete(toDirPath);
        rootContents = nasService.list(rootDir);
        assertThat(rootContents).isEmpty();
    }

    @Test
    public void testSpaces() throws IOException {
        String dirPath = nasService.getRelativePath(rootDir, "some dir");
        String toDirPath = nasService.getRelativePath(rootDir, "other dir");
        // Create
        nasService.createDir(dirPath);
        List<String> rootContents = nasService.list(rootDir);
        assertThat(rootContents).containsExactly("some dir");

        // Move
        nasService.move(dirPath, toDirPath);
        rootContents = nasService.list(rootDir);
        assertThat(rootContents).containsExactly("other dir");

        // Create file
        InputStream fileStream = getClass().getResourceAsStream(TEST_FILE_RESOURCE);
        String filePath = nasService.getRelativePath(toDirPath, "some  file.jpg");
        nasService.createFile(filePath, fileStream);
        List<String> dirContents = nasService.list(toDirPath);
        assertThat(dirContents).containsExactly("some  file.jpg");

        // Read file
        byte[] initialFile = IOUtils.toByteArray(getClass().getResourceAsStream(TEST_FILE_RESOURCE));
        byte[] storedFile = IOUtils.toByteArray(nasService.read(filePath));
        assertThat(storedFile).containsExactly(initialFile);

        // Delete
        nasService.delete(toDirPath);
        rootContents = nasService.list(rootDir);
        assertThat(rootContents).isEmpty();
    }

    public String getRootDir() {
        return "" + Math.abs(new Random().nextInt());
    }
}
