package ru.yandex.market.logistics.nesu.jobs;

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

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.executor.CleanupDeletedEntitiesIdsExecutor;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.Mockito.mock;

@DisplayName("Тест джобы очистки информации об удалённых записях")
@ParametersAreNonnullByDefault
public class CleanupDeletedEntitiesIdsExecutorTest extends AbstractContextualTest {

    @Autowired
    private CleanupDeletedEntitiesIdsExecutor executor;

    private final JobExecutionContext jobContext = mock(JobExecutionContext.class);

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2022-05-02T12:00:00Z"),  ZoneId.systemDefault());
    }

    @Test
    @DatabaseSetup("/jobs/executors/cleanupDeletedEntitiesIdsExecutor/before/setup.xml")
    @ExpectedDatabase(
        value = "/jobs/executors/cleanupDeletedEntitiesIdsExecutor/after/success_delete.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное удаление")
    void success() {
        executor.doJob(jobContext);
    }

}
