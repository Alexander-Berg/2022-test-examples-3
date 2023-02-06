package ru.yandex.direct.common.liveresource.zookeeper;

import org.apache.curator.RetryPolicy;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;

import ru.yandex.direct.libs.curator.CuratorFrameworkProvider;
import ru.yandex.direct.liveresource.LiveResourceEvent;
import ru.yandex.direct.liveresource.LiveResourceListener;
import ru.yandex.direct.liveresource.LiveResourceReadException;

import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class ZookeeperLiveResourceWatcherTest {

    private static final String CONTENT_INITIAL = "Initial content";
    private static final String CONTENT_FIRST_UPDATE = "First time updated content";
    private static final String CONTENT_SECOND_UPDATE = "Second time updated content";


    private static final String TEST_PATH = "/test";
    private static final String TEST_INVALID_PATH = "/invalid";

    private static TestingServer testingServer;

    private TaskScheduler taskScheduler;
    private LiveResourceListener listener;
    private ZookeeperLiveResource invalidLiveResource;
    private ZookeeperLiveResourceWatcher liveResourceWatcher;

    private CuratorFrameworkProvider curatorFrameworkProvider;

    @Before
    public void setUp() throws Exception {
        testingServer.start();

        RetryPolicy retryPolicy = new RetryNTimes(0, -1);
        curatorFrameworkProvider =
                new CuratorFrameworkProvider(testingServer.getConnectString(), "/lock-path", retryPolicy);
        curatorFrameworkProvider.getDefaultCurator().create().orSetData()
                .forPath(TEST_PATH, CONTENT_INITIAL.getBytes());

        taskScheduler = mock(TaskScheduler.class);

        ZookeeperLiveResource liveResource = new ZookeeperLiveResource(TEST_PATH, curatorFrameworkProvider);
        liveResourceWatcher = new ZookeeperLiveResourceWatcher(liveResource, curatorFrameworkProvider, taskScheduler);

        invalidLiveResource = new ZookeeperLiveResource(TEST_INVALID_PATH, curatorFrameworkProvider);

        listener = mock(LiveResourceListener.class);
    }

    @After
    public void tearDown() {
        curatorFrameworkProvider.closeDefault();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        testingServer = new TestingServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        testingServer.close();
    }

    @Test
    public void dontNotifyWhenLiveResourceNotChanged() {
        liveResourceWatcher.addListener(listener);
        liveResourceWatcher.watch();
        verify(listener, never()).update(any());
    }

    @Test
    public void notifyListenerWhenLiveResourceChangedFirstTime() throws Exception {
        liveResourceWatcher.addListener(listener);
        liveResourceWatcher.watch();

        curatorFrameworkProvider.getDefaultCurator().setData().forPath(TEST_PATH, CONTENT_FIRST_UPDATE.getBytes());

        var eventCaptor = ArgumentCaptor.forClass(LiveResourceEvent.class);
        verify(listener, timeout(500)).update(eventCaptor.capture());
        assertThat(eventCaptor.getValue(), beanDiffer(new LiveResourceEvent(CONTENT_FIRST_UPDATE)));
    }

    @Test
    public void notifyListenerWhenLiveResourceChangedTwoTimes() throws Exception {
        liveResourceWatcher.addListener(listener);
        liveResourceWatcher.watch();

        curatorFrameworkProvider.getDefaultCurator().setData().forPath(TEST_PATH, CONTENT_FIRST_UPDATE.getBytes());

        var eventCaptor = ArgumentCaptor.forClass(LiveResourceEvent.class);
        verify(listener, timeout(500)).update(eventCaptor.capture());
        assertThat(eventCaptor.getValue(), beanDiffer(new LiveResourceEvent(CONTENT_FIRST_UPDATE)));

        curatorFrameworkProvider.getDefaultCurator().setData().forPath(TEST_PATH, CONTENT_SECOND_UPDATE.getBytes());

        verify(listener, timeout(500).times(2)).update(eventCaptor.capture());
        assertThat(eventCaptor.getValue(), beanDiffer(new LiveResourceEvent(CONTENT_SECOND_UPDATE)));
    }

    @Test
    public void dontNotifyIfClosed() throws Exception {
        liveResourceWatcher.addListener(listener);
        liveResourceWatcher.watch();

        liveResourceWatcher.close();

        curatorFrameworkProvider.getDefaultCurator().setData().forPath(TEST_PATH, CONTENT_FIRST_UPDATE.getBytes());

        verify(listener, after(100).never()).update(any());
    }

    @Test(expected = LiveResourceReadException.class)
    public void failsWithInvalidPath() {
        ZookeeperLiveResourceWatcher watcher =
                new ZookeeperLiveResourceWatcher(invalidLiveResource, curatorFrameworkProvider, taskScheduler);
        watcher.watch();
    }
}
