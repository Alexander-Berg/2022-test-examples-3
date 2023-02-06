package ru.yandex.market.queuedcalls;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.queuedcalls.retry.RetryStrategies;
import ru.yandex.market.queuedcalls.retry.RetryStrategy;

import static ru.yandex.market.queuedcalls.model.TestQCType.FIRST;

public class RetryStrategiesTest extends AbstractQueuedCallTest {

    private final AtomicInteger callCounter = new AtomicInteger(0);
    private final Consumer<Consumer<QueuedCall>> singleQCChecker = consumer -> {
        Optional<QueuedCall> callOptional = qcService.findQueuedCalls(FIRST, 1L).stream().findFirst();
        if (callOptional.isPresent()) {
            consumer.accept(callOptional.get());
        } else {
            throw new RuntimeException();
        }
    };
    private boolean shouldThrowRuntimeException;
    private boolean shouldFinishWithErrorResult;
    private RetryStrategy retryStrategy;
    private final QueuedCallProcessor processor = new SimpleQueuedCallProcessor() {
        @Nullable
        @Override
        public ExecutionResult process(QueuedCallExecution execution) {
            callCounter.incrementAndGet();
            if (shouldThrowRuntimeException) {
                throw new RuntimeException();
            }
            if (shouldFinishWithErrorResult) {
                return ExecutionResult.errorResult("error description", retryStrategy);
            }
            return null;
        }

        @Nonnull
        @Override
        public QueuedCallType getSupportedType() {
            return FIRST;
        }

        @Nonnull
        @Override
        public RetryStrategy getDefaultRetryStrategy() {
            return retryStrategy != null ? retryStrategy : super.getDefaultRetryStrategy();
        }
    };

    @Test
    public void testProgressiveRetryStrategy() {

        // При ошибках вызовы будут откладываться на 10, 20, 40, 60, 60, 60... мин (60 мин - максимум)
        retryStrategy = RetryStrategies.IN_10_MINUTES_PROGRESSIVELY;
        shouldThrowRuntimeException = false;
        shouldFinishWithErrorResult = true;
        Instant startTime = clock.instant();
        setFixedTime(startTime);

        createQueuedCall(FIRST, 1L);
        checkSingleQC(false, 0, null);
        checkCallCounter(0);

        // Запускаем сразу, проверяем, что QC вызвался и отложился на 10 минут
        executeBatch();
        checkSingleQC(false, 1, startTime.plus(10, ChronoUnit.MINUTES));
        checkCallCounter(1);

        // При повторном запуске QC не вызывается, так как был отложен на 10 минут
        executeBatch();
        checkSingleQC(false, 1, startTime.plus(10, ChronoUnit.MINUTES));
        checkCallCounter(1);

        // На 12 минуте QC вызывается и откладывается на 20 минут
        setFixedTime(startTime.plus(12, ChronoUnit.MINUTES));
        executeBatch();
        checkSingleQC(false, 2, startTime.plus(32, ChronoUnit.MINUTES));
        checkCallCounter(2);

        // На 25 минуте QC не вызывается
        setFixedTime(startTime.plus(25, ChronoUnit.MINUTES));
        executeBatch();
        checkSingleQC(false, 2, startTime.plus(32, ChronoUnit.MINUTES));
        checkCallCounter(2);

        // На 35 минуте QC вызывается и откладывается на 40 минут
        setFixedTime(startTime.plus(35, ChronoUnit.MINUTES));
        executeBatch();
        checkSingleQC(false, 3, startTime.plus(75, ChronoUnit.MINUTES));
        checkCallCounter(3);

        // На 80 минуте QC вызывается и откладывается на 60 минут
        setFixedTime(startTime.plus(80, ChronoUnit.MINUTES));
        executeBatch();
        checkSingleQC(false, 4, startTime.plus(140, ChronoUnit.MINUTES));
        checkCallCounter(4);

        // На 150 минуте QC вызывается и откладывается опять на 60 минут (максимум)
        setFixedTime(startTime.plus(150, ChronoUnit.MINUTES));
        executeBatch();
        checkSingleQC(false, 5, startTime.plus(210, ChronoUnit.MINUTES));
        checkCallCounter(5);

        // На 220 минуте QC успешно обрабатывается
        shouldThrowRuntimeException = false;
        shouldFinishWithErrorResult = false;
        setFixedTime(startTime.plus(220, ChronoUnit.MINUTES));
        executeBatch();
        checkCallCounter(6);
        checkCompleted();
    }

    // Стратегия по умолчанию - IN_10_MINUTES_FIXED (QC откладывается на 10 минут при каждом неуспешном вызове)
    @Test
    public void testDefaultProcessorRetryStrategy() {
        shouldThrowRuntimeException = true;
        shouldFinishWithErrorResult = false;
        Instant startTime = clock.instant();
        setFixedTime(startTime);

        createQueuedCall(FIRST, 1L);
        checkSingleQC(false, 0, null);
        checkCallCounter(0);

        // Запускаем сразу, проверяем, что QC вызвался и отложился на 10 минут
        executeBatch();
        checkSingleQC(false, 1, startTime.plus(10, ChronoUnit.MINUTES));
        checkCallCounter(1);

        // При повторном запуске QC не вызывается, так как был отложен на 10 минут
        executeBatch();
        checkSingleQC(false, 1, startTime.plus(10, ChronoUnit.MINUTES));
        checkCallCounter(1);

        // На 12 минуте QC вызывается и опять откладывается на 10 минут
        setFixedTime(startTime.plus(12, ChronoUnit.MINUTES));
        executeBatch();
        checkSingleQC(false, 2, startTime.plus(22, ChronoUnit.MINUTES));
        checkCallCounter(2);

        // На 17 минуте QC не вызывается
        setFixedTime(startTime.plus(17, ChronoUnit.MINUTES));
        executeBatch();
        checkSingleQC(false, 2, startTime.plus(22, ChronoUnit.MINUTES));
        checkCallCounter(2);

        // На 23 минуте QC успешно обрабатывается
        shouldThrowRuntimeException = false;
        shouldFinishWithErrorResult = false;
        setFixedTime(startTime.plus(23, ChronoUnit.MINUTES));
        executeBatch();
        checkCallCounter(3);
        checkCompleted();
    }

    private void checkCallCounter(int expected) {
        Assertions.assertEquals(expected, callCounter.get());
    }

    private void executeBatch() {
        qcService.executeQueuedCallBatch(processor, () -> false);
    }

    private void checkSingleQC(Boolean isProcessed, Integer triesCount, Instant nextTryAt) {
        singleQCChecker.accept(call -> {
            if (isProcessed != null) {
                Assertions.assertEquals(isProcessed, call.isProcessed());
            }
            if (triesCount != null) {
                Assertions.assertEquals(triesCount, call.getTriesCount());
            }
            if (nextTryAt != null) {
                Assertions.assertEquals(nextTryAt, call.getNextTryAt());
            }
        });
    }

    private void checkCompleted() {
        Assertions.assertEquals(0, (long) qcService.findQueuedCalls(FIRST, 1L).size());
    }
}
