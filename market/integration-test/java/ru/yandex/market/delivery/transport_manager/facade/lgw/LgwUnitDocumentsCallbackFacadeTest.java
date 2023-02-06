package ru.yandex.market.delivery.transport_manager.facade.lgw;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.model.dto.PutDocumentsErrorDto;
import ru.yandex.market.delivery.transport_manager.model.dto.PutDocumentsSuccessDto;

@DatabaseSetup({
    "/repository/transportation/multiple_transportations_deps.xml",
    "/repository/transportation/multiple_transportations.xml",
    "/repository/transportation_unit_documents/after/after_status_update.xml"
})
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
public class LgwUnitDocumentsCallbackFacadeTest extends AbstractContextualTest {
    @Autowired
    private LgwUnitDocumentsCallbackFacade documentsCallbackFacade;

    @Test
    @ExpectedDatabase(
        value = "/repository/transportation_unit_documents/after/after_error_received.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/startrek/dbqueue/documents_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void testErrorForTmuId() {
        PutDocumentsErrorDto errorDto = new PutDocumentsErrorDto(1L, null, null, 5L, null);
        documentsCallbackFacade.putError("TMU4", errorDto);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/transportation_unit_documents/after/after_success_received.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSuccessForTmuId() {
        PutDocumentsSuccessDto successDto = new PutDocumentsSuccessDto(null, "5", 6L);
        documentsCallbackFacade.putSuccess("TMU4", successDto);
    }

    @Test
    @DatabaseSetup(
        value = "/repository/transportation/update/set_request_id_for_4.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/repository/transportation_unit_documents/after/after_success_received.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSuccessForFfWfId() {
        PutDocumentsSuccessDto successDto = new PutDocumentsSuccessDto(null, "5", 6L);
        documentsCallbackFacade.putSuccess("777", successDto);
    }

    @Test
    @DatabaseSetup(
        value = "/repository/transportation/update/set_request_id_for_4.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/repository/transportation_unit_documents/after/after_error_received.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/startrek/dbqueue/documents_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void testErrorForFfWfId() {
        PutDocumentsErrorDto errorDto = new PutDocumentsErrorDto(1L, null, null, 5L, null);
        documentsCallbackFacade.putError("777", errorDto);
    }
}
