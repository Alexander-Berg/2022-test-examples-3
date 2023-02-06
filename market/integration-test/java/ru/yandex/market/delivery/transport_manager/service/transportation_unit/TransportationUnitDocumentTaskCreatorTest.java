package ru.yandex.market.delivery.transport_manager.service.transportation_unit;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

@DatabaseSetup({
    "/repository/transportation/multiple_transportations_deps.xml",
    "/repository/transportation/multiple_transportations.xml"
})
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
@DatabaseSetup(value = "/repository/health/dbqueue/empty.xml", connection = "dbUnitDatabaseConnectionDbQueue")
public class TransportationUnitDocumentTaskCreatorTest extends AbstractContextualTest {
    @Autowired
    private TransportationUnitDocumentsTaskCreator taskCreator;

    @Test
    @DatabaseSetup("/repository/transportation_unit_documents/documents.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_unit_documents/after/after_task_creation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/transportation_unit_documents/after/send_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void createTasksTest() {
        taskCreator.createTasks();
    }
}
