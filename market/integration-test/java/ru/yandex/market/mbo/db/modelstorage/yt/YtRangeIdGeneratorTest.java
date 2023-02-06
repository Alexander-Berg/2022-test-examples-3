package ru.yandex.market.mbo.db.modelstorage.yt;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.YtTimestamp;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.common.YtException;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.appcontext.UnstableInit;
import ru.yandex.market.mbo.configs.YtTestConfiguration;
import ru.yandex.market.yt.util.table.YtTable;
import ru.yandex.market.yt.util.table.YtTableRpcApi;
import ru.yandex.market.yt.util.table.YtTableService;
import ru.yandex.market.yt.util.table.model.YtTableModel;
import ru.yandex.yt.ytclient.proxy.ApiServiceClient;
import ru.yandex.yt.ytclient.proxy.ApiServiceTransaction;
import ru.yandex.yt.ytclient.proxy.ModifyRowsRequest;
import ru.yandex.yt.ytclient.proxy.SelectRowsRequest;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.rpc.RpcError;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * @author moskovkin@yandex-team.ru
 * @since 02.03.18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {YtTestConfiguration.class})
@SuppressWarnings("checkstyle:magicnumber")
@Ignore
public class YtRangeIdGeneratorTest {
    private static final org.apache.log4j.Logger log = Logger.getLogger(YtRangeIdGeneratorTest.class);

    public static final int GET_IDS_THREAD_COUNT = 3;
    public static final int GET_IDS_DRAIN_SIZE = YtRangeIdGenerator.QUEUE_CAPACITY + 1;
    public static final int GET_IDS_DRAIN_ATTEMPTS = 3;
    public static final int WAIT_BEFORE_NEXT_DRAIN_MS = 500;
    public static final int ATOMIC_TEST_THREADS_COUNT = 5;

    private YtRangeIdGenerator idGenerator;

    @Resource(name = "ytHttpApi")
    private Yt yt;

    @Resource(name = "ytTableService")
    private YtTableService tableService;

    @Resource(name = "idsTable32")
    private YtTableModel idsTable;

    @Resource(name = "ytClient")
    private UnstableInit<YtClient> ytClient;

    private YtTableRpcApi rpcApi;

    private ApiServiceClient apiServiceClient;
    private static final long SEED = 10L;

    @Before
    public void init() throws InterruptedException {
        String expirationTimeISO = DateTimeFormatter.ISO_DATE_TIME.format(
            OffsetDateTime.now().plus(Duration.ofDays(1))
        );

        String newTablePath = idsTable.getYtPath().parent() + "/ids-" + UUID.randomUUID();
        idsTable.setPath(newTablePath);
        YtTable ytTable = tableService.getTable(this.idsTable);

        YPath tableExpirationTime = YPath.simple(newTablePath + "/@expiration_time");
        yt.cypress().set(tableExpirationTime, expirationTimeISO);

        // Mount table
        Thread.sleep(5000);

        rpcApi = new YtTableRpcApi(ytTable, ytClient.get());

        idGenerator = createYtGenerator(Integer.MAX_VALUE);

        apiServiceClient = ytClient.get();
    }

    private YtRangeIdGenerator createYtGenerator(long maxValue) {
        return new YtRangeIdGenerator(idsTable, maxValue, ytClient);
    }

    private InterceptedYtRangeIdGenerator createInterceptedIdGenerator(Runnable interceptor) {
        InterceptedYtRangeIdGenerator result = new InterceptedYtRangeIdGenerator(
            idsTable, Integer.MAX_VALUE, ytClient, interceptor);
        result.deferredInit();
        return result;
    }

    private void saveChunk(long begin, long count, boolean free) {
        rpcApi.doInTransaction(YtRangeIdGenerator.TRANSACTION_OPTIONS, transaction -> {
            ModifyRowsRequest updateChunksRequest = rpcApi.createModifyRowRequest();
            updateChunksRequest.addUpdate(IdsChunk.Builder.anIdsChunk()
                .withBegin(begin)
                .withCount(count)
                .withFree(free)
                .build()
                .toYtRecord()
            );
            transaction.modifyRows(updateChunksRequest).join();
            return true;
        });
    }

    @After
    public void clean() {
        YtTable table = tableService.getTable(idsTable);
        tableService.unmountTable(table);
        tableService.removeTable(table);
    }

    @Test
    public void testInit() {
        idGenerator.deferredInit();

        List<IdsChunk> chunks = new ArrayList<>();
        yt.tables().selectRows("* FROM [" + idsTable.getPath() + "]", YtRangeIdGenerator.IDS_CHUNK_TYPE,
            chunk -> {
                chunks.add(chunk);
            }
        );

        Assert.assertEquals(1, chunks.size());
        IdsChunk chunk = chunks.get(0);
        Assert.assertEquals(YtRangeIdGenerator.DEFAULT_SEED, chunk.getBegin().longValue());
        Assert.assertEquals(idGenerator.getMaxId() - YtRangeIdGenerator.DEFAULT_SEED + 1, chunk.getCount().longValue());
        Assert.assertTrue(chunk.getFree());
    }

    @Test
    @Ignore("Covered in testInit, should delete?")
    public void testInitSeedGeneratorChunk() {
        idGenerator.deferredInit();

        List<IdsChunk> chunks = new ArrayList<>();
        yt.tables().selectRows("* FROM [" + idsTable.getPath() + "]", YtRangeIdGenerator.IDS_CHUNK_TYPE,
            chunk -> {
                chunks.add(chunk);
            }
        );

        Assert.assertEquals(1, chunks.size());
        IdsChunk chunk = chunks.get(0);

        long expectedBegin = SEED + YtRangeIdGenerator.SEED_GENERATOR_OFFSET;
        Assert.assertEquals(expectedBegin, chunk.getBegin().longValue());
        Assert.assertEquals(idGenerator.getMaxId() - expectedBegin + 1, chunk.getCount().longValue());
        Assert.assertTrue(chunk.getFree());
    }

    @Test
    @Ignore("Covered in testMinId, should delete?")
    public void testInitSeedGenerator() {
        idGenerator.deferredInit();
        List<Long> ids = idGenerator.getIds(100);
        Long minId = Collections.min(ids);

        long expectedMinId = SEED + YtRangeIdGenerator.SEED_GENERATOR_OFFSET;
        Assert.assertEquals(minId.longValue(), expectedMinId);
    }

    @Test
    public void testMinId() {
        idGenerator.deferredInit();

        List<Long> ids = idGenerator.getIds(100);
        Long minId = Collections.min(ids);

        Assert.assertEquals(YtRangeIdGenerator.DEFAULT_SEED, minId.longValue());
    }

    @Test(expected = Exception.class)
    public void testMissconfiguration() {
        idGenerator = createYtGenerator(YtRangeIdGenerator.DEFAULT_SEED - 1);
        idGenerator.deferredInit();
    }

    @Test
    public void testMaxId() {
        saveChunk(SEED, YtRangeIdGenerator.QUEUE_CAPACITY * 3, true);

        // Generator may return ids SEED ... YtRangeIdGenerator.QUEUE_CAPACITY
        // Total count of available ids YtRangeIdGenerator.QUEUE_CAPACITY + 1
        idGenerator = createYtGenerator(SEED + YtRangeIdGenerator.QUEUE_CAPACITY);
        idGenerator.deferredInit();

        List<Long> ids = idGenerator.getIds(YtRangeIdGenerator.QUEUE_CAPACITY);
        ids.sort(Long::compareTo);

        List<Long> expected = LongStream.range(SEED, SEED + YtRangeIdGenerator.QUEUE_CAPACITY)
            .boxed()
            .collect(Collectors.toList());

        Assert.assertArrayEquals(expected.toArray(),  ids.toArray());

        try {
            idGenerator.getId();
            Assert.fail("Should not be able to get id here");
        } catch (Throwable e) {
        }
    }

    @Test
    public void testGetIds() {
        saveChunk(1, 2, false); // Used chunk
        saveChunk(10, 2, true); // Free chunk
        saveChunk(20, 2, false); // Used chunk
        saveChunk(30, 10, true); // Free chunk
        saveChunk(40, YtRangeIdGenerator.QUEUE_CAPACITY, true);

        idGenerator.deferredInit();

        List<Long> ids = idGenerator.getIds(5);
        // Free chunks created before should be converted to ids
        Assert.assertArrayEquals(new Long[] {10L, 11L, 30L, 31L, 32L},  ids.toArray());
    }

    @Test
    public void testReservedIntervals() {
        saveChunk(1, 2, false); // Used chunk
        saveChunk(10, 2, true); // Free chunk
        saveChunk(20, 2, false); // Used chunk
        saveChunk(30, 2, true); // Free chunk
        saveChunk(40, YtRangeIdGenerator.QUEUE_CAPACITY, true);

        idGenerator.deferredInit();
        List<Long> reservedIds = idGenerator.getIds(5);

        // Convert chunk table contents to list of free ids
        List<Long> freeIds = getAllFreeIdsFromTable();
        reservedIds.retainAll(freeIds);
        Assert.assertTrue(reservedIds.isEmpty());
    }

    private List<Long> getAllFreeIdsFromTable() {

        List<IdsChunk> chunks = new ArrayList<>();
        yt.tables().selectRows("* FROM [" + idsTable.getPath() + "] WHERE free = true ORDER BY begin LIMIT 1000",
            YtRangeIdGenerator.IDS_CHUNK_TYPE,
            chunk -> {
                chunks.add(chunk);
            }
        );

        List<Long> result = chunks.stream()
                .map(chunk -> LongStream.range(chunk.getBegin(), chunk.getBegin() + chunk.getCount()))
                .flatMapToLong(longStream -> longStream)
                .sorted()
                .boxed()
                .collect(Collectors.toList());
        return result;
    }

    @Test
    @Ignore
    @Repeat(value = 4)
    public void testNoDuplicates() throws InterruptedException, ExecutionException {
        List<Long>  reservedIds = Collections.synchronizedList(new ArrayList<>());
        List<Future<Boolean>> results = new ArrayList<>();
        // Populates
        ExecutorService executor = Executors.newFixedThreadPool(GET_IDS_THREAD_COUNT);
        for (int i = 0; i < GET_IDS_THREAD_COUNT; i++) {
            Future<Boolean> result = executor.submit(() -> {
                try {
                    YtRangeIdGenerator generaor = createYtGenerator(Integer.MAX_VALUE);
                    generaor.deferredInit();
                    for (int j = 0; j < GET_IDS_DRAIN_ATTEMPTS; j++) {
                        List<Long> newIds = generaor.getIds(GET_IDS_DRAIN_SIZE);
                        reservedIds.addAll(newIds);
                        Thread.sleep(WAIT_BEFORE_NEXT_DRAIN_MS);
                    }
                    return true;
                } catch (Throwable e) {
                    log.error("Exception while getting ids", e);
                    return false;
                }
            });
            results.add(result);
        }

        for (Future<Boolean> result : results) {
            Assert.assertTrue(result.get());
        }

        Assert.assertEquals(GET_IDS_THREAD_COUNT * GET_IDS_DRAIN_ATTEMPTS * GET_IDS_DRAIN_SIZE, reservedIds.size());
        Assert.assertEquals(reservedIds.size(), reservedIds.stream().distinct().count());
    }

    @Test
    @Ignore
    @Repeat(value = 4)
    public void testNoFreeAndReservedIntersection() throws InterruptedException, ExecutionException {
        long freeSize = GET_IDS_THREAD_COUNT * GET_IDS_DRAIN_ATTEMPTS * GET_IDS_DRAIN_SIZE
            + YtRangeIdGenerator.QUEUE_CAPACITY * 100;

        saveChunk(1, freeSize, true);

        List<Long> reservedIds = Collections.synchronizedList(new ArrayList<>());
        List<Future<Boolean>> results = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(GET_IDS_THREAD_COUNT);
        for (int i = 0; i < GET_IDS_THREAD_COUNT; i++) {
            Future<Boolean> result = executor.submit(() -> {
                try {
                    YtRangeIdGenerator generator = createYtGenerator(Integer.MAX_VALUE);
                    generator.deferredInit();
                    for (int j = 0; j < GET_IDS_DRAIN_ATTEMPTS; j++) {
                        List<Long> newIds = generator.getIds(GET_IDS_DRAIN_SIZE);
                        reservedIds.addAll(newIds);
                    }
                    return true;
                } catch (Throwable e) {
                    log.error("Exception while getting ids", e);
                    return false;
                }
            });
            results.add(result);
        }

        for (Future<Boolean> result : results) {
            Assert.assertTrue(result.get());
        }

        List<Long> freeIdsInTable = getAllFreeIdsFromTable();

        // Something reserved
        Assert.assertFalse(reservedIds.isEmpty());

        // Reserved and free do not intersect
        reservedIds.retainAll(freeIdsInTable);
        Assert.assertTrue(reservedIds.isEmpty());
    }


    @Test
    public void testBigDrain() {
        idGenerator.deferredInit();

        int count = YtRangeIdGenerator.QUEUE_CAPACITY * 3;
        List<Long> ids = idGenerator.getIds(count);
        List<Long> expectedIds = LongStream.range(
                YtRangeIdGenerator.DEFAULT_SEED,
                YtRangeIdGenerator.DEFAULT_SEED + count
            )
            .boxed()
            .collect(Collectors.toList());
        Assert.assertArrayEquals(expectedIds.toArray(), ids.toArray());
    }

    @Test
    @Repeat(value = 3)
    @Ignore("Flapping, fix here: MBO-23863")
    public void testAtomicity() throws ExecutionException, InterruptedException {

        saveChunk(1, 10_000_000, true);

        CountDownLatch ready = new CountDownLatch(ATOMIC_TEST_THREADS_COUNT);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<Boolean>> results = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(ATOMIC_TEST_THREADS_COUNT);
        for (int i = 0; i < ATOMIC_TEST_THREADS_COUNT; i++) {
            Future<Boolean> result = executor.submit(() -> {
                ready.countDown();
                start.await();

                for (int j = 0; j < 5; j++) {
                    try {
                        return atomicChange();
                    } catch (RpcError | CompletionException | YtException e) {
                        log.warn("YT error: ", e);
                        continue;
                    }
                }
                log.warn("No more retries left");
                return true;
            });
            results.add(result);
        }

        ready.await();
        start.countDown();

        for (Future<Boolean> result : results) {
            Assert.assertTrue(result.get());
        }
    }

    @NotNull
    private Boolean atomicChange() {

        try (ApiServiceTransaction transaction = apiServiceClient
            .startTransaction(YtRangeIdGenerator.TRANSACTION_OPTIONS).join()) {

            SelectRowsRequest select = SelectRowsRequest.of(
                "begin, count, free FROM [" + idsTable.getPath() + "] WHERE free = true");
            select.setTimestamp(transaction.getStartTimestamp());

            List<YTreeMapNode> rows = apiServiceClient.selectRows(select)
                .join()
                .getYTreeRows();

            List<IdsChunk> freeChunks = new ArrayList<>();
            for (YTreeMapNode row : rows) {
                IdsChunk chunk = IdsChunk.Builder.anIdsChunk()
                    .withBegin(row.getLong("begin"))
                    .withCount(row.getLong("count"))
                    .withFree(row.getBool("free"))
                    .build();

                freeChunks.add(chunk);
            }

            if (freeChunks.isEmpty()) {
                Assert.fail("No free chunks found");
            }

            if (freeChunks.size() > 1) {
                Assert.fail("More than one chunk found: " + freeChunks);
            }

            IdsChunk freeChunk = freeChunks.iterator().next();

            IdsChunk newFreeChunk = IdsChunk.Builder.anIdsChunk()
                .withBegin(freeChunk.getBegin() + 1)
                .withCount(freeChunk.getCount() - 1)
                .withFree(true)
                .build();

            IdsChunk usedChunk = IdsChunk.Builder.anIdsChunk()
                .withBegin(freeChunk.getBegin())
                .withCount(1L)
                .withFree(false)
                .build();

            ModifyRowsRequest modifyRequest = rpcApi.createModifyRowRequest();
            modifyRequest.addUpdate(usedChunk.toYtRecord());
            modifyRequest.addUpdate(newFreeChunk.toYtRecord());

            transaction.modifyRows(modifyRequest);
            transaction.commit().join();

            log.debug("Wrote chunks: " + usedChunk + ", " + newFreeChunk);

            return true;
        }
    }

    @Test
    public void testTransactions() {
        saveChunk(1, YtRangeIdGenerator.QUEUE_CAPACITY * 3, true);
        idGenerator.deferredInit();

        List<Long> ids = new ArrayList<>();
        YtRangeIdGenerator interceptedIdGenerator = createInterceptedIdGenerator(
            // This code will be called inside interceptedIdGenerator get ids transaction
            // Following happens
            // 1) interceptedIdGenerator starts transaction
            // 2) interceptedIdGenerator reads free chunks
            // 3) idGenerator starts transaction
            // 4) idGenerator reads free chunks
            // 5) idGenerator mark chunks as reserved in table
            // 6) interceptedIdGenerator try to mark chunks as reserved in table because them occuped by idGenerator
            // 7) interceptedIdGenerator read next free interval of chunks
            // 8) interceptedIdGenerator mark new chunks as reserved
            () -> ids.addAll(idGenerator.getIds(10))
        );
        List<Long> idsFromIntercepted = interceptedIdGenerator.getIds(10);

        List<Long> expectedIds = LongStream.range(1, 11)
            .boxed()
            .collect(Collectors.toList());
        Assert.assertArrayEquals(expectedIds.toArray(), ids.toArray());

        List<Long> expectedIdsFromIntercepted = LongStream.range(
                YtRangeIdGenerator.QUEUE_CAPACITY + 1,
                YtRangeIdGenerator.QUEUE_CAPACITY + 11
            )
            .boxed()
            .collect(Collectors.toList());
        Assert.assertArrayEquals(expectedIdsFromIntercepted.toArray(), idsFromIntercepted.toArray());
    }

    private static class InterceptedYtRangeIdGenerator extends YtRangeIdGenerator {
        private Runnable interceptor;

        InterceptedYtRangeIdGenerator(YtTableModel table,
                                      long maxId,
                                      UnstableInit<YtClient> rpcClient,
                                      Runnable interceptor) {
            super(table, maxId, rpcClient);
            this.interceptor = interceptor;
        }

        @NotNull
        @Override
        protected List<IdsChunk> getFreeChunks(YtTimestamp timestamp, int count) {
            List<IdsChunk> result = super.getFreeChunks(timestamp, count);
            if (interceptor != null) {
                interceptor.run();
                interceptor = null;
            }
            return result;
        }
    }
}
