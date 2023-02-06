package ru.yandex.market.logistics.lom.jobs.executor;

import java.time.Instant;
import java.time.ZoneId;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.Mockito.mock;

@DisplayName("Тест джобы удаления экспортированных исторических событий")
@ParametersAreNonnullByDefault
public class RemoveExportedOrderHistoryEventsExecutorTest extends AbstractContextualTest {

    @Autowired
    private RemoveExportedOrderHistoryEventsExecutor executor;

    private final JobExecutionContext jobContext = mock(JobExecutionContext.class);

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2018-05-05T22:00:00Z"),  ZoneId.systemDefault());
    }

    @Test
    @DatabaseSetup("/jobs/executor/removeExportedHistoryEvents/before/setup_history_events.xml")
    @ExpectedDatabase(
        value = "/jobs/executor/removeExportedHistoryEvents/after/success_delete.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное удаление экспортированных исторических событий")
    @JpaQueriesCount(7)
    void success() {
        executor.doJob(jobContext);
    }

}
