package ru.yandex.market.tsup.core.pipeline;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsup.AbstractContextualTest;

@DatabaseSetup("/repository/pipeline/pipelines_with_core_cube_after_launching.xml")
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
class PipelineRunnerTest extends AbstractContextualTest {
    @Autowired
    private PipelineRunner pipelineRunner;

    @Test
    @ExpectedDatabase(
        value = "/repository/pipeline/pipelines_with_core_cube_after_running.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/dbqueue/after/after_running_pipeline.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void run() {
        pipelineRunner.run(1L);
    }

    @Test
    void runNotRunnable() {
        Assertions.assertThrows(RuntimeException.class, () -> pipelineRunner.run(2L));
    }
}
