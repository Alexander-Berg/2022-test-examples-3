package ru.yandex.market.queuedcalls;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import javax.annotation.Nonnull;

import com.opentable.db.postgres.junit5.EmbeddedPostgresExtension;
import com.opentable.db.postgres.junit5.PreparedDbExtension;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.queuedcalls.configuration.DatabaseConfiguration;
import ru.yandex.market.queuedcalls.configuration.TestConfiguration;
import ru.yandex.market.queuedcalls.configuration.TestProcessorsConfiguration;
import ru.yandex.market.queuedcalls.impl.QCTypeCancellationToken;
import ru.yandex.market.queuedcalls.impl.QueuedCallDao;

import static com.opentable.db.postgres.embedded.LiquibasePreparer.forClasspathLocation;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.queuedcalls.impl.ProcessorNameHolder.getProcessorName;

@ExtendWith({SpringExtension.class})
@ContextConfiguration(classes = {TestConfiguration.class, QueuedCallConfiguration.class,
        DatabaseConfiguration.class, TestProcessorsConfiguration.class})
@TestExecutionListeners({
        DBCleanerTestExecutionListener.class,
        DependencyInjectionTestExecutionListener.class,
        // можно помечать тест как транзакционный, чтобы транзакция начиналась в тесте
        TransactionalTestExecutionListener.class
})
public class AbstractQueuedCallTest {

    @RegisterExtension
    public static PreparedDbExtension preparedDbExtension =
            EmbeddedPostgresExtension.preparedDatabase(forClasspathLocation("queued_calls/changelog.xml"))
                    .customize(builder -> builder.setServerConfig("unix_socket_directories", ""));

    private final HashMap<QueuedCallType, QCTypeCancellationToken> tokensByType = new HashMap<>();
    @Autowired
    protected TestableClock clock;
    @Autowired
    protected TransactionTemplate transactionTemplate;
    @Autowired
    protected QueuedCallService qcService;
    @Autowired
    protected QueuedCallDao qcDao;
    @Autowired
    protected QueuedCallSettingsService qcSettingsService;
    @Autowired
    protected JdbcTemplate jdbcTemplate;

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

    @AfterEach
    public void tearDown() {
        qcSettingsService.updateSettings(new QueuedCallSettings());
    }

    protected void executeQueuedCalls(QueuedCallType type, Function objectIdProcessor) {
        qcService.executeQueuedCallBatch(
                new SimpleQueuedCallProcessor() {
                    @Override
                    public ExecutionResult process(QueuedCallProcessor.QueuedCallExecution execution) {
                        return objectIdProcessor.apply(execution.getObjId());
                    }

                    @Override
                    public int maxAgeInDays() {
                        return 999;
                    }

                    @Nonnull
                    @Override
                    public QueuedCallType getSupportedType() {
                        return type;
                    }
                },
                tokensByType.computeIfAbsent(type, t -> new QCTypeCancellationToken(t, qcSettingsService))
        );
    }

    protected void checkCallInQueue(QueuedCallType type,
                                    long objectId,
                                    Matcher<Instant> expectedNextTryAt,
                                    int triesCount,
                                    Matcher<String> lastExecutionMessage) {
        Collection<QueuedCall> queuedCalls = qcService.findQueuedCalls(type, objectId);
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
        Collection<QueuedCall> queuedCalls = qcDao.allCallsForObject(Collections.singleton(type), objectId);
        assertEquals(1, queuedCalls.size());

        checkQueuedCallCompleted(queuedCalls.iterator().next(),
                type, objectId, triesCount, lastExecutionMessage, processedAt);
    }

    protected void checkSettingsFunctionallyNotEqual(QueuedCallSettings v1, QueuedCallSettings v2) {
        assertFalse(areSettingsFunctionallyEqual(v1, v2));
    }

    protected void checkSettingsFunctionallyEqual(QueuedCallSettings v1, QueuedCallSettings v2) {
        assertTrue(areSettingsFunctionallyEqual(v1, v2));
    }

    protected boolean areSettingsFunctionallyEqual(QueuedCallSettings v1, QueuedCallSettings v2) {
        QueuedCallSettings v1Clone = new QueuedCallSettings(v1);
        QueuedCallSettings v2Clone = new QueuedCallSettings(v2);
        v1Clone.getTypesSettings().forEach(typeSettings -> v2Clone.getOrCreateTypeSettings(typeSettings.getType()));
        v2Clone.getTypesSettings().forEach(typeSettings -> v1Clone.getOrCreateTypeSettings(typeSettings.getType()));
        return v1Clone.equals(v2Clone);
    }

    protected void checkCallLocked(QueuedCallType type,
                                   long objectId,
                                   Matcher<Instant> expectedNextTryAt) {
        Collection<QueuedCall> queuedCalls = qcService.findQueuedCalls(type, objectId);
        assertEquals(1, queuedCalls.size());

        checkQueuedCallNotCompleted(queuedCalls.iterator().next(),
                expectedNextTryAt,
                type,
                objectId,
                true,
                0,
                nullValue(String.class));
    }

    protected void createQueuedCall(QueuedCallType type, long objId) {
        transactionTemplate.execute(t -> {
            qcService.addQueuedCall(type, objId);
            return null;
        });
    }

    protected Instant setFixedTime(String localDateTime) {
        Instant someTime = LocalDateTime.parse(localDateTime).atZone(ZoneId.systemDefault()).toInstant();
        setFixedTime(someTime);
        return someTime;
    }

    /**
     * Замораживет время во всем приложении.
     */
    protected void setFixedTime(Instant instant) {
        this.clock.setFixed(instant, ZoneId.systemDefault());
    }

    /**
     * Очищает зафиксированное время.
     */
    protected void clearFixed() {
        clock.clearFixed();
    }

    protected interface Function {

        ExecutionResult apply(Long id) throws RuntimeException;
    }

    protected abstract static class SimpleQueuedCallProcessor implements QueuedCallProcessor {

        @Override
        public int delayBetweenExecutionsOnHostInSeconds() {
            return -1;
        }

        @Override
        public int batchSize() {
            return 10;
        }
    }
}
