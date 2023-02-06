package ru.yandex.market.logistics.dbqueue;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.jdbc.Sql;
import ru.yoomoney.tech.dbqueue.api.QueueConsumer;
import ru.yoomoney.tech.dbqueue.api.TaskRecord;
import ru.yoomoney.tech.dbqueue.config.impl.CompositeTaskLifecycleListener;
import ru.yoomoney.tech.dbqueue.internal.processing.MillisTimeProvider;
import ru.yoomoney.tech.dbqueue.internal.processing.TaskProcessor;
import ru.yoomoney.tech.dbqueue.internal.processing.TaskResultHandler;
import ru.yoomoney.tech.dbqueue.settings.QueueConfig;

import ru.yandex.market.logistics.dbqueue.listeners.LoggingTaskListener;
import ru.yandex.market.logistics.dbqueue.listeners.RequestIdTaskListener;
import ru.yandex.market.request.trace.RequestContextHolder;

@Slf4j
@Sql("/create_queue_tasks_table.sql")
public class DbQueueIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Проверка логгирования жизненного цикла тасок: таска успешно выполнена")
    void testLogging() {
        log.info("Start testing tasks listeners");
        QueueConsumer<?> queueConsumer = consumers.get(0);
        getTasksProcessor(queueConsumer).processTask(
            queueConsumer,
            taskRecord("{\"data\":\"some-data\",\"requestId\":\"test-request-id\"}")
        );

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "code=TEST_QUEUE\t"
                + "payload=Started executing task = {id=1, attemptsCount=1, reenqueueAttemptsCount=1, "
                + "totalAttemptsCount=0, createdAt=2021-11-11T11:11:11Z, nextProcessAt=2021-11-11T11:11:11Z} "
                + "with payload = {\\\"data\\\":\\\"some-data\\\",\\\"requestId\\\":\\\"test-request-id\\\"}\t"
                + "tags=DB_QUEUE_TASK\t"
                + "extra_keys=taskId,status\t"
                + "extra_values=1,EXECUTE",
            "code=TEST_QUEUE\t"
                + "payload=Finished task = {id=1, attemptsCount=1, reenqueueAttemptsCount=1, totalAttemptsCount=0, "
                + "createdAt=2021-11-11T11:11:11Z, nextProcessAt=2021-11-11T11:11:11Z} "
                + "with payload = {\\\"data\\\":\\\"some-data\\\",\\\"requestId\\\":\\\"test-request-id\\\"}\t"
                + "request_id=test-request-id\t"
                + "tags=DB_QUEUE_TASK\t"
                + "extra_keys=taskId,status,attempts\t"
                + "extra_values=1,FINISHED,1"
        );
    }

    @Test
    @DisplayName("Проверка логгирования жизненного цикла тасок: таска выполнена с ошибкой")
    void testLoggingTaskError() {
        log.info("Start testing tasks error listeners");
        QueueConsumer<?> queueConsumer = consumers.get(0);
        getTasksProcessor(queueConsumer).processTask(
            queueConsumer,
            taskRecord("{\"requestId\":\"test-request-id\"}")
        );

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "code=TEST_QUEUE\t"
                + "payload=Started executing task = {id=1, attemptsCount=1, reenqueueAttemptsCount=1, "
                + "totalAttemptsCount=0, createdAt=2021-11-11T11:11:11Z, nextProcessAt=2021-11-11T11:11:11Z} "
                + "with payload = {\\\"requestId\\\":\\\"test-request-id\\\"}\t"
                + "tags=DB_QUEUE_TASK\t"
                + "extra_keys=taskId,status\t"
                + "extra_values=1,EXECUTE",
            "code=TEST_QUEUE\t"
                + "payload=Finished task = {id=1, attemptsCount=1, reenqueueAttemptsCount=1, totalAttemptsCount=0, "
                + "createdAt=2021-11-11T11:11:11Z, nextProcessAt=2021-11-11T11:11:11Z} "
                + "with payload = {\\\"requestId\\\":\\\"test-request-id\\\"}\t"
                + "request_id=test-request-id\t"
                + "tags=DB_QUEUE_TASK\t"
                + "extra_keys=taskId,status,attempts\t"
                + "extra_values=1,FINISHED,1",
            "level=ERROR\t"
                + "format=json-exception\t"
                + "code=TEST_QUEUE\t"
                + "payload={\\\"eventMessage\\\":\\\"Crashed task = {id=1, attemptsCount=1, reenqueueAttemptsCount=1, "
                + "totalAttemptsCount=0, createdAt=2021-11-11T11:11:11Z, nextProcessAt=2021-11-11T11:11:11Z} "
                + "with payload = {\\\\\\\"requestId\\\\\\\":\\\\\\\"test-request-id\\\\\\\"}\\\","
                + "\\\"exceptionMessage\\\":\\\"IllegalStateException: payload data is blank\\\","
                + "\\\"stackTrace\\\":\\\"java.lang.IllegalStateException: payload data is blank\\\\n\\\\t"
                + "at ru.yandex.market.logistics.dbqueue.processor.TestQueueProcessor.execute("
                + "TestQueueProcessor.java:20)",
            "tags=DB_QUEUE_TASK\t"
                + "extra_keys=taskId,status,attempts\t"
                + "extra_values=1,CRASHED,1"
        );
    }

    @MethodSource
    @ParameterizedTest
    @DisplayName("Запуск таски по ее названию и телу таски")
    void runQueueByName(
        @SuppressWarnings("unused") String displayName,
        String queueName,
        boolean taskProduced
    ) {
        softly.assertThat(dbQueueService.reproduceTaskByQueueName(queueName, "{}"))
            .isEqualTo(taskProduced);
    }

    @Test
    @SneakyThrows
    @DatabaseSetup("/queue_tasks_setup.xml")
    @DisplayName("Проверка записи статистики в лог")
    void logStatistics() {
        RequestContextHolder.createContext("test-request-id");
        queueTaskStatisticsExecutor.execute(null);
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "level=INFO\t"
                + "format=plain\t"
                + "code=COUNT_QUEUE_TASKS\t"
                + "payload=Queue tasks RETURN_CHANGE_ORDER_ITEMS count = 4\t"
                + "request_id=test-request-id\t"
                + "tags=STATISTICS\t"
                + "extra_keys=amount,queueName\t"
                + "extra_values=4,RETURN_CHANGE_ORDER_ITEMS\n",
                "level=INFO\t"
                + "format=plain\t"
                + "code=COUNT_QUEUE_TASKS\t"
                + "payload=Queue tasks RETURN_CANCEL_ORDER count = 2\t"
                + "request_id=test-request-id\t"
                + "tags=STATISTICS\t"
                + "extra_keys=amount,queueName\t"
                + "extra_values=2,RETURN_CANCEL_ORDER\n",
                "level=INFO\t"
                + "format=plain\t"
                + "code=COUNT_QUEUE_TASKS\t"
                + "payload=Queue tasks count = 6\t"
                + "request_id=test-request-id\t"
                + "tags=STATISTICS\t"
                + "extra_keys=amount,queueName\t"
                + "extra_values=6,all\n",
                "level=WARN\t"
                + "format=plain\t"
                + "code=COUNT_FAILED_QUEUE_TASKS\t"
                + "payload=Queue RETURN_CHANGE_ORDER_ITEMS reached max attempts count\t"
                + "request_id=test-request-id\t"
                + "tags=STATISTICS\t"
                + "extra_keys=amount,queueName\t"
                + "extra_values=1,RETURN_CHANGE_ORDER_ITEMS\n",
                "level=WARN\t"
                + "format=plain\t"
                + "code=COUNT_FAILED_QUEUE_TASKS\t"
                + "payload=Queue RETURN_CANCEL_ORDER reached max attempts count\t"
                + "request_id=test-request-id\t"
                + "tags=STATISTICS\t"
                + "extra_keys=amount,queueName\t"
                + "extra_values=1,RETURN_CANCEL_ORDER\n"
            );
    }

    @Nonnull
    private static Stream<Arguments> runQueueByName() {
        return Stream.of(
            Arguments.of(
                "Таска с заданным названием существует",
                "TEST_QUEUE",
                true
            ),
            Arguments.of(
                "Таска с заданным названием не существует",
                "SOME_NON_EXISTING_QUEUE_NAME",
                false
            )
        );
    }

    @Nonnull
    private TaskProcessor getTasksProcessor(QueueConsumer queueConsumer) {
        QueueConfig queueConfig = queueConsumer.getQueueConfig();
        TaskResultHandler taskResultHandler = new TaskResultHandler(
            queueConfig.getLocation(),
            queueShard,
            queueConfig.getSettings().getReenqueueSettings()
        );

        return new TaskProcessor(
            queueShard,
            new CompositeTaskLifecycleListener(List.of(
                new RequestIdTaskListener(),
                new LoggingTaskListener()
            )),
            new MillisTimeProvider.SystemMillisTimeProvider(),
            taskResultHandler
        );
    }

    @Nonnull
    private TaskRecord taskRecord(String payload) {
        return TaskRecord.builder()
            .withId(1L)
            .withAttemptsCount(1L)
            .withReenqueueAttemptsCount(1L)
            .withCreatedAt(ZonedDateTime.parse("2021-11-11T11:11:11.00Z"))
            .withNextProcessAt(ZonedDateTime.parse("2021-11-11T11:11:11.00Z"))
            .withPayload(payload)
            .build();
    }
}
