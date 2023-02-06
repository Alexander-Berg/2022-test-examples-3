package ru.yandex.market.delivery.transport_manager.service.startrek;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.queue.task.ticket.update.TicketUpdateDto;
import ru.yandex.market.delivery.transport_manager.queue.task.ticket.update.TicketUpdateProducer;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.service.TmPropertyService;
import ru.yandex.market.delivery.transport_manager.service.ticket.service.StInterwarehouseTicketService;
import ru.yandex.market.delivery.transport_manager.service.ticket.tracker_entity.TicketStatus;

@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
public class StInterwarehouseTicketServiceTest extends AbstractContextualTest {
    @Autowired
    private StInterwarehouseTicketService ticketService;

    @Autowired
    private TmPropertyService tmPropertyService;

    @Autowired
    private TransportationMapper transportationMapper;

    @MockBean
    private TicketUpdateProducer ticketUpdateProducer;

    @Test
    @DatabaseSetup("/repository/facade/transportation_booking_slot_task/new_transportation_with_fields.xml")
    @ExpectedDatabase(
        value = "/repository/startrek/dbqueue/interwarehouse_ticket.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void test() {
        Mockito.when(tmPropertyService.getBoolean(TmPropertyKey.ENABLE_AXAPTA_TICKET_CREATION))
            .thenReturn(true);

        Mockito.when(tmPropertyService.getBoolean(TmPropertyKey.ENABLE_AXAPTA_TICKET_CREATION_CUSTOM_TAGS))
            .thenReturn(true);

        ticketService.createTicketForNewInterwarehouse(transportationMapper.getById(1));

    }

    @Test
    @DatabaseSetup(value = {
        "/repository/facade/transportation_booking_slot_task/new_transportation_with_fields.xml",
        "/repository/startrek/after/interwarehouse_ticket_created.xml"
    })
    void testUpdate() {
        Mockito.when(tmPropertyService.getBoolean(TmPropertyKey.ENABLE_AXAPTA_TICKET_STATUS_UPDATE))
            .thenReturn(true);

        ticketService.setTicketStatus(transportationMapper.getById(1), TicketStatus.WITHDRAWAL_CREATED);
        Mockito.verify(ticketUpdateProducer, Mockito.times(1)).enqueue(
            new TicketUpdateDto()
                .setIssueId(1L)
                .setTicketStatus(TicketStatus.WITHDRAWAL_CREATED)
                .setComment("Обновление статуса")
        );
    }
}
