package ru.yandex.market.mbo.cms.core.utils.executors;

import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

public class BlockingPriorityExecutorTest {
    public static final int POOL_SIZE = 1;
    public static final int QUEUE_SIZE = 5;
    private BlockingPriorityExecutor blockingPriorityExecutor = new BlockingPriorityExecutor(POOL_SIZE, QUEUE_SIZE);

    @SuppressWarnings("magicnumber")
    @Test
    public void testBlocking() {
        AtomicBoolean spamThreadStopFlag = new AtomicBoolean(false);
        AtomicBoolean indicatorThreadStopFlag = new AtomicBoolean(false);
        //Заполняем потоки и очередь целиком
        spamTasks(spamThreadStopFlag, POOL_SIZE + QUEUE_SIZE);
        Thread indicatorThread = new Thread(() -> {
            spamTasks(indicatorThreadStopFlag, 1);
        });
        indicatorThread.start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Thread interrupted");
        }
        Assert.assertEquals(indicatorThread.getState(), Thread.State.WAITING);
        indicatorThreadStopFlag.set(true);
        spamThreadStopFlag.set(true);
    }

    private void spamTasks(AtomicBoolean threadStopFlag, int num) {
        for (int i = 0; i < num; i++) {
            blockingPriorityExecutor.submit(makeTask(threadStopFlag, "" + i, 0));
        }
    }

    @SuppressWarnings("magicnumber")
    @Test
    public void testPriority() {
        AtomicBoolean threadStopFlag = new AtomicBoolean(false);

        //Заполняем потоки чтобы активировать очередь
        spamTasks(threadStopFlag, POOL_SIZE);
        Task task1 = makeTask(threadStopFlag, "t1 - p2", 2);
        Task task2 = makeTask(threadStopFlag, "t2 - p1", 1);
        Task task3 = makeTask(threadStopFlag, "t3 - p5", 5);
        Task task4 = makeTask(threadStopFlag, "t4 - p3", 3);
        Task task5 = makeTask(threadStopFlag, "t5 - p4", 4);

        Future f1 = blockingPriorityExecutor.submit(task1);
        Future f2 = blockingPriorityExecutor.submit(task2);
        Future f3 = blockingPriorityExecutor.submit(task3);
        Future f4 = blockingPriorityExecutor.submit(task4);
        Future f5 = blockingPriorityExecutor.submit(task5);

        Queue queue = blockingPriorityExecutor.getQueue();

        //Больший приоритет выполняется быстрее
        Assert.assertEquals(f3, queue.poll());
        Assert.assertEquals(f5, queue.poll());
        Assert.assertEquals(f4, queue.poll());
        Assert.assertEquals(f1, queue.poll());
        Assert.assertEquals(f2, queue.poll());

        threadStopFlag.set(true);
    }

    private Task makeTask(AtomicBoolean threadStopFlag, String text, int priority) {
        return new Task(threadStopFlag, text, priority);
    }

    private static class Task extends PriorityCallable<String> {
        AtomicBoolean threadStopFlag;
        String name;

        private Task(AtomicBoolean threadStopFlag, String name, int priority) {
            super(() -> {
                while (!threadStopFlag.get()) {
                    int a; //do nothing
                }
                return name;
            }, priority);
            this.threadStopFlag = threadStopFlag;
        }

        public String getName() {
            return name;
        }
    }
}
