package ru.yandex.market.ocrm.module.taximl.impl.operations;

import java.util.Map;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.ocrm.module.taximl.ModuleTaximlTestConfiguration;
import ru.yandex.market.ocrm.module.taximl.Ticket;

@Transactional
@SpringJUnitConfig(classes = ModuleTaximlTestConfiguration.class)
public class NullifyTaximlSupportResultIfArchivedTest {
    @Inject
    private BcpService bcpService;

    @Inject
    private TicketTestUtils ticketTestUtils;
    private Ticket ticket;

    @BeforeEach
    public void createTicket() {
        ticket = ticketTestUtils.createTicket(
                Fqn.of("ticket$test"),
                Map.of(
                        Ticket.SERVICE, ticketTestUtils.createService24x7(),
                        Ticket.TAXIML_SUPPORT_RESULT, Map.of("key1", "value1", "key2", "value2")
                )
        );
    }

    @Test
    public void testTicketMovedInArchive() {
        // переводим тикет в один из архивных статусов
        bcpService.edit(ticket, Map.of(Ticket.STATUS, Ticket.STATUS_CLOSED));

        Assertions.assertTrue(ticket.getArchived());
        Assertions.assertNull(ticket.getTaximlSupportResult());
    }

    @Test
    public void testTicketMovedButNotInArchive() {
        var taximlSupportResult = ticket.getTaximlSupportResult();
        // переводим тикет в один из неархивных статусов
        bcpService.edit(ticket, Map.of(Ticket.STATUS, Ticket.STATUS_PROCESSING));

        Assertions.assertFalse(ticket.getArchived());
        Assertions.assertNotNull(ticket.getTaximlSupportResult());
        Assertions.assertEquals(taximlSupportResult, ticket.getTaximlSupportResult());
    }

    @Test
    public void testTicketWalkingAroundLifeCycle() {
        bcpService.edit(ticket, Map.of(Ticket.STATUS, Ticket.STATUS_PROCESSING));
        Assertions.assertNotNull(ticket.getTaximlSupportResult());
        bcpService.edit(ticket, Map.of(Ticket.STATUS, Ticket.STATUS_RESOLVED));
        Assertions.assertNotNull(ticket.getTaximlSupportResult());
        bcpService.edit(ticket, Map.of(Ticket.STATUS, Ticket.STATUS_CLOSED));
        Assertions.assertNull(ticket.getTaximlSupportResult());
    }
}
