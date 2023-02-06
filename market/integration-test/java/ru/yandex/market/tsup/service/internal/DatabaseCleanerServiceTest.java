package ru.yandex.market.tsup.service.internal;

import java.time.Instant;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsup.AbstractContextualTest;

@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
public class DatabaseCleanerServiceTest extends AbstractContextualTest {

    @Autowired
    private DatabaseCleanerService databaseCleanerService;

    @BeforeEach
    void init() {
        clock.setFixed(Instant.parse("2021-09-16T17:00:00Z"), ZoneOffset.UTC);
    }

    @Test
    @DatabaseSetup(
        value = "/repository/dbqueue/logs.xml",
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/dbqueue/after/after_delete.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteOutdatedLogs() {
        databaseCleanerService.cleanLogs();
    }

    @Test
    @DatabaseSetup("/repository/user_acivity_log/logs.xml")
    @ExpectedDatabase(
        value = "/repository/user_acivity_log/after_delete.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteUserActivityLogs() {
        databaseCleanerService.cleanLogs();
    }
}
