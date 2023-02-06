package ru.yandex.market.queuedcalls;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ru.yandex.market.queuedcalls.retry.RetryStrategies;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.queuedcalls.model.TestQCType.FIRST;
import static ru.yandex.market.queuedcalls.model.TestQCType.SECOND;

public class QueuedCallServiceTest extends AbstractQueuedCallTest {

    private static final String SQL_INSERT_TYPE_SETTINGS =
            "insert into qc_type_settings (ID, CALL_TYPE, DISABLED, UPDATED_AT) " +
                    " values (nextval('qc_type_settings_seq'), ?, ?, ?)";

    @Test
    public void testCreateQueuedCalls() {
        Instant someTime = setFixedTime("2027-07-06T10:15:30");
        createQueuedCall(FIRST, 10L);
        checkCallInQueue(FIRST, 10L, equalTo(someTime));
    }

    @Test
    public void testProcessingLock() {
        Instant someTime0 = setFixedTime("2027-07-06T10:15:30");
        createQueuedCall(FIRST, 11L);

        setFixedTime("2027-07-06T10:15:35");
        createQueuedCall(SECOND, 12L);

        Instant someTime2 = setFixedTime("2027-07-06T10:15:40");
        createQueuedCall(FIRST, 12L);

        Instant someTime3 = setFixedTime("2027-07-06T10:15:50");
        createQueuedCall(FIRST, 13L);

        //извлекли 2 самых первых
        Map<Long, QueuedCallProcessor.QueuedCallExecution> calls =
                transactionTemplate.execute(t -> qcDao.reserveCallsToExecute(FIRST, 2));
        assertNotNull(calls);
        assertEquals(2, calls.size());
        assertThat(calls.values().stream()
                        .map(QueuedCallProcessor.QueuedCallExecution::getObjId)
                        .collect(Collectors.toList()),
                Matchers.containsInAnyOrder(11L, 12L));
        //проверяем что их залочили
        checkCallLocked(FIRST, 11L, equalTo(someTime0));
        checkCallLocked(FIRST, 12L, equalTo(someTime2));

        //проверяем что теперь отберется и залочится последний
        calls = transactionTemplate.execute(t -> qcDao.reserveCallsToExecute(FIRST, 2));
        assertNotNull(calls);
        assertEquals(1, calls.size());
        assertEquals(13L, calls.values().iterator().next().getObjId());
        checkCallLocked(FIRST, 13L, equalTo(someTime3));
    }

    @Test
    public void testSuccessfulExecution() {
        Instant someTime0 = setFixedTime("2027-07-06T10:15:30");
        createQueuedCall(FIRST, 11L);

        executeQueuedCalls(FIRST, id -> {
            assertEquals(11L, id.longValue());
            return ExecutionResult.SUCCESS;
        });

        checkCallCompletedAfterExecution(FIRST, 11L, equalTo(someTime0));
        var stats = qcService.collectStatistic();
        assertNotNull(stats);

        stats.stream()
                .filter(stat -> stat.getType() == FIRST)
                .forEach(stat -> {
                    assertEquals(stat.getExecutingCalls(), 0);
                    assertEquals(stat.getErrorsCount(), 0);
                    assertEquals(stat.getCallsInQueue(), 0);
                    assertEquals(stat.getSuccessOneTryCalls(), 1);
                    assertEquals(stat.getSuccessManyTryCalls(), 0);
                });
    }

    @Test
    public void testSuccessfulExecutionWithNullResult() {
        Instant someTime0 = setFixedTime("2027-07-06T10:15:30");

        createQueuedCall(FIRST, 11L);

        executeQueuedCalls(FIRST, id -> {
            assertEquals(11L, id.longValue());
            return null;
        });

        checkCallCompletedAfterExecution(FIRST, 11L, equalTo(someTime0));
    }

    @Test
    public void testExecutionWithError() {
        setFixedTime("2027-07-06T10:15:30");
        createQueuedCall(FIRST, 11L);
        Instant someTime1 = setFixedTime("2027-11-06T13:15:30");

        executeQueuedCalls(FIRST, id -> {
            assertEquals(11L, id.longValue());
            throw new RuntimeException("someMessage");
        });

        checkCallInQueue(FIRST, 11L,
                equalTo(someTime1.plus(10, ChronoUnit.MINUTES)),
                1,
                startsWith("Exception class: java.lang.RuntimeException\n" +
                        "Message: someMessage"));
    }

    @Test
    public void testSecondExecutionAfterError() {
        Instant someTime = setFixedTime("2027-11-06T13:15:30");
        createQueuedCall(FIRST, 11L);

        executeQueuedCalls(FIRST, id -> {
            assertEquals(11L, id.longValue());
            return ExecutionResult.errorResult("someMessage", RetryStrategies.IN_10_MINUTES_FIXED);
        });

        Instant someTime2 = setFixedTime("2028-11-06T13:30:00");
        executeQueuedCalls(FIRST, id -> {
            assertEquals(11L, id.longValue());
            return ExecutionResult.SUCCESS;
        });

        checkCallCompleted(FIRST, 11L, equalTo(someTime2), 2, nullValue(String.class));
        var stats = qcService.collectStatistic();
        assertNotNull(stats);

        stats.stream()
                .filter(stat -> stat.getType() == FIRST)
                .forEach(stat -> {
                    assertEquals(stat.getExecutingCalls(), 0);
                    assertEquals(stat.getErrorsCount(), 0);
                    assertEquals(stat.getCallsInQueue(), 0);
                    assertEquals(stat.getSuccessOneTryCalls(), 0);
                    assertEquals(stat.getSuccessManyTryCalls(), 1);
                });
    }


    @Test
    public void testCompleteTooOldQc() {
        Instant someTime = setFixedTime("2021-11-06T13:15:30");
        createQueuedCall(FIRST, 11L);

        executeQueuedCalls(FIRST, id -> {
            assertEquals(11L, id.longValue());
            return ExecutionResult.errorResult("someMessage", RetryStrategies.fixedInterval(Duration.ofDays(5000)));
        });

        Instant someTime2 = setFixedTime("2028-11-06T13:30:00");
        executeQueuedCalls(FIRST, id -> {
            assertEquals(11L, id.longValue());
            return ExecutionResult.SUCCESS;
        });

        checkCallCompleted(FIRST, 11L, equalTo(someTime), 1, nullValue(String.class));
    }

    @Test
    public void testDelayExecution() {
        setFixedTime("2027-07-06T10:15:30");
        createQueuedCall(FIRST, 11L);
        setFixedTime("2027-11-06T13:15:30");

        Instant nextExecutionTime =
                LocalDateTime.parse("2029-02-06T13:15:30").atZone(ZoneId.systemDefault()).toInstant();

        executeQueuedCalls(FIRST, id -> {
            assertEquals(11L, id.longValue());
            return ExecutionResult.delayExecution(nextExecutionTime);
        });

        checkCallInQueue(FIRST, 11L, equalTo(nextExecutionTime));
    }

    @Test
    public void resetAllSettingsToDefaultTest() {
        qcSettingsService.resetAllSettingsToDefault();
        QueuedCallSettings qcSettingsInit = qcSettingsService.readSettings();

        // Изменяем globalSuspend
        QueuedCallSettings qcSettings = qcSettingsService.readSettings();
        qcSettings.setGloballySuspended(!qcSettings.isGloballySuspended());
        qcSettingsService.updateSettings(qcSettings);
        // Проверяем, что настройки изменились
        checkSettingsFunctionallyNotEqual(
                qcSettingsInit,
                qcSettingsService.readSettings()
        );
        // Вызываем resetAllSettingsToDefault
        qcSettingsService.resetAllSettingsToDefault();
        // Проверяем, что настройки эквивалентны изначальным
        QueuedCallSettings qcSettings2 = qcSettingsService.readSettings();
        checkSettingsFunctionallyEqual(qcSettingsInit, qcSettings2);

        // Изменяем processingDisabled одного из типов
        qcSettings = qcSettingsService.readSettings();
        QueuedCallTypeSettings typeSettings = qcSettings.getOrCreateTypeSettings(FIRST);
        typeSettings.setProcessingDisabled(!typeSettings.isProcessingDisabled());
        qcSettingsService.updateSettings(qcSettings);
        // Проверяем, что настройки изменились
        checkSettingsFunctionallyNotEqual(
                qcSettingsInit,
                qcSettingsService.readSettings()
        );
        // Вызываем resetAllSettingsToDefault
        qcSettingsService.resetAllSettingsToDefault();
        // Проверяем, что настройки эквивалентны изначальным
        qcSettings2 = qcSettingsService.readSettings();
        checkSettingsFunctionallyEqual(qcSettingsInit, qcSettings2);
    }

    @Test
    void collectStatisticShouldReturnData() {
        QueuedCallSettings queuedCallSettings = qcSettingsService.readSettings();
        Set<QueuedCallType> collect = queuedCallSettings.getTypesSettings().stream()
                .map(QueuedCallTypeSettings::getType)
                .collect(Collectors.toSet());

        transactionTemplate.execute(t -> {
            if (!collect.contains(FIRST)) {
                jdbcTemplate.update(SQL_INSERT_TYPE_SETTINGS, FIRST.getId(), false, Timestamp.from(clock.instant()));
            }
            if (!collect.contains(SECOND)) {
                jdbcTemplate.update(SQL_INSERT_TYPE_SETTINGS, SECOND.getId(), false, Timestamp.from(clock.instant()));
            }
            return null;
        });

        Collection<QueuedCallStats> stats = qcService.collectStatistic();
        assertNotNull(stats);
        stats.forEach(stat -> {
            assertEquals(stat.getErrorsCount(), 0);
            assertEquals(stat.getExecutingCalls(), 0);
            assertEquals(stat.getMaxTriesCount(), 0);
            assertEquals(stat.getSuccessOneTryCalls(), 0);
            assertEquals(stat.getSuccessManyTryCalls(), 0);
        });

        createQueuedCall(FIRST, 11L);
        stats = qcService.collectStatistic();
        assertNotNull(stats);

        stats.stream()
                .filter(stat -> stat.getType() == FIRST)
                .forEach(stat -> {
                    assertEquals(stat.getExecutingCalls(), 0);
                    assertEquals(stat.getErrorsCount(), 0);
                    assertEquals(stat.getCallsInQueue(), 1);
                    assertEquals(stat.getSuccessOneTryCalls(), 0);
                    assertEquals(stat.getSuccessManyTryCalls(), 0);
                });
    }

    @Test
    void existsQueuedCallShouldWork() {
        assertFalse(qcService.existsQueuedCall(FIRST, 11L));

        createQueuedCall(FIRST, 11L);
        assertTrue(qcService.existsQueuedCall(FIRST, 11L));
        assertFalse(qcService.existsQueuedCall(FIRST, 12L));
        assertFalse(qcService.existsQueuedCall(SECOND, 11L));

        executeQueuedCalls(FIRST, id -> {
            assertEquals(11L, id);
            return ExecutionResult.SUCCESS;
        });
        assertFalse(qcService.existsQueuedCall(FIRST, 11L));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void existsQueuedCallBulkShouldWorkWithWrongArguments() {
        Set<Long> ids = qcService.existsQueuedCall(FIRST, null);
        assertNotNull(ids);
        assertThat(ids, empty());

        ids = qcService.existsQueuedCall(FIRST, Collections.emptyList());
        assertNotNull(ids);
        assertThat(ids, empty());

        ids = qcDao.existsNotCompletedCallsForObjects(null, null);
        assertNotNull(ids);
        assertThat(ids, empty());

        ids = qcDao.existsNotCompletedCallsForObjects(Collections.emptyList(), Collections.emptyList());
        assertNotNull(ids);
        assertThat(ids, empty());
    }

    @Test
    void existsQueuedCallBulkShouldWorkOnEmptyDB() {
        final List<Long> objIds = Arrays.asList(11L, 22L, 33L);
        Set<Long> ids = qcService.existsQueuedCall(FIRST, objIds);
        assertNotNull(ids);
        assertThat(ids, empty());

        ids = qcService.existsQueuedCall(SECOND, objIds);
        assertNotNull(ids);
        assertThat(ids, empty());
    }

    @Test
    void existsQueuedCallBulkShouldWork() {
        final List<Long> objIds = Arrays.asList(11L, 22L, 33L);

        createQueuedCall(FIRST, 11L);
        Set<Long> ids = qcService.existsQueuedCall(FIRST, objIds);
        assertNotNull(ids);
        assertThat(ids, hasSize(1));
        assertThat(ids, contains(11L));

        createQueuedCall(FIRST, 33L);
        ids = qcService.existsQueuedCall(FIRST, objIds);
        assertNotNull(ids);
        assertThat(ids, hasSize(2));
        assertThat(ids, containsInAnyOrder(11L, 33L));

        ids = qcService.existsQueuedCall(SECOND, objIds);
        assertNotNull(ids);
        assertThat(ids, empty());

        executeQueuedCalls(FIRST, id -> ExecutionResult.SUCCESS);
        ids = qcService.existsQueuedCall(FIRST, objIds);
        assertNotNull(ids);
        assertThat(ids, empty());
    }

    @Test
    void existsQueuedCallBulkWithMultipleQC() {
        final List<Long> objIds = Arrays.asList(11L, 22L, 33L);
        createQueuedCall(FIRST, 11L);
        createQueuedCall(SECOND, 11L);
        Set<Long> ids = qcService.existsQueuedCall(FIRST, objIds);
        assertNotNull(ids);
        assertThat(ids, hasSize(1));
        assertThat(ids, contains(11L));

        executeQueuedCalls(FIRST, id -> ExecutionResult.SUCCESS);
        ids = qcService.existsQueuedCall(FIRST, objIds);
        assertNotNull(ids);
        assertThat(ids, empty());

        createQueuedCall(FIRST, 11L);
        ids = qcService.existsQueuedCall(FIRST, objIds);
        assertNotNull(ids);
        assertThat(ids, hasSize(1));
        assertThat(ids, contains(11L));
    }
}
