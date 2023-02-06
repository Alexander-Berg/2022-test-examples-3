package ru.yandex.market.delivery.transport_manager.service;

import java.time.Instant;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.service.interwarehouse.transportation.InterwarehouseTaskCreator;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
class InterwarehouseTaskCreatorTest extends AbstractContextualTest {
    @Autowired
    private InterwarehouseTaskCreator taskCreator;

    @Test
    @DatabaseSetup("/repository/interwarehouse/regular_xdoc.xml")
    @DatabaseSetup(
        value = "/repository/task/no_tasks.xml",
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/interwarehouse/dbqueue/enrichment_task.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/interwarehouse/after/after_enrichment_start.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testEnrichmentLauncher() {
        clock.setFixed(Instant.parse("2021-02-23T23:00:00.00Z"), ZoneOffset.UTC);
        taskCreator.launchDraftInterwarehouse();
    }
}
