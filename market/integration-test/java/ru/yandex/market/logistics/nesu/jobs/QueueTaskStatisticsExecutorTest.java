package ru.yandex.market.logistics.nesu.jobs;

import java.time.Instant;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.executor.QueueTaskStatisticsExecutor;

import static org.mockito.Mockito.mock;

@DisplayName("Тесты логирования статистики количества записей в таблице queue_tasks")
@ParametersAreNonnullByDefault
class QueueTaskStatisticsExecutorTest extends AbstractContextualTest {

    @Autowired
    private QueueTaskStatisticsExecutor queueTaskStatisticsExecutor;
    private final JobExecutionContext jobContext = mock(JobExecutionContext.class);

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-08-30T15:20:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DatabaseSetup("/jobs/executors/queueTaskStatistics/queue_tasks.xml")
    @DisplayName("Проверка записи статистики в лог")
    void logStatistics() {
        queueTaskStatisticsExecutor.doJob(jobContext);

        softly.assertThat(backLogCaptor.getResults().toString())
                .contains(
                        "level=INFO\t" +
                                "format=plain\t" +
                                "code=COUNT_QUEUE_TASKS\t" +
                                "payload=Queue tasks count = 3\t" +
                                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                                "tags=STATISTICS\t" +
                                "extra_keys=amount,type\t" +
                                "extra_values=3,all\n",
                        "level=INFO\t" +
                                "format=plain\t" +
                                "code=COUNT_QUEUE_TASKS\t" +
                                "payload=Queue tasks PROCESS_UPLOADED_FEED count = 2\t" +
                                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                                "tags=STATISTICS\t" +
                                "extra_keys=amount,type\t" +
                                "extra_values=2,PROCESS_UPLOADED_FEED\n",
                        "level=INFO\t" +
                                "format=plain\t" +
                                "code=COUNT_QUEUE_TASKS\t" +
                                "payload=Queue tasks MODIFIER_UPLOAD count = 1\t" +
                                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                                "tags=STATISTICS\t" +
                                "extra_keys=amount,type\t" +
                                "extra_values=1,MODIFIER_UPLOAD\n"
                );

        softly.assertThat(backLogCaptor.getResults().toString())
                .contains(
                        "level=INFO\t" +
                                "format=plain\t" +
                                "code=COUNT_QUEUE_TASKS_RETRIED_OVER_LIMIT\t" +
                                "payload=Queue tasks retried over 3 times count = 3\t" +
                                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                                "tags=STATISTICS\t" +
                                "extra_keys=amount,type\t" +
                                "extra_values=3,all\n",
                        "level=INFO\t" +
                                "format=plain\t" +
                                "code=COUNT_QUEUE_TASKS_RETRIED_OVER_LIMIT\t" +
                                "payload=Queue tasks PROCESS_UPLOADED_FEED retried over 3 times count = 1\t" +
                                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                                "tags=STATISTICS\t" +
                                "extra_keys=amount,type\t" +
                                "extra_values=1,PROCESS_UPLOADED_FEED\n",
                        "level=INFO\t" +
                                "format=plain\t" +
                                "code=COUNT_QUEUE_TASKS_RETRIED_OVER_LIMIT\t" +
                                "payload=Queue tasks MODIFIER_UPLOAD retried over 3 times count = 1\t" +
                                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                                "tags=STATISTICS\t" +
                                "extra_keys=amount,type\t" +
                                "extra_values=1,MODIFIER_UPLOAD\n",
                        "level=INFO\t" +
                                "format=plain\t" +
                                "code=COUNT_QUEUE_TASKS_RETRIED_OVER_LIMIT\t" +
                                "payload=Queue tasks CREATE_TRUST_PRODUCT retried over 3 times count = 1\t" +
                                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                                "tags=STATISTICS\t" +
                                "extra_keys=amount,type\t" +
                                "extra_values=1,CREATE_TRUST_PRODUCT\n"
                );
    }

    @Test
    @DisplayName("Проверка записи пустой статистики в лог")
    void logEmptyStatistics() {
        queueTaskStatisticsExecutor.doJob(jobContext);

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "level=INFO\t" +
                    "format=plain\t" +
                    "code=COUNT_QUEUE_TASKS\t" +
                    "payload=Queue tasks count = 0\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                    "tags=STATISTICS\t" +
                    "extra_keys=amount,type\t" +
                    "extra_values=0,all\n"
            );
    }
}

