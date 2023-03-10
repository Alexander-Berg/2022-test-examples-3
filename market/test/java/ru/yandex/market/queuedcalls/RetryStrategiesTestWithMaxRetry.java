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

public class RetryStrategiesTestWithMaxRetry extends AbstractQueuedCallTest {

    private boolean shouldThrowRuntimeException;
    private boolean shouldFinishWithErrorResult;
    private RetryStrategy retryStrategy;
    private final AtomicInteger callCounter = new AtomicInteger(0);

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

        @Override
        public int maxAgeInDays() {
            return 1;
        }
    };
    private final Consumer<Consumer<QueuedCall>> singleQCChecker = consumer -> {
        Optional<QueuedCall> callOptional = qcService.findQueuedCalls(FIRST, 1L).stream().findFirst();
        if (callOptional.isPresent()) {
            consumer.accept(callOptional.get());
        } else {
            throw new RuntimeException();
        }
    };

    @Test
    public void testProgressiveRetryStrategy() {

        // ?????? ?????????????? ???????????? ?????????? ?????????????????????????? ???? 10, 20 ??????
        retryStrategy = RetryStrategies.IN_10_MINUTES_PROGRESSIVELY;
        shouldThrowRuntimeException = false;
        shouldFinishWithErrorResult = true;
        Instant startTime = clock.instant();
        setFixedTime(startTime);

        createQueuedCall(FIRST, 1L);
        checkSingleQC(false, 0, null);
        checkCallCounter(0);

        // ?????????????????? ??????????, ??????????????????, ?????? QC ???????????????? ?? ?????????????????? ???? 10 ??????????
        executeBatch();
        checkSingleQC(false, 1, startTime.plus(10, ChronoUnit.MINUTES));
        checkCallCounter(1);

        // ?????? ?????????????????? ?????????????? QC ???? ????????????????????, ?????? ?????? ?????? ?????????????? ???? 10 ??????????
        executeBatch();
        checkSingleQC(false, 1, startTime.plus(10, ChronoUnit.MINUTES));
        checkCallCounter(1);

        // ???? 12 ???????????? QC ???????????????????? ?? ?????????????????????????? ???? 20 ??????????
        setFixedTime(startTime.plus(12, ChronoUnit.MINUTES));
        executeBatch();
        checkSingleQC(false, 2, startTime.plus(32, ChronoUnit.MINUTES));
        checkCallCounter(2);

        // ???? 25 ???????????? QC ???? ????????????????????
        setFixedTime(startTime.plus(25, ChronoUnit.MINUTES));
        executeBatch();
        checkSingleQC(false, 2, startTime.plus(32, ChronoUnit.MINUTES));
        checkCallCounter(2);

        // ?????????????????????? ?????????????????????? maxRetryDays
        setFixedTime(startTime.plus(1, ChronoUnit.DAYS));
        executeBatch();
        checkCallCounter(3);
    }

    // ?????????????????? ???? ?????????????????? - IN_10_MINUTES_FIXED (QC ?????????????????????????? ???? 10 ?????????? ?????? ???????????? ???????????????????? ????????????)
    @Test
    public void testDefaultProcessorRetryStrategy() {
        shouldThrowRuntimeException = true;
        shouldFinishWithErrorResult = false;
        Instant startTime = clock.instant();
        setFixedTime(startTime);

        createQueuedCall(FIRST, 1L);
        checkSingleQC(false, 0, null);
        checkCallCounter(0);

        // ?????????????????? ??????????, ??????????????????, ?????? QC ???????????????? ?? ?????????????????? ???? 10 ??????????
        executeBatch();
        checkSingleQC(false, 1, startTime.plus(10, ChronoUnit.MINUTES));
        checkCallCounter(1);

        // ?????? ?????????????????? ?????????????? QC ???? ????????????????????, ?????? ?????? ?????? ?????????????? ???? 10 ??????????
        executeBatch();
        checkSingleQC(false, 1, startTime.plus(10, ChronoUnit.MINUTES));
        checkCallCounter(1);

        // ???? 12 ???????????? QC ???????????????????? ?? ?????????? ?????????????????????????? ???? 10 ??????????
        setFixedTime(startTime.plus(12, ChronoUnit.MINUTES));
        executeBatch();
        checkSingleQC(false, 2, startTime.plus(22, ChronoUnit.MINUTES));
        checkCallCounter(2);

        // ???? 17 ???????????? QC ???? ????????????????????
        setFixedTime(startTime.plus(17, ChronoUnit.MINUTES));
        executeBatch();
        checkSingleQC(false, 2, startTime.plus(22, ChronoUnit.MINUTES));
        checkCallCounter(2);

        // ?????????????????????? ?????????????????????? maxRetryDays
        shouldThrowRuntimeException = false;
        shouldFinishWithErrorResult = false;
        setFixedTime(startTime.plus(1, ChronoUnit.DAYS));
        executeBatch();
        checkCallCounter(3);
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
}
