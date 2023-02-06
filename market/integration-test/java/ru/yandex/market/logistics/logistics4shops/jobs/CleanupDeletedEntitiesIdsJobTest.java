package ru.yandex.market.logistics.logistics4shops.jobs;

import java.time.Instant;
import java.time.ZoneId;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DisplayName("Тест джобы очистки информации об удалённых записях")
@ParametersAreNonnullByDefault
class CleanupDeletedEntitiesIdsJobTest extends AbstractIntegrationTest {

    @Autowired
    private CleanupDeletedEntitiesIdsJob job;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2022-05-02T12:00:00Z"),  ZoneId.systemDefault());
    }

    @Test
    @DatabaseSetup("/jobs/cleanupDeletedEntitiesIdsJob/before/setup.xml")
    @ExpectedDatabase(
        value = "/jobs/cleanupDeletedEntitiesIdsJob/after/success_delete.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное удаление")
    void success() throws JobExecutionException {
        job.execute(null);
    }

}
