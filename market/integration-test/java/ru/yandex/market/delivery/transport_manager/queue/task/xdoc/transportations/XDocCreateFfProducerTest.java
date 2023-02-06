package ru.yandex.market.delivery.transport_manager.queue.task.xdoc.transportations;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.task.xdoc.transportations.ff.XDocCreateFfProducer;
import ru.yandex.market.ff.client.dto.RequestStatusChangeDto;
import ru.yandex.market.ff.client.enums.RequestStatus;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"}
)
class XDocCreateFfProducerTest extends AbstractContextualTest {
    @Autowired
    private XDocCreateFfProducer producer;

    @DatabaseSetup(
        value = "/repository/task/no_tasks.xml",
        type = DatabaseOperation.DELETE_ALL,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/task/xdoc_create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @Test
    void testQnqueue() {
        producer.enqueue(
            new RequestStatusChangeDto()
                .setRequestId(1024L)
                .setNewStatus(RequestStatus.VALIDATED)
        );
    }
}
