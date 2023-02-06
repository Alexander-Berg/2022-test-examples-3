package ru.yandex.market.delivery.transport_manager.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.task.order.deletion.OrderDeletionProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.deletion.TransportationDeletionProducer;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
class DatabaseCleanUpServiceTest extends AbstractContextualTest {

    public static final int MONTHS_TO_EXPIRE = 3;

    @Autowired
    private DatabaseCleanUpService databaseCleanUpService;

    @Autowired
    private TransportationDeletionProducer transportationDeletionProducer;

    @Autowired
    private OrderDeletionProducer orderDeletionProducer;

    @BeforeEach
    void init() {
        clock.setFixed(Instant.parse("2021-01-21T19:00:00.00Z"), ZoneOffset.UTC);
    }

    @Test
    @DatabaseSetup(
        value = {
            "/repository/transportation/transportations_with_full_metadata.xml",
            "/repository/transportation/register_meta.xml"
        }
    )
    @DatabaseSetup(
        value = "/repository/task/no_tasks.xml",
        type = DatabaseOperation.DELETE_ALL,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/task/delete_transportations_task.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void deleteOutdatedTransportations() {
        databaseCleanUpService.deleteOutdatedTransportations(MONTHS_TO_EXPIRE);
        Mockito.verify(transportationDeletionProducer).enqueue(
            Mockito.argThat(t -> t.containsAll(List.of(1L, 2L))),
            Mockito.eq(3)
        );
    }

    @Test
    @DatabaseSetup(value = {
        "/repository/transportation_task/transportation_tasks.xml",
    })
    @DatabaseSetup(
        value = "/repository/task/no_tasks.xml",
        type = DatabaseOperation.DELETE_ALL,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/task/delete_transportation_tasks_task.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void deleteOutdatedTransportationTasks() {
        clock.setFixed(Instant.parse("2022-03-18T20:00:00.0Z"), ZoneOffset.UTC);
        databaseCleanUpService.deleteOutdatedTransportationTasks(MONTHS_TO_EXPIRE);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DatabaseSetup(
        value = {
            "/repository/order_route/orders.xml",
            "/repository/order_route/routes.xml"
        }
    )
    @DatabaseSetup(
        value = "/repository/order_route/update_date.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/repository/task/no_tasks.xml",
        type = DatabaseOperation.DELETE_ALL,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/task/delete_orders_task.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void deleteOutdatedOrders() {
        clock.setFixed(Instant.parse("2022-02-11T19:00:00.00Z"), ZoneOffset.UTC);
        databaseCleanUpService.deleteOutdatedOrders(MONTHS_TO_EXPIRE, 100);
        ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(orderDeletionProducer).enqueue(listCaptor.capture());
        softly.assertThat(listCaptor.getValue()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DatabaseSetup(
        value = {"/repository/dbqueue/logs.xml"},
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/dbqueue/after/after_delete.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"

    )
    void deleteDbqueueLogs() {
        clock.setFixed(Instant.parse("2021-09-15T04:01:00.00Z"), ZoneOffset.UTC);
        databaseCleanUpService.cleanDbQueueLog(14, 5000);
    }
}
