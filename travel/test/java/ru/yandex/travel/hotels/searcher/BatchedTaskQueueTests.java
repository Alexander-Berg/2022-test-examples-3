package ru.yandex.travel.hotels.searcher;

import org.junit.Test;
import ru.yandex.travel.commons.proto.ECurrency;
import ru.yandex.travel.hotels.proto.*;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class BatchedTaskQueueTests {
    private TSearchOffersReq.Builder getDefaultReqBuilder(String originalId) {
        return TSearchOffersReq.newBuilder()
                .setId("1")
                .setHotelId(THotelId.newBuilder().setPartnerId(EPartnerId.PI_BOOKING).setOriginalId(originalId))
                .setOccupancy("2")
                .setCheckInDate("2019-01-01")
                .setCheckOutDate("2019-01-10")
                .setCurrency(ECurrency.C_RUB)
                .setRequestClass(ERequestClass.RC_INTERACTIVE);
    }

    @Test
    public void testTwoBatches() {
        BatchedTaskQueue queue = new BatchedTaskQueue();
        IntStream.range(1, 101)
                .mapToObj(i -> new Task(getDefaultReqBuilder(String.valueOf(i)).build(), true))
                .forEach(queue::offer);
        assertEquals(70, queue.getBatch(70).getTasks().size());
        assertEquals(30, queue.getBatch(70).getTasks().size());
        assertNull(queue.getBatch(70));
    }

    @Test
    public void testNBatches() {
        BatchedTaskQueue queue = new BatchedTaskQueue();
        IntStream.range(1, 101)
                .mapToObj(i -> new Task(getDefaultReqBuilder(String.valueOf(i)).build(), true))
                .forEach(queue::offer);
        int n = 0;
        while (queue.getBatch(1) != null) {
            ++n;
        }
        assertEquals(100, n);
    }

    @Test
    public void testTakesOldestGroupFirstOneByOne() throws InterruptedException {
        BatchedTaskQueue queue = new BatchedTaskQueue();
        Task taskFromGroupA = new Task(getDefaultReqBuilder("1").build(), true, 1);
        Task taskFromGroupB = new Task(getDefaultReqBuilder("2").setCheckOutDate("other").build(), true, 2);
        Task newerTaskFromGroupA = new Task(getDefaultReqBuilder("3").build(), true, 3);
        queue.offer(taskFromGroupA);
        queue.offer(taskFromGroupB);
        queue.offer(newerTaskFromGroupA);
        assertEquals(taskFromGroupA, queue.getBatch(1).getTasks().get(0));
        assertEquals(taskFromGroupB, queue.getBatch(1).getTasks().get(0));
        assertEquals(newerTaskFromGroupA, queue.getBatch(1).getTasks().get(0));
    }

    @Test
    public void testTakesOldestGroupBatches() throws InterruptedException {
        BatchedTaskQueue queue = new BatchedTaskQueue();
        Task taskFromGroupA = new Task(getDefaultReqBuilder("1").build(), true, 1);
        Task taskFromGroupB = new Task(getDefaultReqBuilder("2").setCheckOutDate("other").build(), true, 2);
        Task newerTaskFromGroupA = new Task(getDefaultReqBuilder("3").build(), true, 3);
        queue.offer(taskFromGroupA);
        queue.offer(taskFromGroupB);
        queue.offer(newerTaskFromGroupA);
        List<Task> batch1 = queue.getBatch(10).getTasks();
        List<Task> batch2 = queue.getBatch(10).getTasks();
        assertEquals(2, batch1.size());
        assertEquals(1, batch2.size());
        assertEquals(taskFromGroupA, batch1.get(0));
        assertEquals(newerTaskFromGroupA, batch1.get(1));
        assertEquals(taskFromGroupB, batch2.get(0));
    }

    @Test
    public void testLimitExceeded() {
        BatchedTaskQueue queue = new BatchedTaskQueue(3);
        assertTrue(queue.offer(new Task(getDefaultReqBuilder("1").build(), true)));
        assertTrue(queue.offer(new Task(getDefaultReqBuilder("2").build(), true)));
        assertTrue(queue.offer(new Task(getDefaultReqBuilder("3").build(), true)));
        assertFalse(queue.offer(new Task(getDefaultReqBuilder("4").build(), true)));
    }

    @Test
    public void testSize() {
        BatchedTaskQueue queue = new BatchedTaskQueue(3);
        assertEquals(0, queue.getSize());
        queue.offer(new Task(getDefaultReqBuilder("1").build(), true));
        queue.offer(new Task(getDefaultReqBuilder("2").build(), true));
        queue.offer(new Task(getDefaultReqBuilder("3").setCheckInDate("AnotherDate").build(), true));
        assertEquals(3, queue.getSize());
        assertEquals(2, queue.getBatch(100).getTasks().size());
        assertEquals(1, queue.getSize());
    }
}
