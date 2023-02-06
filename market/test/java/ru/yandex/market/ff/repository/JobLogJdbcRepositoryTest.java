package ru.yandex.market.ff.repository;

import java.time.LocalDateTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.configuration.QuartzDatabaseDatasourceConfig;

@ContextConfiguration(classes = QuartzDatabaseDatasourceConfig.class)
public class JobLogJdbcRepositoryTest extends IntegrationTest {

    @Autowired
    private JobLogJdbcRepository repository;

    @Test
    @DatabaseSetup("classpath:repository/job-log-jdbc-repository/before.xml")
    public void getLastSuccessfulExecutionTest() {
        LocalDateTime lastSuccessfulExecution = repository.getLastSuccessfulExecution("testJob");
        assertions.assertThat(lastSuccessfulExecution).isEqualTo(LocalDateTime.of(2021, 8, 31, 15, 18, 0, 0));
    }

    @Test
    @DatabaseSetup("classpath:empty.xml")
    public void getLastSuccessfulExecutionWhenNoResultTest() {
        LocalDateTime lastSuccessfulExecution = repository.getLastSuccessfulExecution("testJob");
        assertions.assertThat(lastSuccessfulExecution).isNull();
    }
}
