package ru.yandex.market.logistics.management.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.exception.TooManyRequestsException;
import ru.yandex.market.logistics.management.util.tvm.TvmInfoExtractor;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class InFlightRequestLimiterTest extends AbstractTest {

    private static final String LIMITER_NAME = "test request limiter";
    private static final int MOCK_VALUE = new Random().nextInt();

    private InFlightRequestLimiter requestLimiter;
    private CountDownLatch readySignal;
    private CountDownLatch doneSignal;

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("testSource")
    void wrapTest(int limit) throws Exception {
        requestLimiter = new InFlightRequestLimiter(
            LIMITER_NAME,
            new SimpleMeterRegistry(),
            limit,
            true,
            new HashSet<>(),
            () -> null
        );
        CompletableFuture<?>[] results = exceedTheLimit(limit);

        assertThatHasFailedTooManyRequestsException(results, limit);
    }

    @Test
    void testPrioritizedRequestWithReachedLimit() throws InterruptedException {
        int limit = 5;
        updateRequestLimiter(limit, Set.of(123L), () -> 123L);

        CompletableFuture<?>[] results = exceedTheLimit(limit);

        assertThat(CompletableFuture.allOf(results))
            .isCompleted();
    }

    @Test
    void testNonPrioritizedRequestWithReachedLimit() throws InterruptedException {
        int limit = 5;
        updateRequestLimiter(limit, Set.of(124L), () -> 122L);

        CompletableFuture<?>[] results = exceedTheLimit(limit);

        assertThatHasFailedTooManyRequestsException(results, limit);
    }

    @Test
    void testConstructionWithNullTvmIds() {
        List<Long> ids = new ArrayList<>();
        ids.add(null);
        assertThatThrownBy(() -> updateRequestLimiter(1, ids, () -> null))
            .isInstanceOf(NullPointerException.class);
    }

    private void assertThatHasFailedTooManyRequestsException(CompletableFuture<?>[] results, int limit) {
        assertThat(CompletableFuture.allOf(results))
            .hasFailedWithThrowableThat()
            .isInstanceOf(TooManyRequestsException.class)
            .hasMessage("429 TOO_MANY_REQUESTS \"Unable to process request. Limit of " + limit +
                " in-flight requests reached for limiter " + LIMITER_NAME + ".\"");
    }

    private void updateRequestLimiter(int limit, Collection<Long> priorityTvmIds, TvmInfoExtractor tvmInfoExtractor) {
        requestLimiter = new InFlightRequestLimiter(
            LIMITER_NAME,
            new SimpleMeterRegistry(),
            limit,
            true,
            priorityTvmIds,
            tvmInfoExtractor
        );
    }

    private CompletableFuture<?>[] exceedTheLimit(int limit) throws InterruptedException {
        readySignal = new CountDownLatch(limit + 1);
        doneSignal = new CountDownLatch(1);
        ExecutorService executor = Executors.newCachedThreadPool();

        CompletableFuture<?>[] results = IntStream.range(0, limit + 1)
            .mapToObj(i -> CompletableFuture.supplyAsync(this::wrapRequestLimiter, executor))
            .toArray(CompletableFuture[]::new);

        // ожидаем "успешные" потоки, пока они не дойдут до исполнения supplier, переданного в метод wrap,
        // и "неуспешные" потоки, пока они не выбросят TooManyRequestsException
        readySignal.await();

        // продолжаем выполнение "успешных" потоков
        doneSignal.countDown();
        // дополнительно убеждаемся, что все потоки завершили свое выполнение
        executor.shutdown();
        assertThat(executor.awaitTermination(3, TimeUnit.SECONDS)).isTrue();
        return results;
    }

    private Integer wrapRequestLimiter() {
        try {
            return requestLimiter.wrap(() -> {
                readySignal.countDown();
                doneSignal.await();
                return MOCK_VALUE;
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            readySignal.countDown();
        }
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
            Arguments.of(1),
            Arguments.of(2),
            Arguments.of(3),
            Arguments.of(5),
            Arguments.of(8),
            Arguments.of(13),
            Arguments.of(21)
        );
    }
}
