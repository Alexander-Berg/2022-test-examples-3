package ru.yandex.market.logistics.management.service.combinator;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractContextualTest;

public class YtSyncLogisticsServicesAvailabilityTest extends AbstractContextualTest {

    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/yt_logistics_services.xml",
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/yt_logistics_services_availability_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void logisticServiceAdded() {
        //check only database state
    }
}
