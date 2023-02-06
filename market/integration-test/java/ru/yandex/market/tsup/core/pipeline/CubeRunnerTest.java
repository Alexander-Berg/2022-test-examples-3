package ru.yandex.market.tsup.core.pipeline;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsup.AbstractContextualTest;

@DatabaseSetup("/repository/pipeline/two_pipelines_with_cubes.xml")
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
class CubeRunnerTest extends AbstractContextualTest {
    @Autowired
    private CubeRunner cubeRunner;

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
    void run() {
        cubeRunner.run(List.of(2L, 3L, 7L));
    }
}
