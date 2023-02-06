package ru.yandex.market.delivery.transport_manager.service.interwarehouse.transportation_task;

import java.time.Instant;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.service.interwarehouse.transportation.RegularInterwarehouseCancellationService;

@DatabaseSetup(value = "/repository/health/dbqueue/empty.xml", connection = "dbUnitDatabaseConnectionDbQueue")
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
public class RegularInterwarehouseCancellationServiceTest extends AbstractContextualTest {
    @Autowired
    private RegularInterwarehouseCancellationService cancellationService;

    @Test
    @DatabaseSetup("/repository/transportation/not_approved_in_time.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/dbqueue/cancel_transportation_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void test() {
        clock.setFixed(Instant.parse("2021-11-02T22:15:00Z"), ZoneOffset.UTC);
        cancellationService.cancelNotApprovedInTime(clock);
    }
}
