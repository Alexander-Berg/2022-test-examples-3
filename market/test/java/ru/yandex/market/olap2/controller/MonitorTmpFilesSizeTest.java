package ru.yandex.market.olap2.controller;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.yandex.market.olap2.load.tasks.VerticaLoadTask;
import ru.yandex.market.olap2.load.YtLoader;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.olap2.TestUtils.setFinalStatic;

public class MonitorTmpFilesSizeTest {
    private static String CONTENT = "MonitorTmpFilesSizeTestContent";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void init() throws IOException {
        Files.write(folder.newFile(VerticaLoadTask.TMP_PREFFIX + "1.tmp").toPath(), CONTENT.getBytes());
        Files.write(folder.newFile(VerticaLoadTask.TMP_PREFFIX + "2.tmp").toPath(), CONTENT.getBytes());
        Files.write(folder.newFile(VerticaLoadTask.TMP_PREFFIX + "3.tmp").toPath(), CONTENT.getBytes());
    }

    @Test
    public void testGetErrorOk() throws Exception {
        assertThat(MonitorTmpFilesSize.getError(), is(JugglerConstants.OK));
    }

    // to run next two @Ignored tests change MonitorTmpFilesSize
    // MAX_FILE_COUNT & TMP_DIR constants to public boxed
    // primitives Long and Integer
    @Test
    @Ignore
    public void testGetErrorCount() throws Exception {
        setFinalStatic(
            MonitorTmpFilesSize.class.getDeclaredField("MAX_FILE_COUNT"),
            2
        );
        setFinalStatic(
            YtLoader.class.getDeclaredField("TMP_DIR"),
            folder.getRoot().getAbsolutePath()
        );
        assertTrue(MonitorTmpFilesSize.getError().startsWith(JugglerConstants.CRIT));
    }

    @Test
    @Ignore
    public void testGetErrorSize() throws Exception {
        setFinalStatic(
            MonitorTmpFilesSize.class.getDeclaredField("MAX_DIR_SIZE_BYTES"),
            32L
        );
        setFinalStatic(
            YtLoader.class.getDeclaredField("TMP_DIR"),
            folder.getRoot().getAbsolutePath()
        );
        assertTrue(MonitorTmpFilesSize.getError().startsWith(JugglerConstants.CRIT));
    }

    @Test
    public void testGlobSize() {
        MonitorTmpFilesSize.SizeAndCount result =
            MonitorTmpFilesSize.globSize(folder.getRoot().getAbsolutePath() + "/" + VerticaLoadTask.TMP_PREFFIX + "*");
        assertThat(result, is(new MonitorTmpFilesSize.SizeAndCount(CONTENT.length() * 3, 3)));
    }
}
