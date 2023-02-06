package ru.yandex.market.mbo.taskqueue;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author yuramalinov
 * @created 12.11.2019
 */
@SuppressWarnings("checkstyle:MagicNumber")
public abstract class TestTaskHandlerBase<T extends TestTaskHandlerBase.Task> implements TaskQueueHandler<T> {
    public static final String RESULT = "Yay! Your result!";
    ConcurrentMap<Integer, String> results = new ConcurrentHashMap<>();
    ConcurrentMap<Integer, Integer> runsToFail = new ConcurrentHashMap<>();
    ConcurrentMap<Integer, CountDownLatch> latches = new ConcurrentHashMap<>();
    ConcurrentMap<Integer, Boolean> isRunning = new ConcurrentHashMap<>();

    RuntimeException failWithException;


    @Override
    public Object handle(TestTaskHandlerBase.Task task, TaskRecord taskRecord) {
        int slot = task.getSlot();
        try {
            isRunning.put(slot, true);
            CountDownLatch latch = latches.get(slot);
            if (latch != null) {
                awaitLatch(latch);
            }
            if (runsToFail.getOrDefault(slot, 0) > 0) {
                runsToFail.put(slot, runsToFail.get(slot) - 1);
                throw failWithException != null ? failWithException : new RuntimeException("I'm failing!");
            }
            results.put(slot, task.getValue());
            return RESULT;
        } finally {
            isRunning.put(slot, false);
        }
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public TaskQueueRetryPolicy getRetryPolicy(TestTaskHandlerBase.Task task) {
        return (attempt, error) -> attempt < task.retries ? Optional.of(10L) : Optional.empty();
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static class Task implements TaskQueueTask {
        private final int slot;
        private final int retries;
        private final String value;

        public Task(int slot, String value) {
            this(slot, value, 0);
        }

        @JsonCreator
        public Task(@JsonProperty("slot") int slot,
                    @JsonProperty("value") String value,
                    @JsonProperty("retries") int retries) {
            this.slot = slot;
            this.retries = retries;
            this.value = value;
        }

        public int getSlot() {
            return slot;
        }

        public String getValue() {
            return value;
        }

        public int getRetries() {
            return retries;
        }

        @Override
        public String toString() {
            return "Task{" +
                    "slot=" + slot +
                    ", retries=" + retries +
                    ", value='" + value + '\'' +
                    '}';
        }
    }
}
