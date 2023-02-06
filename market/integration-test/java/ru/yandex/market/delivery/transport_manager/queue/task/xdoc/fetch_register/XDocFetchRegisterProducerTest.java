package ru.yandex.market.delivery.transport_manager.queue.task.xdoc.fetch_register;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.dto.xdoc.XDocRequestStatus;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"}
)
class XDocFetchRegisterProducerTest extends AbstractContextualTest {
    @Autowired
    private XDocFetchRegisterProducer producer;

    @DatabaseSetup(
        value = "/repository/task/no_tasks.xml",
        type = DatabaseOperation.DELETE_ALL,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/task/xdoc_fetch_register.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    void testEnqueue() {
        producer.produce(1L, true, XDocRequestStatus.ACCEPTED_BY_SERVICE, null, 123L);
    }
}
