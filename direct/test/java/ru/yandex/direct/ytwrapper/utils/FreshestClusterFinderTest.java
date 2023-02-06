package ru.yandex.direct.ytwrapper.utils;

import java.time.Duration;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.direct.utils.io.FileUtils;
import ru.yandex.direct.ytwrapper.client.YtClusterConfig;
import ru.yandex.direct.ytwrapper.client.YtClusterTypesafeConfigProvider;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtOperator;
import ru.yandex.direct.ytwrapper.model.YtTable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore("Ходит в yt, для ручного запуска")
public class FreshestClusterFinderTest {
    private static final String TEST_TABLE_PATH = "//home/direct-dev/tmp/tmp/testTable";
    private YtProvider ytProvider;
    private YtTable ytTable;

    @Before
    public void before() {
        YtClusterTypesafeConfigProvider configProvider = mock(YtClusterTypesafeConfigProvider.class);
        YtClusterConfig ytClusterConfig = mock(YtClusterConfig.class);
        when(configProvider.get(YtCluster.ARNOLD)).thenReturn(ytClusterConfig);
        when(ytClusterConfig.getToken()).thenReturn(FileUtils.slurp(FileUtils.expandHome("~/.yt/token")).trim());
        when(ytClusterConfig.getYqlToken()).thenReturn(FileUtils.slurp(FileUtils.expandHome("~/.yt/yql_token")).trim());
        when(ytClusterConfig.getProxy()).thenReturn("arnold.yt.yandex.net");

        ytProvider = new YtProvider(configProvider, null, null);
        ytTable = new YtTable(TEST_TABLE_PATH);

        createTable(TEST_TABLE_PATH);
    }

    @After
    public void after() {
        dropTable(TEST_TABLE_PATH);
    }

    @Test
    public void testYtOperatorTimeout_successfull() {
        int simpleCommandsRetries = 1;

        String errorMessage = "";
        YtOperator ytOperator = ytProvider.getOperator(YtCluster.ARNOLD,
                Duration.ofMillis(1000), Duration.ofMillis(1000), simpleCommandsRetries);
        try {
            String modificationTime = ytOperator.readTableModificationTime(ytTable);
        } catch (RuntimeException ex) {
            errorMessage = ex.getMessage();
        }

        assertEquals("Чтение должно пройти успешно", "", errorMessage);
    }

    @Test
    public void testYtOperatorTimeout_socketTimeoutFailed() {
        int simpleCommandsRetries = 1;

        String errorMessage = "";
        YtOperator ytOperator = ytProvider.getOperator(YtCluster.ARNOLD,
                Duration.ofMillis(1), Duration.ofMillis(1000), simpleCommandsRetries);
        try {
            String modificationTime = ytOperator.readTableModificationTime(ytTable);
        } catch (RuntimeException ex) {
            try {
                errorMessage = ex.getCause().getMessage();
            } catch (NullPointerException e) { }
        }

        assertEquals("Чтение не должно пройти по socket таймауту", "Read timed out", errorMessage);
    }

    @Test
    public void testYtOperatorTimeout_connectTimeoutFailed() {
        int simpleCommandsRetries = 1;

        String errorMessage = "";
        YtOperator ytOperator = ytProvider.getOperator(YtCluster.ARNOLD,
                Duration.ofMillis(1000), Duration.ofMillis(1), simpleCommandsRetries);
        try {
            String modificationTime = ytOperator.readTableModificationTime(ytTable);
        } catch (RuntimeException ex) {
            try {
                errorMessage = ex.getCause().getCause().getMessage();
            } catch (NullPointerException e) { }
        }

        assertEquals("Чтение не должно пройти по connect таймауту", "connect timed out", errorMessage);
    }

    private void createTable(String path) {
        YtOperator ytOperator = ytProvider.getOperator(YtCluster.ARNOLD);
        String insertQuery = "insert into `" + path + "` (select \"some value\" as field)";
        ytOperator.yqlExecute(insertQuery);
    }

    private void dropTable(String path) {
        YtOperator ytOperator = ytProvider.getOperator(YtCluster.ARNOLD);
        String dropQuery = "drop table `" + path + "`";
        ytOperator.yqlExecute(dropQuery);
    }
}
