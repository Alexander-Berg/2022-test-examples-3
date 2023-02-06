package ru.yandex.market.mbo.yt;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.mbo.appcontext.UnstableInit;
import ru.yandex.market.mbo.db.modelstorage.yt.MboYtTable;
import ru.yandex.market.yt.util.table.YtTable;
import ru.yandex.market.yt.util.table.YtTableRpcApi;
import ru.yandex.market.yt.util.table.model.YtColumnSchema;
import ru.yandex.market.yt.util.table.model.YtTableAttributes;
import ru.yandex.market.yt.util.table.model.YtTableModel;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.tables.TableSchema;
import ru.yandex.yt.ytclient.wire.UnversionedRow;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

/**
 * @author apluhin
 * @created 10/4/21
 */
@SuppressWarnings("checkstyle:magicnumber")
public class MultiClusterYtResolverTest {

    private MultiClusterYtResolver multiClusterYtResolver;
    private MboYtTable mboYtTable;
    private YtClient mainMock;
    private YtClient replicaMock;
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    @Before
    public void setUp() throws Exception {
        YtTableModel tableModel = new YtTableModel();
        tableModel.setSchema(YtColumnSchema.create("test", YtColumnSchema.Type.INT64).setSorted(true));
        tableModel.setPath("//tmp");
        tableModel.setAttributes(new YtTableAttributes().setDynamic(true));
        mboYtTable = new MboYtTable(tableModel, YtTable.CreationStatus.EXISTING);
        mainMock = Mockito.mock(YtClient.class);
        replicaMock = Mockito.mock(YtClient.class);
        multiClusterYtResolver = new MultiClusterYtResolver(
            mboYtTable,
            new UnstableInit<>("test", scheduledExecutorService, () -> mainMock),
            new UnstableInit<>("test", scheduledExecutorService, () -> replicaMock),
            new MultiClusterYtResolver.SchedulerConfig(
                SchedulerPolicy.of(30, 60, TimeUnit.SECONDS),
                SchedulerPolicy.of(30, 60, TimeUnit.SECONDS),
                SchedulerPolicy.of(30, 60, TimeUnit.SECONDS)
            )
        );
    }

    @Test
    public void testFallbackToAliveCluster() throws ExecutionException, InterruptedException {
        Mockito.when(mainMock.selectRows(Mockito.anyString())).thenReturn(
            CompletableFuture.supplyAsync(() -> {
                throw new RuntimeException();
            }));
        List<UnversionedRow> result = Collections.emptyList();
        Mockito.when(replicaMock.selectRows(Mockito.anyString())).thenReturn(
            CompletableFuture.supplyAsync(() -> new UnversionedRowset(Mockito.mock(TableSchema.class), result)));

        //select from main
        IntStream.range(0, 5).forEach((i) -> result(multiClusterYtResolver));
        //select from replica
        List<UnversionedRow> rows = multiClusterYtResolver
            .getAliveCluster().getClient().selectRows("").get().getRows();

        Assert.assertEquals(result, rows);
    }

    @Test(timeout = 10000)
    public void testReconnectToMainClusterAfterDowntime() throws ExecutionException, InterruptedException {
        initResolver(
            new MultiClusterYtResolver.SchedulerConfig(
                SchedulerPolicy.of(30, 60, TimeUnit.SECONDS),
                SchedulerPolicy.of(30, 60, TimeUnit.SECONDS),
                SchedulerPolicy.of(3, 1, TimeUnit.SECONDS)
            )
        );

        AtomicInteger counter = new AtomicInteger(0);
        List<UnversionedRow> mainResult = Collections.singletonList(Mockito.mock(UnversionedRow.class));
        List<UnversionedRow> replicaResult = Collections.emptyList();
        Mockito.when(mainMock.selectRows(Mockito.anyString()))
            .thenAnswer((Answer<CompletableFuture<UnversionedRowset>>) call -> CompletableFuture.supplyAsync(() -> {
                if (counter.get() < 5) {
                    counter.incrementAndGet();
                    throw new RuntimeException();
                } else {
                    return new UnversionedRowset(Mockito.mock(TableSchema.class), mainResult);
                }
            }));

        Mockito.when(replicaMock.selectRows(Mockito.anyString())).thenReturn(
            CompletableFuture.supplyAsync(() -> new UnversionedRowset(Mockito.mock(TableSchema.class), replicaResult)));

        //select from main
        IntStream.range(0, 10).forEach((i) -> result(multiClusterYtResolver));

        boolean wait = true;
        while (wait) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException();
            }
            UnversionedRowset result = result(multiClusterYtResolver);
            if (result != null && result.getRows().size() == 1) {
                wait = false;
            }
        }
    }

    @Test(timeout = 10000)
    public void testSecondaryBanAfterDowntime() throws ExecutionException, InterruptedException {
        initResolver(
            new MultiClusterYtResolver.SchedulerConfig(
                SchedulerPolicy.of(30, 60, TimeUnit.SECONDS),
                SchedulerPolicy.of(30, 60, TimeUnit.SECONDS),
                SchedulerPolicy.of(3, 10, TimeUnit.SECONDS)
            )
        );

        AtomicInteger counter = new AtomicInteger(0);
        List<UnversionedRow> result = Collections.singletonList(Mockito.mock(UnversionedRow.class));
        Mockito.when(mainMock.selectRows(Mockito.anyString()))
            .thenAnswer((Answer<CompletableFuture<UnversionedRowset>>) call -> CompletableFuture.supplyAsync(() -> {
                if (counter.get() < 6) {
                    counter.incrementAndGet();
                }
                throw new RuntimeException();
            }));

        Mockito.when(replicaMock.selectRows(Mockito.anyString())).thenAnswer((stub) ->
            CompletableFuture.supplyAsync(() -> {
                if (counter.get() == 6) {
                    //получение этой строки возможно в случае повторного бана кластера
                    return new UnversionedRowset(Mockito.mock(TableSchema.class), result);
                } else {
                    return new UnversionedRowset(Mockito.mock(TableSchema.class), Collections.emptyList());
                }
            }));

        //select from main
        IntStream.range(0, 5).forEach((i) -> result(multiClusterYtResolver));

        boolean wait = true;
        while (wait) {
            try {
                Thread.sleep(500);
                UnversionedRowset rowset = result(multiClusterYtResolver);
                if (rowset != null && rowset.getRows().size() == 1) {
                    wait = false;
                }
            } catch (Exception e) {
                //
            }
        }
    }

    @Test
    public void testReadFromReplicaWithErrors() {
        Mockito.when(mainMock.selectRows(Mockito.anyString())).thenReturn(
            CompletableFuture.supplyAsync(() -> {
                throw new RuntimeException();
            }));

        AtomicInteger counter = new AtomicInteger(0);
        Mockito.when(replicaMock.selectRows(Mockito.anyString())).thenAnswer((stub) ->
            CompletableFuture.supplyAsync(() -> {
                counter.incrementAndGet();
                throw new RuntimeException();
            }));

        //select from main
        IntStream.range(0, 100).forEach((i) -> {
            try {
                multiClusterYtResolver.getAliveCluster().getClient().selectRows("").get();
            } catch (Exception e) {
                //ignore
            }
        });
        Assert.assertEquals(95, counter.get());
    }

    @Test
    public void testCorrectSelectCluster() {
        AtomicReference<MultiClusterYtResolver.YtClusterInfo> mainClusterRef = new AtomicReference<>();
        AtomicReference<MultiClusterYtResolver.YtClusterInfo> replicaClusterRef = new AtomicReference<>();

        ReflectionTestUtils.setField(multiClusterYtResolver, "mainClusterRef", mainClusterRef);
        ReflectionTestUtils.setField(multiClusterYtResolver, "replicaClusterRef", replicaClusterRef);

        MultiClusterYtResolver.YtClusterInfo mainClusterInfo = new MultiClusterYtResolver.YtClusterInfo();
        YtTableRpcApi mainRpcApi = Mockito.mock(YtTableRpcApi.class);
        mainClusterInfo.setRpcApi(mainRpcApi);

        MultiClusterYtResolver.YtClusterInfo replicaClusterInfo = new MultiClusterYtResolver.YtClusterInfo();
        YtTableRpcApi replicaRpcApi = Mockito.mock(YtTableRpcApi.class);
        replicaClusterInfo.setRpcApi(replicaRpcApi);

        replicaClusterRef.set(replicaClusterInfo);
        mainClusterRef.set(null);

        Assert.assertEquals(replicaRpcApi, multiClusterYtResolver.getAliveCluster());

        mainClusterInfo.ban();
        mainClusterRef.set(mainClusterInfo);

        Assert.assertEquals(replicaRpcApi, multiClusterYtResolver.getAliveCluster());

        mainClusterInfo.unban();
        Assert.assertEquals(mainRpcApi, multiClusterYtResolver.getAliveCluster());

        mainClusterInfo.ban();
        replicaClusterRef.set(null);
        Assert.assertEquals(mainRpcApi, multiClusterYtResolver.getAliveCluster());
    }

    private MultiClusterYtResolver initResolver(MultiClusterYtResolver.SchedulerConfig config) {
        multiClusterYtResolver = new MultiClusterYtResolver(
            mboYtTable,
            new UnstableInit<>("test", scheduledExecutorService, () -> mainMock),
            new UnstableInit<>("test", scheduledExecutorService, () -> replicaMock),
            config
        );
        return multiClusterYtResolver;
    }

    private UnversionedRowset result(MultiClusterYtResolver resolver) {
        try {
            return resolver.getAliveCluster().getClient().selectRows("").get();
        } catch (Exception e) {
            return null;
        }
    }

}
