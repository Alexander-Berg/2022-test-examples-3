package ru.yandex.market.delivery.transport_manager.service.startrek;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.EntityType;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.queue.task.ticket.StartrekErrorTicketDto;
import ru.yandex.market.delivery.transport_manager.queue.task.ticket.StartrekErrorType;
import ru.yandex.market.delivery.transport_manager.service.ticket.StartrekDtoConverter;
import ru.yandex.market.delivery.transport_manager.service.ticket.service.StEntityErrorTicketService;

@DatabaseSetup(value = "/repository/health/dbqueue/empty.xml", connection = "dbUnitDatabaseConnectionDbQueue")
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
public class StEntityErrorTicketServiceTest extends AbstractContextualTest {

    @Autowired
    private StartrekDtoConverter converter;

    @Autowired
    private StEntityErrorTicketService ticketCreationService;

    @Test
    @ExpectedDatabase(
        value = "/repository/startrek/dbqueue/transportation_task_ticket.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void transportationTaskTicket() {
        StartrekErrorTicketDto dto = new StartrekErrorTicketDto()
            .setMessage("error")
            .setErrorType(StartrekErrorType.VALIDATION_ERROR);

        ticketCreationService.createErrorTicket(EntityType.TRANSPORTATION_TASK, 1L, dto);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/startrek/dbqueue/transportation_ticket.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void transportationTicket() {
        StartrekErrorTicketDto dto = new StartrekErrorTicketDto()
            .setMessage("error")
            .setErrorType(StartrekErrorType.REGISTER_ERROR);

        ticketCreationService.createErrorTicket(EntityType.TRANSPORTATION, 1L, dto);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/startrek/dbqueue/movement_ticket.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void movementTicket() {
        StartrekErrorTicketDto dto = new StartrekErrorTicketDto()
            .setMessage("TPL error")
            .setErrorType(StartrekErrorType.MOVEMENT_ERROR);

        ticketCreationService.createErrorTicket(EntityType.MOVEMENT, 1L, dto);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/startrek/dbqueue/transportation_ticket_new_method.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void transportationTicketNewMethods() {
        ticketCreationService.createErrorTicket(
            EntityType.TRANSPORTATION,
            1L,
            converter.toRequestErrorDto(
                unit(123L, TransportationUnitType.OUTBOUND, 345L, 145L, "000123"),
                "https://lms-admin.tst.market.yandex-team.ru/lgw/client-tasks?entityId=345"
            )
        );
        ticketCreationService.createErrorTicket(
            EntityType.TRANSPORTATION,
            2L,
            converter.toRegistryErrorDto(
                unit(10L, TransportationUnitType.INBOUND, 20L, 147L, "000987"),
                "https://lms-admin.tst.market.yandex-team.ru/lgw/client-tasks?entityId=20"
            )
        );
    }

    private TransportationUnit unit(
        long id,
        TransportationUnitType type,
        long requestId,
        long partnerId,
        String externalId
    ) {
        return new TransportationUnit()
            .setId(id)
            .setType(type)
            .setRequestId(requestId)
            .setPartnerId(partnerId)
            .setExternalId(externalId);
    }
}
