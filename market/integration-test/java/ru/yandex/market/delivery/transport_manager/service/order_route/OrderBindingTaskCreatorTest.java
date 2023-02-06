package ru.yandex.market.delivery.transport_manager.service.order_route;

import java.time.Instant;
import java.time.ZoneId;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.service.order.OrderBindingTaskCreator;

@DatabaseSetup(
    value = "/repository/health/dbqueue/empty.xml",
    connection = "dbUnitDatabaseConnectionDbQueue"
)
@DatabaseSetup("/repository/order_route/orders.xml")
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
public class OrderBindingTaskCreatorTest extends AbstractContextualTest {
    @Autowired
    private OrderBindingTaskCreator taskCreator;

    @Test
    @DatabaseSetup("/repository/transportation/all_kinds_of_transportation.xml")
    @DatabaseSetup("/repository/order_route/unbound_for_date.xml")
    @ExpectedDatabase(
        value = "/repository/order_route/after/binding_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void test() {
        clock.setFixed(Instant.parse("2021-11-09T10:00:00Z"), ZoneId.systemDefault());
        taskCreator.createBindingTasks(clock);
    }
}
