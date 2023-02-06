package ru.yandex.market.logistics.lom.jobs.executor;

import java.time.Instant;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.mockito.Mockito.mock;

@DatabaseSetup("/jobs/executor/queueTaskStatistics/queue_tasks.xml")
@DisplayName("Тесты логирования статистики количества записей в таблице queue_tasks")
@ParametersAreNonnullByDefault
class QueueTaskStatisticsExecutorTest extends AbstractContextualTest {

    @Autowired
    private QueueTaskStatisticsExecutor queueTaskStatisticsExecutor;
    private final JobExecutionContext jobContext = mock(JobExecutionContext.class);

    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-08-30T15:20:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
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
                    "payload=Queue tasks REGISTER_DELIVERY_TRACK count = 2\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                    "tags=STATISTICS\t" +
                    "extra_keys=amount,type\t" +
                    "extra_values=2,REGISTER_DELIVERY_TRACK\n",
                "level=INFO\t" +
                    "format=plain\t" +
                    "code=COUNT_QUEUE_TASKS\t" +
                    "payload=Queue tasks CREATE_ORDER_EXTERNAL count = 1\t" +
                    "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                    "tags=STATISTICS\t" +
                    "extra_keys=amount,type\t" +
                    "extra_values=1,CREATE_ORDER_EXTERNAL\n"
            );
    }
}
