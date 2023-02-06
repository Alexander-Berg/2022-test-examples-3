package ru.yandex.market.delivery.transport_manager.service;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.master.TransportationMasterProducer;

@DatabaseSetup(value = "/repository/task/no_tasks.xml", connection = "dbUnitDatabaseConnectionDbQueue")
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
public class TransportationMasterTransactionIsolatorTest extends AbstractContextualTest {

    @Autowired
    private TransportationMasterTransactionIsolator transactionIsolator;

    @Autowired
    private TransportationMasterProducer masterProducer;

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_shipment_deps.xml",
        "/repository/transportation/transportation_shipment.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation/transportation_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/task/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void testBatchRollbackOnException() {
        Mockito.doThrow(NullPointerException.class).when(masterProducer).enqueue(Mockito.eq(2L));
        softly.assertThatThrownBy(() -> transactionIsolator.enqueue(List.of(1L, 2L)));
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_shipment_deps.xml",
        "/repository/transportation/transportation_shipment.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation/after/after_transportation_master_task_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/transportation/after/after_transportation_master_task_created_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void testNoUpdateIfNotScheduled() {
        softly.assertThat(transactionIsolator.enqueue(List.of(1L, 2L, 3L))).isEqualTo(2);
    }
}
