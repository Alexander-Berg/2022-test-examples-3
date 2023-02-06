package ru.yandex.travel.task_processor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import ru.yandex.travel.testing.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
public class PausableTerminationSemaphoreWithBucketsTest {
    public static final int BUCKET_1 = 1;
    public static final int BUCKET_2 = 2;

    @Test
    public void testPermitsOneBucket() {
        var semaphore = testSemaphore(Map.of(BUCKET_1, 2));

        assertThat(semaphore.getAvailablePermits()).isEqualTo(2);
        assertThat(semaphore.getPermits()).isEqualTo(0);
        assertThat(semaphore.getMaxPermits()).isEqualTo(2);

        semaphore.resume();

        assertThat(semaphore.acquire(BUCKET_1)).isTrue();
        assertThat(semaphore.getAvailablePermits()).isEqualTo(1);
        assertThat(semaphore.getPermits()).isEqualTo(1);
        assertThat(semaphore.getMaxPermits()).isEqualTo(2);

        assertThat(semaphore.acquire(BUCKET_1)).isTrue();
        assertThat(semaphore.getAvailablePermits()).isEqualTo(0);
        assertThat(semaphore.getPermits()).isEqualTo(2);
        assertThat(semaphore.getMaxPermits()).isEqualTo(2);

        assertThat(semaphore.acquire(BUCKET_1)).isFalse();
        assertThat(semaphore.getAvailablePermits()).isEqualTo(0);
        assertThat(semaphore.getPermits()).isEqualTo(2);
        assertThat(semaphore.getMaxPermits()).isEqualTo(2);

        semaphore.release(BUCKET_1);
        assertThat(semaphore.getAvailablePermits()).isEqualTo(1);
        assertThat(semaphore.getPermits()).isEqualTo(1);
        assertThat(semaphore.getMaxPermits()).isEqualTo(2);

        semaphore.release(BUCKET_1);
        assertThat(semaphore.getAvailablePermits()).isEqualTo(2);
        assertThat(semaphore.getPermits()).isEqualTo(0);
        assertThat(semaphore.getMaxPermits()).isEqualTo(2);
    }

    @Test
    public void testPermitsMultipleBuckets() {
        var semaphore = testSemaphore(Map.of(BUCKET_1, 1, BUCKET_2, 2));

        assertThat(semaphore.getAvailablePermits()).isEqualTo(3);
        assertThat(semaphore.getPermits()).isEqualTo(0);
        assertThat(semaphore.getMaxPermits()).isEqualTo(3);

        semaphore.resume();

        assertThat(semaphore.acquire(BUCKET_1)).isTrue();
        assertThat(semaphore.acquire(BUCKET_1)).isFalse();
        assertThat(semaphore.getAvailablePermits()).isEqualTo(2);
        assertThat(semaphore.getPermits()).isEqualTo(1);
        assertThat(semaphore.getMaxPermits()).isEqualTo(3);

        assertThat(semaphore.acquire(BUCKET_2)).isTrue();
        assertThat(semaphore.acquire(BUCKET_2)).isTrue();
        assertThat(semaphore.acquire(BUCKET_2)).isFalse();
        assertThat(semaphore.getAvailablePermits()).isEqualTo(0);
        assertThat(semaphore.getPermits()).isEqualTo(3);
        assertThat(semaphore.getMaxPermits()).isEqualTo(3);

        assertThat(semaphore.acquire(BUCKET_1)).isFalse();
        semaphore.release(BUCKET_1);
        assertThat(semaphore.acquire(BUCKET_2)).isFalse();
        semaphore.release(BUCKET_2);
        semaphore.release(BUCKET_2);
        assertThat(semaphore.getAvailablePermits()).isEqualTo(3);
        assertThat(semaphore.getPermits()).isEqualTo(0);
        assertThat(semaphore.getMaxPermits()).isEqualTo(3);
    }

    @Test
    public void testPermitsIllegalUsage() {
        var semaphore = testSemaphore(Map.of(BUCKET_1, 1));

        assertThatThrownBy(() -> semaphore.acquire(BUCKET_2))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No such bucket");

        assertThatThrownBy(() -> semaphore.release(BUCKET_1))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Trying to release semaphore that was not acquired");
    }

    @Test
    public void testStates() throws Exception {
        var semaphore = testSemaphore(Map.of(BUCKET_1, 10));

        assertThat(semaphore.isActive()).isFalse();
        assertThat(semaphore.acquire(BUCKET_1)).isFalse();

        semaphore.resume();
        assertThat(semaphore.isActive()).isTrue();
        assertThat(semaphore.acquire(BUCKET_1)).isTrue();
        semaphore.release(BUCKET_1);

        semaphore.pause();
        assertThat(semaphore.isActive()).isFalse();
        assertThat(semaphore.acquire(BUCKET_1)).isFalse();

        semaphore.shutdown();
        assertThat(semaphore.isActive()).isFalse();
        assertThat(semaphore.acquire(BUCKET_1)).isFalse();
    }

    /**
     * The same logic applies to shutdown + awaitTermination.
     */
    @Test
    public void testBlockingPause() throws Exception {
        var semaphore = testSemaphore(Map.of(BUCKET_1, 10));

        semaphore.resume();
        assertThat(semaphore.isActive()).isTrue();
        assertThat(semaphore.acquire(BUCKET_1)).isTrue();
        assertThat(semaphore.getPermits()).isEqualTo(1);

        var releaseFuture = runAsync(() -> {
            TestUtils.waitForState("Waiting for call: semaphore.pause()", Duration.ofSeconds(10),
                    () -> !semaphore.isActive());
            // will fail the release operation below if something goes wrong with the stopping process
            Preconditions.checkState(!semaphore.isActive(), "Not paused yet");
            Preconditions.checkState(semaphore.getPermits() == 1, "Unexpected permits: %s", semaphore.getPermits());
            semaphore.release(BUCKET_1);
        });
        semaphore.pause();
        // should be successfully completed
        releaseFuture.get();

        assertThat(semaphore.isActive()).isFalse();
        assertThat(semaphore.getPermits()).isEqualTo(0);
    }

    private Future<Void> runAsync(RunnableWithException action) {
        var executor = Executors.newSingleThreadExecutor();
        return executor.submit(() -> {
            try {
                action.run();
            } catch (Exception e) {
                log.error("Async operation has failed", e);
                throw e;
            } finally {
                executor.shutdown();
            }
            return null;
        });
    }

    private PausableTerminationSemaphoreWithBuckets testSemaphore(Map<Integer, Integer> bucketPermits) {
        return new PausableTerminationSemaphoreWithBuckets("test_semaphore", bucketPermits);
    }

    private interface RunnableWithException {
        void run() throws Exception;
    }
}
