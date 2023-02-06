package ru.yandex.direct.common.liveresource.zookeeper;

import org.apache.curator.RetryPolicy;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.test.TestingServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.direct.libs.curator.CuratorFrameworkProvider;
import ru.yandex.direct.liveresource.LiveResourceReadException;

import static org.assertj.core.api.Assertions.assertThat;

public class ZookeeperLiveResourceTest {

    public static final String TEST_DATA = "test-data";
    public static final String TEST_PATH = "/test";
    public static final String TEST_INVALID_PATH = "/invalid";

    private static TestingServer testingServer;
    private static CuratorFrameworkProvider curatorFrameworkProvider;

    @BeforeClass
    public static void beforeClass() throws Exception {
        RetryPolicy retryPolicy = new RetryNTimes(0, -1);
        testingServer = new TestingServer();
        curatorFrameworkProvider =
                new CuratorFrameworkProvider(testingServer.getConnectString(), "/lock-path", retryPolicy);
        curatorFrameworkProvider.getDefaultCurator().create().forPath(TEST_PATH, TEST_DATA.getBytes());
    }

    @AfterClass
    public static void afterClass() throws Exception {
        testingServer.close();
        curatorFrameworkProvider.closeDefault();
    }

    @Test
    public void get() {
        assertThat(new ZookeeperLiveResource(TEST_PATH, curatorFrameworkProvider).getContent()).isEqualTo(TEST_DATA);
    }

    @Test
    public void get_CanHandleRestart() throws Exception {
        testingServer.restart();
        assertThat(new ZookeeperLiveResource(TEST_PATH, curatorFrameworkProvider).getContent()).isEqualTo(TEST_DATA);
    }

    @Test(expected = LiveResourceReadException.class)
    public void get_InvalidFails() {
        new ZookeeperLiveResource(TEST_INVALID_PATH, curatorFrameworkProvider).getContent();
    }
}
