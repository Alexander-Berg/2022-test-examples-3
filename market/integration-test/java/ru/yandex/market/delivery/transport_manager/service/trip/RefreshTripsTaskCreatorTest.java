package ru.yandex.market.delivery.transport_manager.service.trip;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

@DatabaseSetup(
    value = {
        "/repository/route/route.xml",
        "/repository/route_schedule/before/schedules_with_different_statuses.xml"
    }
)
@DatabaseSetup(
    value = "/repository/health/dbqueue/empty.xml",
    connection = "dbUnitDatabaseConnectionDbQueue"
)
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
class RefreshTripsTaskCreatorTest extends AbstractContextualTest {

    @Autowired
    private RefreshTripsTaskCreator refreshTripsTaskCreator;

    @ExpectedDatabase(
        value = "/repository/route_schedule/after/refresh_trips_tasks.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createTasks() {
        refreshTripsTaskCreator.createTasks();
    }
}
