package ru.yandex.market.delivery.transport_manager.service.dbqueue;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

@DatabaseSetup(value = "/repository/task/no_tasks.xml", connection = "dbUnitDatabaseConnectionDbQueue")
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnectionDbQueue"})
public class DbQueueTaskRestartServiceTest extends AbstractContextualTest {

    @Autowired
    private DbQueueTaskRestartService restartService;

    @Test
    @DatabaseSetup(
        value = "/repository/dbqueue/log_with_payload.xml",
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @DatabaseSetup(
        value = "/repository/task/xdoc_create.xml",
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/task/xdoc_create.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void restartWithoutCreation() {
        restartService.restartFromLog(1L);
    }

    @Test
    @DatabaseSetup(
        value = "/repository/dbqueue/log_with_payload.xml",
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/task/xdoc_create.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void restartWithCreation() {
        restartService.restartFromLog(1L);
    }
}
