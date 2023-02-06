package ru.yandex.market.delivery.transport_manager.service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationSubstatus;
import ru.yandex.market.delivery.transport_manager.queue.task.ticket.StartrekErrorTicketDto;
import ru.yandex.market.delivery.transport_manager.queue.task.ticket.StartrekErrorType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
class TransportationStatusServiceTest extends AbstractContextualTest {
    @Autowired
    private TransportationStatusService transportationStatusService;

    @Autowired
    private TransportationMapper transportationMapper;

    private static final StartrekErrorTicketDto FAILED_TRANSPORTATION_DTO =
        new StartrekErrorTicketDto()
            .setMessage("Error")
            .setErrorType(StartrekErrorType.REGISTER_ERROR)
            .addTags(List.of("tag1", "tag2"));

    @BeforeEach
    void init() {
        clock.setFixed(Instant.parse("2021-06-29T19:00:00.00Z"), ZoneId.of("Europe/Moscow"));
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_deps.xml",
        "/repository/transportation/multiple_transportations.xml"
    })
    void testSetStatus() {
        transportationStatusService.setTransportationStatus(List.of(2L), TransportationStatus.CHECK_PREPARED);

        var ids = transportationMapper.findIds(TransportationStatus.CHECK_PREPARED, 10);
        softly.assertThat(ids).contains(2L);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_deps.xml",
        "/repository/transportation/multiple_transportations.xml"
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
    void testSetErrorStatusSingle() {
        transportationStatusService.setTransportationErrorStatus(
            2L,
            TransportationStatus.ERROR,
            FAILED_TRANSPORTATION_DTO
        );

        var ids = transportationMapper.findIds(TransportationStatus.ERROR, 5);
        softly.assertThat(ids).containsOnly(2L);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_deps.xml",
        "/repository/transportation/multiple_transportations.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation/after/updated_transportation_history.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testSetStatus_updatesStatusHistory() {
        transportationStatusService.setTransportationStatus(List.of(2L, 4L), TransportationStatus.RECEIVED);

        var ids = transportationMapper.findIds(TransportationStatus.RECEIVED, 2);
        softly.assertThat(ids).containsOnly(2L, 4L);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_deps.xml",
        "/repository/transportation/multiple_transportations.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation/after/after_substatus.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testSetStatusWithSubStatus() {
        transportationStatusService.setTransportationStatus(
            1L,
            TransportationStatus.CANCELLED,
            TransportationSubstatus.MOVEMENT_CREATION_FAILED
        );
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_deps.xml",
        "/repository/transportation/multiple_transportations.xml"
    })
    void testSetStatusWithWrongSubStatus() {
        softly.assertThatThrownBy(() ->
            transportationStatusService.setTransportationStatus(
                1L,
                TransportationStatus.SCHEDULED,
                TransportationSubstatus.MOVEMENT_CREATION_FAILED
            ));
    }
}
