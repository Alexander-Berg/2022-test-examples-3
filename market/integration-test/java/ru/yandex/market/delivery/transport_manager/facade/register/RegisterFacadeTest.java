package ru.yandex.market.delivery.transport_manager.facade.register;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
class RegisterFacadeTest extends AbstractContextualTest {
    @Autowired
    private RegisterFacade facade;

    @Test
    @DatabaseSetup({
        "/repository/facade/register_facade/fetch_registries.xml",
        "/repository/facade/register_facade/register_links.xml",
    })
    @ExpectedDatabase(
        value = "/repository/facade/register_facade/after/success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void setSuccess() {
        facade.registerSuccess("TMR1", "exId");
    }

    @Test
    @DatabaseSetup({
        "/repository/facade/register_facade/fetch_registries.xml",
        "/repository/facade/register_facade/register_links.xml"
    })
    @DatabaseSetup(value = "/repository/facade/register_facade/after/lgw_task.xml", type = DatabaseOperation.UPDATE)
    @DatabaseSetup(
        value = "/repository/task/no_tasks.xml",
        type = DatabaseOperation.DELETE_ALL,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/task/create_ticket_task.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/facade/register_facade/after/error.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void setError() {
        facade.registerError("TMR1", "Error message", "task_id");
    }

    @Test
    @DatabaseSetup({
        "/repository/facade/register_facade/fetch_registries.xml",
        "/repository/facade/register_facade/register_links.xml"
    })
    @DatabaseSetup(
        value = "/repository/task/no_tasks.xml",
        type = DatabaseOperation.DELETE_ALL,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/task/create_ticket_task.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/facade/register_facade/after/error.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void setErrorMissingTaskId() {
        facade.registerError("TMR1", "Error message", null);
    }
}
