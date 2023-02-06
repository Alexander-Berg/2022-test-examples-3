package ru.yandex.market.tsup.core.pipeline;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsup.AbstractContextualTest;

@DatabaseSetup("/repository/pipeline/pipelines_with_core_cube.xml")
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
class NewPipelineLauncherTest extends AbstractContextualTest {
    @Autowired
    private NewPipelineLauncher newPipelineLauncher;

    @Test
    @ExpectedDatabase(
        value = "/repository/pipeline/pipelines_with_core_cube_after_launching.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/dbqueue/after/after_launching_pipeline.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void launch() {
        newPipelineLauncher.launch();
    }
}
