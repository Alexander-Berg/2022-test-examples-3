package ru.yandex.market.logshatter.config.ddl;

import junit.framework.Assert;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.test.TestingServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.clickhouse.ddl.DDL;
import ru.yandex.market.clickhouse.ddl.DdlQuery;
import ru.yandex.market.clickhouse.ddl.DdlQueryType;
import ru.yandex.market.monitoring.MonitoringStatus;
import ru.yandex.market.monitoring.MonitoringUnit;

import java.net.BindException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 02.11.16
 */
public class UpdateDDLWorkerTest {
    private static final Logger log = LogManager.getLogger();
    private static final String MANUAL_QUERY = "ALTER TABLE market.market";

    private static TestingServer server;
    private String namespace;

    @BeforeClass
    public static void onlyOnce() throws Exception {
       server = createServer();
    }

    @Before
    public void setUp() throws Exception {
        namespace = UUID.randomUUID().toString();
    }

    @Test
    public void testDDLServiceSuccess() throws Exception {
        CuratorFramework client = createClient();
        UpdateDDLService ddlService = new FakeUpdateDDLService(new UpdateDDLTaskExecutorResult.Success());
        UpdateDDLWorker sut = createSut(client, ddlService);
        sut.run();

        UpdateDDLStatus status = sut.awaitStatus();
        Assert.assertEquals(UpdateDDLStatus.SUCCESS, status);
    }

    @Test
    public void testDDLServiceManualDDLRequired() throws Exception {
        CuratorFramework client = createClient();

        UpdateDDLService ddlService = new FakeUpdateDDLService(
            new UpdateDDLTaskExecutorResult.ManualDDLRequired(
                Arrays.asList(createDDL(DdlQueryType.MODIFY_COLUMN))
            )
        );

        UpdateDDLWorker sut = createSut(client, ddlService);
        sut.run();

        UpdateDDLStatus status = sut.awaitStatus();
        Assert.assertEquals(UpdateDDLStatus.MANUAL_DDL_REQUIRED, status);
    }

    @Test
    public void testDDLServiceColumnDropsRequired() throws Exception {
        CuratorFramework client = createClient();

        UpdateDDLService ddlService = new FakeUpdateDDLService(
            new UpdateDDLTaskExecutorResult.ManualDDLRequired(
                Collections.singletonList(createDDL(DdlQueryType.DROP_COLUMN))
            )
        );

        UpdateDDLWorker sut = createSut(client, ddlService);
        sut.run();

        UpdateDDLStatus status = sut.awaitStatus();
        Assert.assertEquals(UpdateDDLStatus.COLUMN_DROPS_REQUIRED, status);
        Assert.assertEquals(1, sut.getManualDDLs().size());
        DdlQuery manualDdlQuery = sut.getManualDDLs().get(0).getManualUpdates().get(0);
        Assert.assertEquals(MANUAL_QUERY, manualDdlQuery.getQueryString());
        Assert.assertEquals(DdlQueryType.DROP_COLUMN, manualDdlQuery.getType());
    }

    @Test
    public void testDDLServiceManualPartialSuccess() throws Exception {
        CuratorFramework client = createClient();
        UpdateDDLService ddlService = new FakeUpdateDDLService(new UpdateDDLTaskExecutorResult.PartialSuccess());
        UpdateDDLWorker sut = createSut(client, ddlService);
        sut.run();

        UpdateDDLStatus status = sut.awaitStatus();
        Assert.assertEquals(UpdateDDLStatus.PARTIAL_SUCCESS, status);
    }

    @Test(expected = ExecutionException.class)
    public void testDDLServiceFail() throws Exception {
        CuratorFramework client = createClient();
        UpdateDDLService ddlService = new FakeUpdateDDLService() {
            @Override
            protected void run() throws InterruptedException {
                throw new IllegalStateException();
            }
        };

        UpdateDDLWorker sut = createSut(client, ddlService);
        sut.run();

        sut.awaitStatus();
    }

    public void partailSuccessThenSuccess() throws Exception {
        FakeUpdateDDLService ddlService = new FakeUpdateDDLService() {
            @Override
            protected void run() throws InterruptedException {
                Thread.sleep(10);
                notifyWatchers(new UpdateDDLTaskExecutorResult.PartialSuccess());

                Thread.sleep(10);
                notifyWatchers(new UpdateDDLTaskExecutorResult.Success());
            }
        };

        CuratorFramework client = createClient();
        UpdateDDLWorker sut = createSut(client, ddlService);
        sut.run();

        UpdateDDLStatus status = sut.awaitStatus();
        Assert.assertEquals(UpdateDDLStatus.PARTIAL_SUCCESS, status);

        // ждём пока завершится, иначе упадёт по таймауту
        waitUntilFinished(sut);
    }

    @Test(timeout = 5_000)
    public void partailSuccessThenManualDDLRequired() throws Exception {
        FakeUpdateDDLService ddlService = new FakeUpdateDDLService() {
            @Override
            protected void run() throws InterruptedException {
                Thread.sleep(10);
                notifyWatchers(new UpdateDDLTaskExecutorResult.PartialSuccess());

                Thread.sleep(10);
                notifyWatchers(new UpdateDDLTaskExecutorResult.ManualDDLRequired(new ArrayList<>()));
            }
        };

        CuratorFramework client = createClient();
        MonitoringUnit monitoringUnit = new MonitoringUnit("DDL");

        UpdateDDLWorker sut = createSut(client, ddlService, monitoringUnit);
        sut.run();

        UpdateDDLStatus status = sut.awaitStatus();
        Assert.assertEquals(UpdateDDLStatus.PARTIAL_SUCCESS, status);

        // wait unit critical
        while (!monitoringUnit.getStatus().equals(MonitoringStatus.CRITICAL)) {
        }
        Assert.assertFalse(sut.isFinished());
    }

    @Test
    public void partialSuccessThenManualDDLRequiredThenSuccess() throws Exception {
        FakeUpdateDDLService ddlService = new FakeUpdateDDLService() {
            @Override
            protected void run() throws InterruptedException {
                Thread.sleep(10);
                notifyWatchers(new UpdateDDLTaskExecutorResult.PartialSuccess());

                Thread.sleep(10);
                notifyWatchers(new UpdateDDLTaskExecutorResult.ManualDDLRequired(new ArrayList<>()));

                Thread.sleep(10);
                // кто-то накатил DDL руками
                notifyWatchers(new UpdateDDLTaskExecutorResult.Success());
            }
        };

        MonitoringUnit monitoringUnit = mock(MonitoringUnit.class);
        CuratorFramework client = createClient();
        UpdateDDLWorker sut = createSut(client, ddlService, monitoringUnit);
        sut.run();

        UpdateDDLStatus status = sut.awaitStatus();
        Assert.assertEquals(UpdateDDLStatus.PARTIAL_SUCCESS, status);

        // должен дождаться завершения

        waitUntilFinished(sut);

        verify(monitoringUnit).warning(anyString());
        verify(monitoringUnit).critical(anyString());
        verify(monitoringUnit).ok();
    }

    @Test(timeout = 5_000)
    public void twoNodesSuccess() throws Exception {
        AtomicInteger ddlServiceCallTimes = new AtomicInteger(0);

        FakeUpdateDDLService ddlService = new FakeUpdateDDLService() {
            @Override
            protected void run() throws InterruptedException {
                notifyWatchers(new UpdateDDLTaskExecutorResult.Success());
                ddlServiceCallTimes.getAndIncrement();
            }
        };

        Thread firstNode = new Thread(() -> {
            CuratorFramework client = createClient();
            UpdateDDLWorker sut = createSut(client, ddlService);
            try {
                sut.run();
                sut.awaitStatus();
            } catch (Exception e) {
                Assert.fail(e.toString());
            }
        });
        firstNode.start();

        CuratorFramework client = createClient();
        UpdateDDLWorker sut = createSut(client, ddlService);
        sut.run();
        UpdateDDLStatus status = sut.awaitStatus();

        Assert.assertEquals(UpdateDDLStatus.SUCCESS, status);
        waitUntilFinished(sut);

        firstNode.join();

        Assert.assertEquals(1, ddlServiceCallTimes.get());
    }

    @Test
    public void twoNodesFailure() throws Exception {
        FakeUpdateDDLService ddlService = new FakeUpdateDDLService() {
            @Override
            protected void run() throws InterruptedException {
                throw new IllegalStateException();
            }
        };

        Thread firstNode = new Thread(() -> {
            CuratorFramework client = createClient();
            UpdateDDLWorker sut = createSut(client, ddlService);
            try {
                sut.run();
                sut.awaitStatus();
            } catch (Exception e) {
                assertThat(e, instanceOf(ExecutionException.class));
                return;
            }

            Assert.fail("Exception must be thrown");
        });
        firstNode.start();

        try {
            CuratorFramework client = createClient();
            UpdateDDLWorker sut = createSut(client, ddlService);
            sut.run();
            sut.awaitStatus();
        } catch (Exception e) {
            assertThat(e, instanceOf(ExecutionException.class));
            firstNode.join();
            return;
        }

        Assert.fail("Exception must be thrown");
    }

    @Test
    public void twoNodesPartialSuccessThenSuccess() throws Exception {
        FakeUpdateDDLService ddlService = new FakeUpdateDDLService() {
            @Override
            protected void run() throws InterruptedException {
                Thread.sleep(10);
                notifyWatchers(new UpdateDDLTaskExecutorResult.PartialSuccess());

                Thread.sleep(10);
                notifyWatchers(new UpdateDDLTaskExecutorResult.Success());
            }
        };

        Thread firstNode = new Thread(() -> {
            CuratorFramework client = createClient();
            UpdateDDLWorker sut = createSut(client, ddlService);
            try {
                sut.run();
                UpdateDDLStatus status = sut.awaitStatus();
                Assert.assertEquals(UpdateDDLStatus.PARTIAL_SUCCESS, status);
                Assert.assertFalse(sut.isFinished());

                waitUntilFinished(sut);
            } catch (Exception e) {
                Assert.fail(e.toString());
            }
        });
        firstNode.start();

        CuratorFramework client = createClient();
        UpdateDDLWorker sut = createSut(client, ddlService);
        sut.run();
        UpdateDDLStatus status = sut.awaitStatus();

        Assert.assertEquals(UpdateDDLStatus.PARTIAL_SUCCESS, status);

        waitUntilFinished(sut);

        firstNode.join();
    }

    @Test
    @Ignore("algebraic: к сожалению, этот тест работает в Teamcity крайне нестабильно")
    public void twoNodesMasterFailThenSuccess() throws Exception {
        FakeUpdateDDLService ddlService = new FakeUpdateDDLService() {
            private AtomicInteger runCount = new AtomicInteger(0);

            @Override
            protected void run() throws InterruptedException {
                int runCountLocal = runCount.incrementAndGet();
                if (runCountLocal == 1) {
                    throw new IllegalStateException();
                } else {
                    notifyWatchers(new UpdateDDLTaskExecutorResult.Success());
                }
            }
        };

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        Runnable worker = () -> {
            CuratorFramework client = createClient();
            UpdateDDLWorker sut = createSut(client, ddlService);
            try {
                sut.run();
                UpdateDDLStatus status = sut.awaitStatus();

                if (status.equals(UpdateDDLStatus.SUCCESS)) {
                    successCount.getAndIncrement();
                }
            } catch (Exception e) {
                failCount.getAndIncrement();
            }
        };

        Thread firstNode = new Thread(worker);
        firstNode.start();
        Thread secondNode = new Thread(worker);
        secondNode.start();

        firstNode.join();
        secondNode.join();

        Assert.assertEquals("Success count differs!", 1, successCount.get());
        Assert.assertEquals("Fail count differs!", 1, failCount.get());
    }

    @Test(timeout = 5_000)
    public void threeNodesSuccess() throws Exception {
        FakeUpdateDDLService ddlService = new FakeUpdateDDLService(new UpdateDDLTaskExecutorResult.Success());

        Thread firstNode = new Thread(() -> {
            CuratorFramework client = createClient();
            UpdateDDLWorker sut = createSut(client, ddlService);
            try {
                sut.run();
                sut.awaitStatus();
            } catch (Exception e) {
                Assert.fail(e.toString());
            }
        });
        firstNode.start();

        Thread secondNode = new Thread(() -> {
            CuratorFramework client = createClient();
            UpdateDDLWorker sut = createSut(client, ddlService);
            try {
                sut.run();
                sut.awaitStatus();
            } catch (Exception e) {
                Assert.fail(e.toString());
            }
        });
        secondNode.start();

        CuratorFramework client = createClient();
        UpdateDDLWorker sut = createSut(client, ddlService);
        sut.run();
        UpdateDDLStatus status = sut.awaitStatus();

        Assert.assertEquals(UpdateDDLStatus.SUCCESS, status);
        waitUntilFinished(sut);

        firstNode.join();
        secondNode.join();
    }

    private UpdateDDLWorker createSut(CuratorFramework client, UpdateDDLService ddlService, MonitoringUnit monitoringUnit) {
        try {
            UpdateDDLWorker sut = new UpdateDDLWorker(client, ddlService, new ArrayList<>(), monitoringUnit, 12345);
            sut.setTimeToWaitFollowersToGetStatusMillis(1000);
            return sut;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private UpdateDDLWorker createSut(CuratorFramework client, UpdateDDLService ddlService) {
        return createSut(client, ddlService, mock(MonitoringUnit.class));
    }

    private static TestingServer createServer() throws Exception {
        int tryNumber = 0;
        while (true) {
            try {
                server = new TestingServer();
                System.out.println("Server started");
                return server;
            } catch (BindException e) {
                if (tryNumber >= 3) {
                    throw e;
                }

                ++tryNumber;
            }
        }
    }

    private CuratorFramework createClient() {
        CuratorFramework client = CuratorFrameworkFactory.builder()
            .connectString("localhost:" + server.getPort())
            .retryPolicy(new RetryNTimes(Integer.MAX_VALUE, (int) TimeUnit.SECONDS.toMillis(30)))
            .namespace(namespace)
            .build();

        client.start();

        CuratorFrameworkState state = client.getState();
        if (!state.equals(CuratorFrameworkState.STARTED)) {
            throw new IllegalStateException("Curator framework hasn't started, current state: " + state);
        }

        System.out.println("Client started");
        return client;
    }

    private void waitUntilFinished(UpdateDDLWorker worker) throws TimeoutException {
        waitUntilFinished(worker, 5000);
    }

    private void waitUntilFinished(UpdateDDLWorker worker, int timeoutMillis) throws TimeoutException {
        long startTime = System.currentTimeMillis();

        while (!worker.isFinished()) {
            if (System.currentTimeMillis() - startTime > timeoutMillis) {
                throw new TimeoutException("UpdateDDLWorker finish timeout " + timeoutMillis + "ms exceeded");
            }
        }
    }

    private DDL createDDL(DdlQueryType queryType) {
        DDL ddl = new DDL("host");
        ddl.addManualQuery(new DdlQuery(queryType, MANUAL_QUERY));
        return ddl;
    }

}