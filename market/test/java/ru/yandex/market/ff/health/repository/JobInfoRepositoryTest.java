package ru.yandex.market.ff.health.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.configuration.QuartzDatabaseDatasourceConfig;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

@ContextConfiguration(classes = QuartzDatabaseDatasourceConfig.class)
public class JobInfoRepositoryTest extends IntegrationTest {

    @Autowired
    private JobInfoRepository repository;

    @Test
    @DatabaseSetup("classpath:repository/job-info-repository/empty-job-status/before.xml")
    @ExpectedDatabase(value = "classpath:repository/job-info-repository/empty-job-status/after.xml",
            assertionMode = NON_STRICT)
    public void appendLogBeforeJobStatusEmptyJobStatus() {
        repository.prependJobStatusInfoToLogRow(2L, "Additional information");
    }

    @Test
    @DatabaseSetup("classpath:repository/job-info-repository/finished-job/before.xml")
    @ExpectedDatabase(value = "classpath:repository/job-info-repository/finished-job/after.xml",
            assertionMode = NON_STRICT)
    public void appendBeforeJobStatusByLogIdWithFinishedTime() {
        repository.prependJobStatusInfoToLogRow(1L, "info");
    }
}
