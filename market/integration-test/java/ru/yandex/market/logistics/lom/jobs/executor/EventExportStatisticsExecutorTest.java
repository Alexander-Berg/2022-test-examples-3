package ru.yandex.market.logistics.lom.jobs.executor;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.mockito.Mockito.mock;

@DatabaseSetup("/jobs/executor/eventExportStatistics/history_events.xml")
@DisplayName("Тесты логирования статистики количества не экспортированных исторических ивентов")
@ParametersAreNonnullByDefault
class EventExportStatisticsExecutorTest extends AbstractContextualTest {

    @Autowired
    private EventExportStatisticsExecutor eventExportStatisticsExecutor;

    private final JobExecutionContext jobContext = mock(JobExecutionContext.class);

    @Test
    @DisplayName("Проверка записи статистики в лог")
    void logStatistics() {
        eventExportStatisticsExecutor.doJob(jobContext);

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("level=INFO\t" +
                "format=plain\t" +
                "code=COUNT_NOT_EXPORTED_HISTORY_EVENTS\t" +
                "payload=5 order history events pending for export to logbroker\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=NOT_EXPORTED_HISTORY_EVENTS_STATS\t" +
                "extra_keys=amount\t" +
                "extra_values=5\n");
    }
}
