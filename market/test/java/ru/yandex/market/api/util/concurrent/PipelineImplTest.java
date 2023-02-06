package ru.yandex.market.api.util.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.market.api.util.concurrent.PipelineImpl.MultiResultImpl;
import ru.yandex.market.api.util.concurrent.Pipelines.MultiResult;
import ru.yandex.market.http.FuturesHelperImpl;
import ru.yandex.market.http.concurrent.FuturesHelper;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 *
 * Created by apershukov on 02.10.16.
 */
public class PipelineImplTest {

    private static class Result {
        String field1;
        String field2;
        String field3;
    }

    private static AsyncExecutor executor;

    @BeforeClass
    public static void setUpClass() {
        FuturesHelper futuresHelper = new FuturesHelperImpl(new NioEventLoopGroup());
        executor = new AsyncExecutor(futuresHelper);
        Futures.futuresConf = new FuturesConf(
                futuresHelper,
                5000,
                5000,
                e -> {}
        );
    }

    @Test
    public void testExecuteSequentAsyncActions() {
        Queue<String> queue = new ConcurrentLinkedQueue<>();

        Future<?> future = Pipelines
                .startWith(executor.run(() -> queue.add("first")))
                .then(arg -> executor.run(() -> queue.add("second")))
                .then(arg -> executor.run(() -> queue.add("third")));

        Futures.wait(future);

        assertEquals("first", queue.poll());
        assertEquals("second", queue.poll());
        assertEquals("third", queue.poll());
    }

    @Test
    public void testExecuteSequentMixedActions() {
        Future<String> future = Pipelines.startWith(executor.call(() -> "value"))
                .thenSync(v -> v + "1")
                .thenSync(v -> v + "2")
                .then(v -> executor.call(() -> v + "3"));

        assertEquals("value123", Futures.waitAndGet(future));
    }

    @Test
    public void testExecuteParallelResultProcessing() {
        Future<Result> future = Pipelines.startWith(executor.call(Result::new))
                .thenParallel()
                    .run(v -> executor.run(() -> v.field1 = "value1"))
                    .run(v -> executor.run(() -> v.field2 = "value2"))
                    .run(v -> executor.run(() -> v.field3 = "value3"))
                .end();

        Result result = Futures.waitAndGet(future);

        assertEquals("value1", result.field1);
        assertEquals("value2", result.field2);
        assertEquals("value3", result.field3);
    }

    @Test
    public void testCollectResultFromFutures() {
        Future<MultiResult> future = Pipelines.startWith(executor.call(() -> "value1"),
                    executor.call(() -> "value2"),
                    executor.call(() -> "value3"),
                    executor.call(() -> "value4"))
                .thenSync(v -> v);

        MultiResult result = Futures.waitAndGet(future);
        assertEquals(new MultiResultImpl(new String[] {"value1", "value2", "value3", "value4"}, null), result);
    }

    @Test
    public void testCollectResultsFromParallelPhase() {
        Future<MultiResult> future = Pipelines.startWith(executor.call(() -> "value"))
                .thenParallel()
                    .call(v -> executor.call(() -> v + "1"))
                    .call(v -> executor.call(() -> v + "2"))
                    .call(v -> executor.call(() -> v + "3"))
                .endAndCollect();

        MultiResult result = Futures.waitAndGet(future);
        assertEquals(new MultiResultImpl(new String[] {"value1", "value2", "value3"}, null), result);
    }

    /**
     * Тестирование того что падение отдельного таска в первой фазе не приводит к
     * падению всего pipeline'а
     */
    @Test
    public void testHandleErrorIfParallelTasks() {
        Future<MultiResult> future = Pipelines.startWith(executor.call(() -> "value1"),
                executor.call(() -> {
                    throw new RuntimeException();
                }));

        MultiResult result = Futures.waitAndGet(future);
        assertEquals("value1", result.get(0));
        assertNull(result.get(1));
    }

    @Test
    public void testSwitchPhase() {
        Future<String> future = Pipelines.startWith(executor.call(() -> "value2"))
                .thenSwitch()
                    .runIf("value1"::equals, v -> executor.call(() -> v + "1"))
                    .runIf("value2"::equals, v -> executor.call(() -> v + "2"))
                    .runSyncDefault(v -> v + "3");

        String result = Futures.waitAndGet(future);

        assertEquals("value22", result);
    }

    @Test
    public void testDefaultSwitch() {
        Future<String> future = Pipelines.startWith(executor.call(() -> "value3"))
                .thenSwitch()
                    .runSyncIf("value1"::equals, v -> executor.call(() -> v + "1"))
                    .runSyncIf("value2"::equals, v -> executor.call(() -> v + "2"))
                    .runSyncDefault(v -> "value3");

        String result = Futures.waitAndGet(future);

        assertEquals("value3", result);
    }

    @Test
    public void testRunDefaultAsync() {
        Future<String> future = Pipelines.startWith(executor.call(() -> "value3"))
                .thenSwitch()
                    .runSyncIf("value1"::equals, v -> v + "1")
                    .runSyncIf("value2"::equals, v -> v + "2")
                    .runDefault(v -> executor.call(() -> "value"));

        String result = Futures.waitAndGet(future);

        assertEquals("value", result);
    }

    @Test
    public void testErrorWithSeqProcessing() {
        Future<String> future = Pipelines.startWith(executor.call(() -> "value"))
                .then(v -> executor.call(() -> v + "1"))
                .<String> then(v -> executor.call(() -> {
                    throw new RuntimeException();
                }))
                .then(v -> executor.call(() -> v + "3"));

        Futures.wait(future);

        assertFalse(future.isSuccess());
        assertEquals(RuntimeException.class, future.cause().getClass());
    }

    @Test
    public void testErrorWithParallelProcessing() {
        Future<Result> future = Pipelines.startWith(executor.call(Result::new))
                .thenParallel()
                    .run(v -> executor.run(() -> v.field1 = "value1"))
                    .run(v -> executor.run(() -> {
                        throw new RuntimeException();
                    }))
                    .run(v -> executor.run(() -> v.field3 = "value3"))
                .end();

        Result result = Futures.waitAndGet(future);

        assertTrue(future.isSuccess());

        assertEquals("value1", result.field1);
        assertNull(result.field2);
        assertEquals("value3", result.field3);
    }

    @Test
    public void testErrorWithUnsafeParallelProcessing() {
        Future<Result> future = Pipelines.startWith(executor.call(Result::new))
                .thenParallelUnsafe()
                    .run(v -> executor.run(() -> v.field1 = "value1"))
                    .call(v -> executor.run(() -> {
                        throw new RuntimeException();
                    }))
                    .run(v -> executor.run(() -> v.field3 = "value3"))
                .end();

        Futures.wait(future);

        assertFalse(future.isSuccess());
        assertEquals(RuntimeException.class, future.cause().getClass());
    }

    @Test
    public void testSyncPipelines() {
        Future<Result> future =  Pipelines.startWith(Pipelines.startWith(executor.call(() -> "value1")),
                    Pipelines.startWith(executor.call(() -> "value2")),
                    Pipelines.startWith(executor.call(() -> "value3")))
                .thenSync(values -> {
                    Result result = new Result();
                    result.field1 = values.get(0);
                    result.field2 = values.get(1);
                    result.field3 = values.get(2);
                    return result;
                });

        Result result = Futures.waitAndGet(future);

        assertEquals("value1", result.field1);
        assertEquals("value2", result.field2);
        assertEquals("value3", result.field3);
    }

    @Test
    public void testPseudoParallelSyncWithWait() {
        Future<Result> future = Pipelines.startWith(Futures.newSucceededFuture("value1"),
                    Futures.newSucceededFuture("value2"),
                    Futures.newSucceededFuture("value3"))
                .thenSync(values -> {
                    Result result = new Result();
                    result.field1 = values.get(0);
                    result.field2 = values.get(1);
                    result.field3 = values.get(2);
                    return result;
                });

        Result result = Futures.waitAndGet(future);

        assertEquals("value1", result.field1);
        assertEquals("value2", result.field2);
        assertEquals("value3", result.field3);
    }

    /**
     * Проверка того что pipeline не будет завершен раньше времени если после параллельной обработки
     * идет длительная асинхронная фаза
     */
    @Test
    public void testParallelProcessingWithLongAggregatePhase() {
        Future<Result> future = Pipelines.startWith(executor.call(Result::new))
                .thenParallel()
                    .run(result -> executor.run(() -> result.field1 = "value1"))
                    .run(result -> executor.run(() -> result.field2 = "value2"))
                    .run(result -> executor.run(() -> result.field3 = "value3"))
                .end()
                .then(result ->
                        executor.call(() -> {
                        try {
                            // Ждем гарантировано дольше чем выполняется предыдущая фаза
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        result.field1 = "altered1";
                        result.field2 = "altered2";
                        result.field3 = "altered3";
                        return result;
                    })
                );

        Result result = Futures.waitAndGet(future);

        assertEquals("altered1", result.field1);
        assertEquals("altered2", result.field2);
        assertEquals("altered3", result.field3);
    }

    /**
     * Тестирование случая когда параллельная обработка догоняет построение pipeline
     */
    @Test
    public void testRunPseudoParallelProcessing() {
        Future<Result> future = Pipelines.startWithValue(new Result())
                .thenParallel()
                    .run(result -> {
                        result.field1 = "value1";
                        return Futures.newSucceededFuture(null);
                    })
                    .run(result -> {
                        result.field2 = "value2";
                        return Futures.newSucceededFuture(null);
                    })
                    .run(result -> {
                        result.field3 = "value3";
                        return Futures.newSucceededFuture(null);
                    })
                .end();

        Result result = Futures.waitAndGet(future);

        assertEquals("value1", result.field1);
        assertEquals("value2", result.field2);
        assertEquals("value3", result.field3);
    }

    /**
     * Тестирование случая когда параллельная обработка догоняет построение pipeline
     * при этом параллельная фаза заканчивается сбором результатов
     */
    @Test
    public void testRunPseudoParallelProcessingWithResultCollect() {
        Future<String[]> future = Pipelines.startWithValue("value")
                .thenParallel()
                    .call(v -> Futures.newSucceededFuture(v + 1))
                    .call(v -> Futures.newSucceededFuture(v + 2))
                    .call(v -> Futures.newSucceededFuture(v + 3))
                    .endAndCollect()
                .thenSync(values -> {
                    String[] result = new String[3];
                    result[0] = values.get(0);
                    result[1] = values.get(1);
                    result[2] = values.get(2);
                    return result;
                });

        String[] result = Futures.waitAndGet(future);

        assertArrayEquals(new String[] {"value1", "value2", "value3"}, result);
    }

    @Test
    public void testSplitCollectionAndProcessInParallel() {
        Future<List<String>> future = Pipelines.startWithValue( IntStream.range(0, 5)
                    .mapToObj(i -> "value" + i)
                    .collect(Collectors.toList()) )
                .thenSplit(list -> list)
                .parallelUnsafe(str -> executor.call(() -> str + "1"))
                .collect(ArrayList::new);

        List<String> list = Futures.waitAndGet(future);

        assertEquals(Arrays.asList("value01", "value11", "value21", "value31", "value41"), list);
    }

    @Test
    public void testSplitCollectionAndProcessInSerial() {
        Queue<String> queue = new ConcurrentLinkedQueue<>();

        Future<List<String>> future = Pipelines.startWithValue( IntStream.range(0, 5)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.toList()) )
                .thenSplit(list -> list)
                .serial(item -> executor.call(() -> {
                    String value = "value" + item;
                    queue.add(value);
                    return value;
                }))
                .collect(ArrayList::new);

        List<String> list = Futures.waitAndGet(future);

        assertEquals("value0", queue.poll());
        assertEquals("value1", queue.poll());
        assertEquals("value2", queue.poll());
        assertEquals("value3", queue.poll());
        assertEquals("value4", queue.poll());

        assertEquals(Arrays.asList("value0", "value1", "value2", "value3", "value4"), list);
    }

    /**
     * Тестирование того что при безопасной параллельной обработке в случае если вызов отдельного параллельного
     * таска оканчивается неудачей это не влияет на всю обработку
     */
    @Test
    public void testSplitParallelSafe() {
        Future<List<String>> future = Pipelines.startWithValue( IntStream.range(0, 5)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.toList()) )
                .thenSplit(list -> list)
                    .parallel(item -> executor.call(() -> {
                        if ("3".equals(item)) {
                            throw new RuntimeException("Task failed!");
                        }
                        return "value" + item;
                    }))
                .collect(SplitCollectors.notNull());

        List<String> list = Futures.waitAndGet(future);

        assertEquals(Arrays.asList("value0", "value1", "value2", "value4"), list);
    }

    /**
     * Тестирование безопасного выполнения с ошибкой произошедщей в том же потоке из которого запускаются
     * параллельные таски
     */
    @Test
    public void testParallelSafeWithErrorInPipelineThread() {
        Future<List<String>> future = Pipelines.startWithValue( IntStream.range(0, 5)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.toList()) )
                .thenSplit(list -> list)
                    .parallel(item -> {
                        if ("3".equals(item)) {
                            throw new RuntimeException("Task failed!");
                        }
                        return executor.call(() -> "value" + item);
                    })
                .collect(SplitCollectors.notNull());

        List<String> list = Futures.waitAndGet(future);

        assertEquals(Arrays.asList("value0", "value1", "value2", "value4"), list);
    }

    @Test
    public void testSplitParallelUnsafe() {
        Future<List<String>> future = Pipelines.startWithValue( IntStream.range(0, 5)
                .mapToObj(String::valueOf)
                .collect(Collectors.toList()) )
                .thenSplit(list -> list)
                .parallelUnsafe(item -> executor.call(() -> {
                    if ("3".equals(item)) {
                        throw new RuntimeException("Task failed!");
                    }
                    return "value" + item;
                }))
                .collect(SplitCollectors.notNull());

        Futures.wait(future);

        assertFalse(future.isSuccess());

        Throwable cause = future.cause();
        assertEquals(RuntimeException.class, cause.getClass());
        assertEquals("Task failed!", cause.getMessage());
    }

    /**
     * Тестирование не безопасного выполнения с ошибкой произошедщей в том же потоке из которого запускаются
     * параллельные таски
     */
    @Test
    public void testParallelUnsafeWithErrorInPipelineThread() {
        Future<List<String>> future = Pipelines.startWithValue( IntStream.range(0, 5)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.toList()) )
                .thenSplit(list -> list)
                    .parallelUnsafe(item -> {
                        if ("3".equals(item)) {
                            throw new RuntimeException("Task failed!");
                        }
                        return executor.call(() -> "value" + item);
                    })
                .collect(SplitCollectors.notNull());

        Futures.wait(future);

        assertFalse(future.isSuccess());

        Throwable cause = future.cause();
        assertEquals(RuntimeException.class, cause.getClass());
        assertEquals("Task failed!", cause.getMessage());
    }

    @Test
    public void testSafeParallelPhaseWithErrorInPipeThread() {
        Future<Result> future = Pipelines.startWith(executor.call(Result::new))
                .thenParallel()
                    .run(v -> executor.run(() -> v.field1 = "value1"))
                    .run(v -> {
                        throw new RuntimeException();
                    })
                    .run(v -> executor.run(() -> v.field3 = "value3"))
                .end();

        Result result = Futures.waitAndGet(future);

        assertTrue(future.isSuccess());

        assertEquals("value1", result.field1);
        assertNull(result.field2);
        assertEquals("value3", result.field3);
    }

    @Test
    public void testFailedPipelineWithErrorInPipeThread() {
        Future<?> future = Pipelines.startWithValue("Value")
                .then(v -> {
                    throw new RuntimeException("Pipeline failed!");
                });

        Futures.wait(future);

        assertFalse(future.isSuccess());

        Throwable cause = future.cause();
        assertEquals(RuntimeException.class, cause.getClass());
        assertEquals("Pipeline failed!", cause.getMessage());
    }

    /**
     * Тестирование того что передача null в качестве future в первой фазе
     * не приводит к фатальным последствиям
     */
    @Test
    public void testPassNullAsFutureInFirstPhase() {
        Future<MultiResult> future = Pipelines.startWith(null,
                executor.call(() -> "value"));

        MultiResult result = Futures.waitAndGet(future);

        assertNull(result.get(0));
        assertEquals("value", result.get(1));
    }

    @Test
    public void testGetPreviousResultFromPipeline() {
        Future<MultiResult> future = Pipelines.startWithValue("value")
                .thenParallel()
                    .call(v -> executor.call(() -> v + 1))
                    .call(v -> executor.call(() -> v + 2))
                    .call(v -> executor.call(() -> v + 3))
                .endAndCollect();

        MultiResult result = Futures.waitAndGet(future);
        assertEquals("value", result.getPrevious());
    }

    /**
     * Проверка возможности возвращения null из then без NPE
     */
    @Test
    public void testReturnNullFromThen() {
        Future<String> future = Pipelines.startWithValue(executor.call(() -> "value"))
                .then(v -> null);

        String value = Futures.waitAndGet(future);
        assertNull(value);
    }

    @Test
    public void testThenReturn() {
        Future<String> future = Pipelines.startWithValue(executor.call(() -> "value"))
                .thenReturn("hardcoded");

        String value = Futures.waitAndGet(future);
        assertEquals("hardcoded", value);
    }

    @Test
    public void testSplitToInt() {
        Future<List<Integer>> future =  Pipelines.startWithValue(new IntArrayList(new int[] { 1, 2, 3 }))
                .thenSplitToInt(Splitters.identity())
                    .parallel(i -> executor.call(() -> i + 1))
                .collect();

        List<Integer> result = Futures.waitAndGet(future);
        assertEquals(Arrays.asList(2, 3, 4), result);
    }

    @Test
    public void testSafeSplitToInt() {
        Future<List<Integer>> future =  Pipelines.startWithValue(new IntArrayList(new int[] { 1, 2, 3 }))
                .thenSplitToInt(Splitters.identity())
                    .parallel(i -> executor.call(() -> {
                        if (i == 2) {
                            throw new RuntimeException("Task failed");
                        }
                        return i + 1;
                    }))
                .collect();

        List<Integer> result = Futures.waitAndGet(future);
        assertEquals(Arrays.asList(2, null, 4), result);
    }

    @Test
    public void testUnsafeSplitToInt() {
        Future<List<Integer>> future =  Pipelines.startWithValue(new IntArrayList(new int[] { 1, 2, 3 }))
                .thenSplitToInt(Splitters.identity())
                    .parallelUnsafe(i -> executor.call(() -> {
                        if (i == 2) {
                            throw new RuntimeException("Task failed");
                        }
                        return i + 1;
                    }))
                .collect();

        Futures.wait(future);
        assertFalse(future.isSuccess());
    }

    @Test
    public void testSerialSplitToInt() {
        Future<List<String>> future =  Pipelines.startWithValue(new IntArrayList(new int[] { 1, 2, 3 }))
                .thenSplitToInt(Splitters.identity())
                    .serial(i -> executor.call(() -> "value" + i))
                .collect();

        List<String> result = Futures.waitAndGet(future);
        assertEquals(Arrays.asList("value1", "value2", "value3"), result);
    }

    @Test
    public void testSplitToLong() {
        Future<List<String>> future =  Pipelines.startWithValue(new LongArrayList(new long[] { 1, 2, 3 }))
                .thenSplitToLong(Splitters.identity())
                    .parallel(i -> executor.call(() -> "value" + i))
                .collect();

        List<String> result = Futures.waitAndGet(future);
        assertEquals(Arrays.asList("value1", "value2", "value3"), result);
    }

    @Test
    public void testSafeSplitToLong() {
        Future<List<Long>> future =  Pipelines.startWithValue(new LongArrayList(new long[] { 1, 2, 3 }))
                .thenSplitToLong(Splitters.identity())
                    .parallel(i -> executor.call(() -> {
                        if (i == 2) {
                            throw new RuntimeException("Task failed");
                        }
                        return i + 1;
                    }))
                .collect();

        List<Long> result = Futures.waitAndGet(future);
        assertEquals(Arrays.asList(2L, null, 4L), result);
    }

    @Test
    public void testUnsafeSplitToLong() {
        Future<List<Long>> future =  Pipelines.startWithValue(new LongArrayList(new long[] { 1, 2, 3 }))
                .thenSplitToLong(Splitters.identity())
                    .parallelUnsafe(i -> executor.call(() -> {
                        if (i == 2) {
                            throw new RuntimeException("Task failed");
                        }
                        return i + 1;
                    }))
                .collect();

        Futures.wait(future);
        assertFalse(future.isSuccess());
    }

    @Test
    public void testSerialSplitToLong() {
        Future<List<String>> future =  Pipelines.startWithValue(new LongArrayList(new long[] { 1, 2, 3 }))
                .thenSplitToLong(Splitters.identity())
                    .serial(i -> executor.call(() -> "value" + i))
                .collect();

        List<String> result = Futures.waitAndGet(future);
        assertEquals(Arrays.asList("value1", "value2", "value3"), result);
    }
}