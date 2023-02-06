package ru.yandex.market.crm.triggers.services.bpm.correlation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;

import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.crm.util.Exceptions.TrashRunnable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author apershukov
 */
public class SemiOrderedQueueTest {

    private static MessageContainer item(Uid uid) {
        var message = new UidBpmMessage(
                MessageTypes.NEW_QUESTION_ON_REVIEWED_MODEL,
                uid,
                Map.of(),
                Map.of()
        );
        return new MessageContainer(ITEM_ID.getAndIncrement(), message);
    }

    private static MessageContainer item() {
        return item(Uid.asPuid(111L));
    }

    private static void doInThread(TrashRunnable runnable) {
        new Thread(() -> {
                try {
                    runnable.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }).start();
    }

    private static final AtomicLong ITEM_ID = new AtomicLong(1);

    private SemiOrderedQueue queue = new SemiOrderedQueue();

    /**
     * Сообщения для одного пользователя достаются из очереди в порядке FIFO
     */
    @Test
    public void testPutAndTake() throws Exception {
        List<MessageContainer> items = Stream.generate(SemiOrderedQueueTest::item)
                .limit(3)
                .collect(Collectors.toList());

        for (MessageContainer message : items) {
            queue.put(message);
        }

        BlockingQueue<MessageContainer> consumed = new ArrayBlockingQueue<>(3);

        doInThread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    queue.take(consumed::add);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        List<Long> ids = new ArrayList<>();
        MessageContainer item;
        while ((item = consumed.poll(1, TimeUnit.SECONDS)) != null) {
            ids.add(item.getId());
        }

        List<Long> expectedIds = items.stream()
                .map(MessageContainer::getId)
                .collect(Collectors.toList());

        assertEquals(expectedIds, ids);
    }

    /**
     * Сообщения для одного пользователя обрабатываются строго последовательно.
     * Пока обработка предыдущего сообщения не закончилась следующее не достается.
     */
    @Test
    public void testStrictOrderForSameUserMessages() throws Exception {
        queue.put(item());
        queue.put(item());

        CyclicBarrier barrier = new CyclicBarrier(3);
        CountDownLatch latch1 = new CountDownLatch(1);

        doInThread(() -> queue.take(x -> {
            barrier.await(5, TimeUnit.SECONDS);
            latch1.await(10, TimeUnit.SECONDS);
        }));

        CountDownLatch latch2 = new CountDownLatch(1);

        doInThread(() -> {
            barrier.await(5, TimeUnit.SECONDS);
            queue.take(x -> latch2.countDown());
        });

        barrier.await(5, TimeUnit.SECONDS);

        assertFalse("Second message processed", latch2.await(1, TimeUnit.SECONDS));

        latch1.countDown();

        assertTrue("Second message not processed", latch2.await(1, TimeUnit.SECONDS));
    }

    /**
     * При превышении максимальной вместительности очереди вставка в нее блокируется
     */
    @Test
    public void testBlocksIfMaxMessageCapacityIsReached() throws Exception {
        SemiOrderedQueue queue = new SemiOrderedQueue(15);

        List<MessageContainer> items = Stream.generate(() -> item(Uid.asPuid(RandomUtils.nextLong(1, 1000))))
                .limit(16)
                .collect(Collectors.toList());

        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);

        doInThread(() -> {
            for (int i = 0; i < 15; ++i) {
                queue.put(items.get(i));
            }

            latch1.countDown();

            queue.put(items.get(15));

            latch2.countDown();
        });

        assertTrue(
                "Insertion is blocked before max capacity is reached",
                latch1.await(5, TimeUnit.SECONDS)
        );

        assertFalse(
                "Insertion is not blocked after max capacity is reached",
                latch2.await(1, TimeUnit.SECONDS)
        );
    }
}
