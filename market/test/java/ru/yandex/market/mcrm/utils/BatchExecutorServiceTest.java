package ru.yandex.market.mcrm.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class BatchExecutorServiceTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16})
    public void processItems_ByThreadCount_ShouldUseThisCountOfThreads(int threadCount) throws InterruptedException {
        var batchExecutorService = new BatchExecutorService(threadCount, "testExecutor");

        var threadIdQueue = new ConcurrentLinkedQueue<Long>();
        batchExecutorService.processItems(
                List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20),
                Object::hashCode,
                x -> threadIdQueue.add(Thread.currentThread().getId()));
        batchExecutorService.destroy();

        var threadIds = new HashSet<>(threadIdQueue);
        Assertions.assertEquals(threadCount, threadIds.size());
    }

    @Test
    public void processItems_CheckGroupByHashCode() throws InterruptedException {
        var batchExecutorService = new BatchExecutorService(3, "testExecutor");

        var batchQueue = new ConcurrentLinkedQueue<Collection<String>>();
        batchExecutorService.processItems(
                List.of("ss", "ss", "dddd", "dddd", "dddd", "dddd", "aaa", "aaa", "aaa"),
                Object::hashCode,
                batchQueue::add);
        batchExecutorService.destroy();

        Assertions.assertEquals(3, batchQueue.size());
        assertItemsCount(batchQueue, "ss", 2);
        assertItemsCount(batchQueue, "dddd", 4);
        assertItemsCount(batchQueue, "aaa", 3);
    }

    private void assertItemsCount(Collection<Collection<String>> stringBatches, String expected, int expectedCount) {
        for (var stringBatch : stringBatches) {
            if (stringBatch.contains(expected)) {
                Assertions.assertEquals(expectedCount, stringBatch.stream().filter(x -> x.equals(expected)).count());
            }
        }
    }
}
