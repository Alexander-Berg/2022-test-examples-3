package ru.yandex.market.checkout.checkouter.tasks.queuedcalls;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.queuedcalls.CancellationToken;
import ru.yandex.market.queuedcalls.ExecutionResult;
import ru.yandex.market.queuedcalls.QueuedCall;
import ru.yandex.market.queuedcalls.QueuedCallObjectType;
import ru.yandex.market.queuedcalls.QueuedCallProcessor;
import ru.yandex.market.queuedcalls.QueuedCallService;
import ru.yandex.market.queuedcalls.QueuedCallSettingsService;
import ru.yandex.market.queuedcalls.QueuedCallType;
import ru.yandex.market.queuedcalls.impl.QCTypeCancellationToken;
import ru.yandex.market.queuedcalls.impl.QueuedCallDao;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.queuedcalls.impl.ProcessorNameHolder.getProcessorName;

public class AbstractQueuedCallTestBase extends AbstractWebTestBase {

    @Autowired
    protected QueuedCallService queuedCallService;
    @Autowired
    protected QueuedCallDao queuedCallDao;
    @Autowired
    protected QueuedCallSettingsService queuedCallSettingsService;

    private final HashMap<QueuedCallType, CancellationToken> tokensByType = new HashMap<>();

    @AfterEach
    public void tearDown() {
        queuedCallSettingsService.resetAllSettingsToDefault();
    }

    protected void executeQueuedCalls(QueuedCallType type, Function objectIdProcessor) {
        queuedCallService.executeQueuedCallBatch(
                new SimpleQueuedCallProcessor() {
                    @Override
                    public ExecutionResult process(QueuedCallProcessor.QueuedCallExecution execution) {
                        return objectIdProcessor.apply(execution.getObjId());
                    }

                    @Nonnull
                    @Override
                    public QueuedCallType getSupportedType() {
                        return type;
                    }
                },
                tokensByType.computeIfAbsent(type, t -> new QCTypeCancellationToken(t, queuedCallSettingsService))
        );
    }

    protected void checkCallInQueue(QueuedCallType type,
                                    long objectId,
                                    Matcher<Instant> expectedNextTryAt,
                                    int triesCount,
                                    Matcher<String> lastExecutionMessage) {
        Collection<QueuedCall> queuedCalls = queuedCallService.findQueuedCalls(type, objectId);
        assertEquals(1, queuedCalls.size());

        checkQueuedCallNotCompleted(queuedCalls.iterator().next(),
                expectedNextTryAt,
                type,
                objectId,
                false,
                triesCount,
                lastExecutionMessage);
    }

    protected void checkCallInQueue(QueuedCallType type, long objectId, Matcher<Instant> expectedNextTryAt) {
        checkCallInQueue(type, objectId, expectedNextTryAt, 0, nullValue(String.class));
    }

    protected void checkCallCompletedAfterExecution(QueuedCallType type, long objectId, Matcher<Instant> processedAt) {
        checkCallCompleted(type, objectId, processedAt, 1, nullValue(String.class));
    }

    protected void checkCallCompleted(QueuedCallType type, long objectId, Matcher<Instant> processedAt,
                                      int triesCount, Matcher<String> lastExecutionMessage) {
        Collection<QueuedCall> queuedCalls = queuedCallDao.allCallsForObject(Collections.singleton(type), objectId);
        assertEquals(1, queuedCalls.size());

        checkQueuedCallCompleted(queuedCalls.iterator().next(),
                type, objectId, triesCount, lastExecutionMessage, processedAt);
    }

    protected void checkCallLocked(QueuedCallType type,
                                   long objectId,
                                   Matcher<Instant> expectedNextTryAt) {
        Collection<QueuedCall> queuedCalls = queuedCallService.findQueuedCalls(type, objectId);
        assertEquals(1, queuedCalls.size());

        checkQueuedCallNotCompleted(queuedCalls.iterator().next(),
                expectedNextTryAt,
                type,
                objectId,
                true,
                0,
                nullValue(String.class));
    }


    protected abstract static class SimpleQueuedCallProcessor implements QueuedCallProcessor {

        @Override
        public int delayBetweenExecutionsOnHostInSeconds() {
            return -1; // не принципиально для обработки.
        }

        @Override
        public int batchSize() {
            return 10;
        }
    }

    private static void checkQueuedCallNotCompleted(QueuedCall actual,
                                                    Matcher<Instant> expectedNextTryAt,
                                                    QueuedCallType expectedType,
                                                    long objId,
                                                    boolean isInProcessing,
                                                    int triesCount,
                                                    Matcher<String> lastExecutionMessage) {
        assertEquals(expectedType, actual.getCallType());
        assertEquals(objId, actual.getObjectId().longValue());
        assertThat(actual.getLastTryErrorMessage(), lastExecutionMessage);
        assertThat(actual.getNextTryAt(), expectedNextTryAt);
        assertEquals(triesCount, actual.getTriesCount());
        assertNotNull(actual.getCreatedAt());
        if (isInProcessing) {
            assertEquals(getProcessorName(), actual.getProcessingBy());
            assertNotNull(actual.getLockedAt());
        } else {
            assertNull(actual.getProcessingBy());
            assertNull(actual.getLockedAt());
        }
        assertNull(actual.getProcessedAt());
    }


    private static void checkQueuedCallCompleted(QueuedCall actual,
                                                 QueuedCallType expectedType,
                                                 long objId,
                                                 int triesCount,
                                                 Matcher<String> lastExecutionMessage,
                                                 Matcher<Instant> processedAt) {
        assertEquals(expectedType, actual.getCallType());
        assertEquals(objId, actual.getObjectId().longValue());
        assertNotNull(actual.getCreatedAt());
        if (triesCount > 0) {
            assertNotNull(actual.getLockedAt());
        } else {
            assertNull(actual.getLockedAt());
        }
        assertThat(actual.getProcessedAt(), processedAt);

        assertThat(actual.getLastTryErrorMessage(), lastExecutionMessage);
        assertEquals(triesCount, actual.getTriesCount());

        assertNull(actual.getProcessingBy());
        assertNull(actual.getNextTryAt());
    }

    protected void createQueuedCall(QueuedCallType type, long objId) {
        transactionTemplate.execute(t -> {
            queuedCallService.addQueuedCalls(
                    Collections.singleton(QueuedCallService.NewQueuedCallBuilder.aNewQueuedCall(type, objId).build())
            );
            return null;

        });
    }

    protected void createQueuedCalls(QueuedCallType type, Collection<Long> objIds) {
        transactionTemplate.execute(t -> {
            queuedCallService.addQueuedCalls(
                    objIds.stream()
                            .map(id -> QueuedCallService.NewQueuedCallBuilder.aNewQueuedCall(type, id).build())
                            .collect(Collectors.toList())
            );
            return null;

        });
    }

    protected interface Function {

        ExecutionResult apply(Long id);
    }

    public static class QueuedCallProcessorStub implements QueuedCallProcessor {

        private final QueuedCallType type = new QueuedCallType() {
            @Nonnull
            @Override
            public QueuedCallObjectType getObjectIdType() {
                return CheckouterQCObjectType.ORDER;
            }

            @Override
            public int getId() {
                return 1000;
            }
        };

        @Nullable
        @Override
        public ExecutionResult process(QueuedCallExecution execution) {
            return null;
        }

        @Nonnull
        @Override
        public QueuedCallType getSupportedType() {
            return type;
        }

        @Override
        public int delayBetweenExecutionsOnHostInSeconds() {
            return 100;
        }

        @Override
        public int batchSize() {
            return 50;
        }
    }
}
