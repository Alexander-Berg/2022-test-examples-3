package ru.yandex.market.crm.lb.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.lb.impl.MessageDispenser.FlushTask;
import ru.yandex.market.crm.util.Exceptions.TrashConsumer;
import ru.yandex.market.crm.util.Exceptions.TrashRunnable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author apershukov
 */
public class MessageDispenserTest {

    private static final String PARTITION_ID = "partition_id";
    private static final String CONSUMER_ID = "consumer_id";

    private static Message result(long offset, Long cookie, String... data) {
        return new Message(List.of(data), offset, cookie);
    }

    private static Message result(long offset, String... data) {
        return result(offset, null, data);
    }

    private static void assertResult(String expectedData,
                                     long expectedOffset,
                                     Long expectedCookie,
                                     Message result) {
        assertEquals(List.of(expectedData), result.getParsedData());
        assertEquals(expectedOffset, result.getOffset());
        assertEquals(expectedCookie, result.getCookie());
    }

    private static void doAsync(TrashRunnable runnable) {
        new Thread(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private static void pollAsync(MessageDispenser queue, TrashConsumer<FlushTask> callback) {
        doAsync(() -> queue.pullNextTask(task -> {
            try {
                callback.accept(task);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }

    /**
     * Пустая очередь не выдает задачи
     */
    @Test
    public void testEmptyQueueGivesOutNoTasks() throws InterruptedException {
        MessageDispenser queue = new MessageDispenser(1, 30_000, 15_000, 4);

        CountDownLatch taskIssuedLatch = new CountDownLatch(1);

        pollAsync(queue, task -> taskIssuedLatch.countDown());

        assertFalse(taskIssuedLatch.await(500, TimeUnit.MILLISECONDS), "Something is issued");
    }

    /**
     * В случае если партиция не была зарегистрирована сообщения от нее игнорируются
     */
    @Test
    public void testIgnoreUnregisteredPartitionMessages() throws Exception {
        MessageDispenser queue = new MessageDispenser(1, 30_000, 15_000, 4);

        queue.put(PARTITION_ID, CONSUMER_ID, result(1, "message-1"));
        queue.put(PARTITION_ID, CONSUMER_ID, result(2, "message-2"));

        CountDownLatch latch = new CountDownLatch(1);
        pollAsync(queue, task -> latch.countDown());

        assertFalse(latch.await(250, TimeUnit.MILLISECONDS), "Queue has messages");
    }

    /**
     * Если размер пачки равен единице результаты выдаются по одному
     */
    @Test
    public void testSingleMessageProcessing() throws InterruptedException {
        MessageDispenser queue = new MessageDispenser(1, 30_000, 15_000, 4);
        queue.registerPartition(PARTITION_ID);

        queue.put(PARTITION_ID, CONSUMER_ID, result(1, "message-1"));
        queue.put(PARTITION_ID, CONSUMER_ID, result(2, 111L, "message-2"));

        List<FlushTask> tasks = new ArrayList<>();
        queue.pullNextTask(tasks::add);
        queue.pullNextTask(tasks::add);

        assertEquals(2, tasks.size());

        assertEquals(PARTITION_ID, tasks.get(0).getPartitionId());
        assertEquals(CONSUMER_ID, tasks.get(0).getConsumerId());

        List<Message> results1 = tasks.get(0).getMessages();
        assertThat(results1, hasSize(1));
        assertResult("message-1", 1, null, results1.get(0));

        List<Message> results2 = tasks.get(1).getMessages();
        assertThat(results2, hasSize(1));
        assertResult("message-2", 2, 111L, results2.get(0));
    }

    @Test
    public void testPushTriggersWaitingProcessing() throws InterruptedException {
        MessageDispenser queue = new MessageDispenser(1, 30_000, 15_000, 4);
        queue.registerPartition(PARTITION_ID);

        CountDownLatch latch = new CountDownLatch(1);
        BlockingQueue<FlushTask> tasks = new ArrayBlockingQueue<>(1);

        doAsync(() -> {
            latch.countDown();
            queue.pullNextTask(tasks::add);
        });

        latch.await(10, TimeUnit.SECONDS);

        queue.put(PARTITION_ID, CONSUMER_ID, result(1, "message-1"));

        FlushTask task = tasks.poll(1, TimeUnit.SECONDS);
        assertNotNull(task);
        assertEquals("message-1", task.getMessages().get(0).getParsedData().get(0));
    }

    /**
     * Если размер пачки равен трем, при поступлении трех сообщений они выдаются в
     * виде одной таски
     */
    @Test
    public void testMultipleMessageProcessing() throws InterruptedException {
        MessageDispenser queue = new MessageDispenser(3, 30_000, 15_000, 4);
        queue.registerPartition(PARTITION_ID);

        queue.put(PARTITION_ID, CONSUMER_ID, result(1, "message-1"));
        queue.put(PARTITION_ID, CONSUMER_ID, result(2, "message-2"));
        queue.put(PARTITION_ID, CONSUMER_ID, result(3, 111L, "message-3"));

        AtomicReference<FlushTask> taskRef = new AtomicReference<>(null);
        queue.pullNextTask(taskRef::set);

        FlushTask task = taskRef.get();
        assertNotNull(task);

        assertEquals(PARTITION_ID, task.getPartitionId());
        assertEquals(CONSUMER_ID, task.getConsumerId());

        List<Message> results = task.getMessages();
        assertThat(results, hasSize(3));

        assertResult("message-1", 1, null, results.get(0));
        assertResult("message-2", 2, null, results.get(1));
        assertResult("message-3", 3, 111L, results.get(2));
    }

    /**
     * В случае если обработка пачки завершилась с ошибкой она не извлекается из очереди
     * и выдается повторно
     */
    @Test
    public void testIfMessageProcessingFailedItWillBeRetried() throws InterruptedException {
        MessageDispenser queue = new MessageDispenser(1, 30_000, 500, 4);
        queue.registerPartition(PARTITION_ID);

        queue.put(PARTITION_ID, CONSUMER_ID, result(1, "message-1"));

        try {
            queue.pullNextTask(task -> {
                throw new RuntimeException("Processing is failed");
            });
        } catch (RuntimeException ignored) {
        }

        BlockingQueue<FlushTask> tasks = new ArrayBlockingQueue<>(1);
        pollAsync(queue, tasks::add);

        assertNull(tasks.poll(250, TimeUnit.MILLISECONDS), "Same task issued immediately");

        FlushTask task = tasks.poll(500, TimeUnit.MILLISECONDS);
        assertNotNull(task, "Task not issued repeatedly");

        assertResult("message-1", 1, null, task.getMessages().get(0));
    }

    /**
     * Данные определенной партиции, предназначенные для определенного консьюмера не
     * могут обрабатываться параллельно
     */
    @Test
    public void testOnlyOneConsumerAtATime() throws InterruptedException, BrokenBarrierException {
        MessageDispenser queue = new MessageDispenser(1, 30_000, 15_000, 4);
        queue.registerPartition(PARTITION_ID);

        queue.put(PARTITION_ID, CONSUMER_ID, result(1, "message-1"));
        queue.put(PARTITION_ID, CONSUMER_ID, result(2, "message-2"));

        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch latch1 = new CountDownLatch(1);

        pollAsync(queue, task -> {
            barrier.await();
            latch1.await();
        });

        barrier.await();

        CountDownLatch latch2 = new CountDownLatch(1);

        pollAsync(queue, task -> latch2.countDown());

        assertFalse(latch2.await(500, TimeUnit.MILLISECONDS), "Parallel processing for consumer");

        latch1.countDown();
    }

    /**
     * Данные одной партиции можно параллельно обрабатывать для разных консьюмеров
     */
    @Test
    public void testSeveralConsumersInParallel() throws Exception {
        MessageDispenser queue = new MessageDispenser(1, 30_000, 15_000, 4);
        queue.registerPartition(PARTITION_ID);

        queue.put(PARTITION_ID, "consumer-1", result(1, "message-1-1"));
        queue.put(PARTITION_ID, "consumer-1", result(2));

        queue.put(PARTITION_ID, "consumer-2", result(1, "message-2-1"));

        CyclicBarrier barrier1 = new CyclicBarrier(2);
        CyclicBarrier barrier2 = new CyclicBarrier(3);

        List<FlushTask> tasks = Collections.synchronizedList(new ArrayList<>());

        TrashConsumer<FlushTask> taskConsumer = task -> {
            barrier1.await();
            if (task != null) {
                tasks.add(task);
            }
            barrier2.await();
        };

        pollAsync(queue, taskConsumer);
        pollAsync(queue, taskConsumer);

        barrier2.await(10, TimeUnit.SECONDS);

        assertEquals(2, tasks.size());

        tasks.sort(Comparator.comparing(FlushTask::getConsumerId));

        assertEquals("consumer-1", tasks.get(0).getConsumerId());
        assertEquals("consumer-2", tasks.get(1).getConsumerId());
    }

    /**
     * Очередь отдельно взятого консьюмера доступна для записи во время обработки данных
     */
    @Test
    public void testConsumerDataIsAllowedForQueueingWhileProcessing() throws Exception {
        MessageDispenser queue = new MessageDispenser(1, 30_000, 15_000, 4);
        queue.registerPartition(PARTITION_ID);

        queue.put(PARTITION_ID, CONSUMER_ID, result(1, "message-1"));

        CyclicBarrier barrier1 = new CyclicBarrier(2);
        CountDownLatch latch1 = new CountDownLatch(1);

        pollAsync(queue, task -> {
            barrier1.await();
            latch1.await();
        });

        barrier1.await(10, TimeUnit.SECONDS);

        CountDownLatch latch2 = new CountDownLatch(1);
        BlockingQueue<FlushTask> tasksQueue = new ArrayBlockingQueue<>(1);

        doAsync(() -> {
            latch2.countDown();
            queue.pullNextTask(tasksQueue::add);
        });

        latch2.await(10, TimeUnit.SECONDS);

        queue.put(PARTITION_ID, CONSUMER_ID, result(2));
        latch1.countDown();

        FlushTask task = tasksQueue.poll(500, TimeUnit.MILLISECONDS);
        assertNotNull(task, "No task is issued");

        assertEquals(2, task.getMessages().get(0).getOffset());
    }

    /**
     * В случае если за заданное время в очереди не накопилось сообщений в количестве достаточном
     * для формирования полноценного батча, на обработку отдается то что есть
     */
    @Test
    public void testForceFlushInterval() throws Exception {
        MessageDispenser queue = new MessageDispenser(3, 500, 10_000, 4);
        queue.registerPartition(PARTITION_ID);

        queue.put(PARTITION_ID, CONSUMER_ID, result(1, "message-1"));

        BlockingQueue<FlushTask> tasks = new ArrayBlockingQueue<>(1);
        pollAsync(queue, tasks::add);

        assertNull(tasks.poll(250, TimeUnit.MILLISECONDS));

        FlushTask task = tasks.poll(500, TimeUnit.MILLISECONDS);
        assertNotNull(task);
        assertThat(task.getMessages(), hasSize(1));
    }

    /**
     * Если очередь для консьюмера переполннена вставка в нее блокируется
     */
    @Test
    public void testBlocksOnInsertWhenFull() throws InterruptedException {
        MessageDispenser queue = new MessageDispenser(1, 30_000, 15_000, 2);
        queue.registerPartition(PARTITION_ID);

        queue.put(PARTITION_ID, CONSUMER_ID, result(1, "message-1"));
        queue.put(PARTITION_ID, CONSUMER_ID, result(2, "message-2"));

        CountDownLatch latch = new CountDownLatch(1);

        doAsync(() -> {
            queue.put(PARTITION_ID, CONSUMER_ID, result(3, "message-3"));
            latch.countDown();
        });

        assertFalse(latch.await(500, TimeUnit.MILLISECONDS), "Queue did not block on insert");

        queue.pullNextTask(task -> {
        });

        assertTrue(
                latch.await(250, TimeUnit.MILLISECONDS),
                "Queue did not unblock when first message has been retrieved"
        );
    }

    /**
     * В случае если из сообщений не извлекается данные для обработки их можно поставить в очередь
     * в количестве четырехкратном размеру пачки. При превышении этого предела очредь блокируется.
     */
    @Test
    public void testQueueDoesNotBlockWhenNotEnoughDataParsed() throws InterruptedException {
        MessageDispenser queue = new MessageDispenser(1, 30_000, 15_000, 4);
        queue.registerPartition(PARTITION_ID);

        CountDownLatch latch1 = new CountDownLatch(1);

        doAsync(() -> {
            queue.put(PARTITION_ID, CONSUMER_ID, result(1, "message-1"));
            queue.put(PARTITION_ID, CONSUMER_ID, result(2));
            queue.put(PARTITION_ID, CONSUMER_ID, result(3));
            queue.put(PARTITION_ID, CONSUMER_ID, result(4));
            latch1.countDown();
        });

        assertTrue(latch1.await(250, TimeUnit.MILLISECONDS), "Query insertion is blocked");

        CountDownLatch latch2 = new CountDownLatch(1);

        doAsync(() -> {
            queue.put(PARTITION_ID, CONSUMER_ID, result(5));
            latch2.countDown();
        });

        assertFalse(latch2.await(250, TimeUnit.MILLISECONDS), "Query insertion is not blocked");
    }

    /**
     * Очередь возвращает за раз такое количество сообщений чтобы количество распаршенных
     * данных в них было максимально большим но при этом не превосходило установленный размер пачки
     */
    @Test
    public void testQueueIssuesMessagesCountMatchingBatchSize() throws InterruptedException {
        MessageDispenser queue = new MessageDispenser(5, 30_000, 15_000, 4);
        queue.registerPartition(PARTITION_ID);

        for (int i = 0; i < 12; ++i) {
            String[] data = i % 2 == 0 ? new String[]{"message-" + i} : new String[0];
            queue.put(PARTITION_ID, CONSUMER_ID, result(i, data));
        }

        AtomicReference<FlushTask> taskRef = new AtomicReference<>(null);
        queue.pullNextTask(taskRef::set);

        FlushTask task = taskRef.get();
        assertNotNull(task);
        assertEquals(10, task.getMessages().size());
    }

    /**
     * Очередь выдает по крайней мере одно сообщение даже если количество распаршенных объектов
     * превосходит указанный расмер пачки
     */
    @Test
    public void testQueueIssuesAtLeastOneMessage() throws InterruptedException {
        MessageDispenser queue = new MessageDispenser(1, 30_000, 15_000, 4);
        queue.registerPartition(PARTITION_ID);
        queue.put(PARTITION_ID, CONSUMER_ID, result(1, "message-1", "message-2", "message-3"));

        AtomicReference<FlushTask> taskRef = new AtomicReference<>(null);
        queue.pullNextTask(taskRef::set);

        FlushTask task = taskRef.get();
        assertNotNull(task);
        assertEquals(1, task.getMessages().size());
    }

    /**
     * В случае если вызов метода очистки буфферов партиции происходит в момент
     * обработки пачки одним из её консьюмеров он не завершается пока не завершится
     * текущая обработка.
     * <p>
     * Если в буффере содержатся другие сообщения, они выбрасываются.
     */
    @Test
    public void testClearPartitionBlocksUntilCurrentProcessingIsOver() throws Exception {
        MessageDispenser queue = new MessageDispenser(1, 30_000, 15_000, 4);
        queue.registerPartition(PARTITION_ID);
        queue.put(PARTITION_ID, CONSUMER_ID, result(1, "message-1"));
        queue.put(PARTITION_ID, CONSUMER_ID, result(2, "message-2"));

        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch latch1 = new CountDownLatch(1);

        pollAsync(queue, task -> {
            barrier.await();
            latch1.await();
        });

        barrier.await(10, TimeUnit.SECONDS);

        CountDownLatch latch2 = new CountDownLatch(1);

        doAsync(() -> {
            queue.unregisterPartition(PARTITION_ID);
            latch2.countDown();
        });

        assertFalse(latch2.await(250, TimeUnit.MILLISECONDS), "Method did not block");

        latch1.countDown();

        assertTrue(latch2.await(250, TimeUnit.MILLISECONDS), "Method did not unblock");

        CountDownLatch latch3 = new CountDownLatch(1);

        pollAsync(queue, task -> latch3.countDown());

        assertFalse(latch3.await(250, TimeUnit.MILLISECONDS), "Queue still has messages of partition");
    }
}
