package ru.yandex.market.delivery.transport_manager.service.startrek;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.queue.task.dropoff.DropoffReturnTicketCreator;
import ru.yandex.market.delivery.transport_manager.repository.mappers.StartrekIssueMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.service.TmPropertyService;
import ru.yandex.market.delivery.transport_manager.service.ticket.service.StReturnDropoffTicketService;

public class DropoffReturnTicketCreatorTest extends AbstractContextualTest {
    @Autowired
    private DropoffReturnTicketCreator ticketCreator;

    @Autowired
    private StReturnDropoffTicketService ticketService;

    @Autowired
    private TransportationMapper transportationMapper;

    @Autowired
    private StartrekIssueMapper startrekIssueMapper;

    @Autowired
    private TmPropertyService tmPropertyService;

    @BeforeEach
    void init() {
        Mockito.when(tmPropertyService.getBoolean(TmPropertyKey.UPDATE_DROPOFF_RETURN_TICKETS))
            .thenReturn(true);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DatabaseSetup("/repository/startrek/choose_ready_return_dropoffs.xml")
    void test() throws IOException {
        clock.setFixed(Instant.parse("2020-09-28T12:00:00Z"), ZoneId.systemDefault());
        ticketCreator.createOrUpdateCourierTickets(clock);

        ArgumentCaptor<List> transportationCaptor = ArgumentCaptor.forClass(List.class);

        Mockito.verify(ticketService).createCourierInfoTicket(
            Mockito.eq(LocalDate.of(2020, 9, 28)),
            transportationCaptor.capture()
        );

        softly.assertThat(transportationCaptor.getValue())
            .containsExactlyInAnyOrder(transportationMapper.getById(1L), transportationMapper.getById(2));

        Mockito.verify(ticketService).createCourierInfoTicket(
            LocalDate.of(2020, 9, 29),
            transportationMapper.getByIds(Set.of(6L))
        );

        Mockito.verify(ticketService).updateCourierInfoTicket(
            startrekIssueMapper.getById(2L),
            128L,
            LocalDate.of(2020, 9, 28),
            transportationMapper.getByIds(Set.of(7L))
        );
    }
}
