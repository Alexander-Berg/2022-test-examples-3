package ru.yandex.market.tsup.core.pipeline;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsup.AbstractContextualTest;

@DatabaseSetup("/repository/pipeline/two_pipelines_with_cubes.xml")
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
class ReadyCubeLauncherTest extends AbstractContextualTest {
    @Autowired
    private ReadyCubeLauncher readyCubeLauncher;

    @Test
    @ExpectedDatabase(
        value = "/repository/pipeline/two_pipelines_with_cubes_after_launcher.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/dbqueue/after/after_launching_cubes.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void launch() {
        readyCubeLauncher.launch();
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/pipeline/two_pipelines_with_cubes_after_launching_first_only.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/dbqueue/after/after_launching_cubes_for_first_pipeline_only.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void launchByPipeline() {
        readyCubeLauncher.launchByPipeline(1L);
    }
}
