package ru.yandex.market.abo.core.startrek;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.startrek.model.StartrekTicket;
import ru.yandex.market.abo.core.startrek.model.StartrekTicketFactory;
import ru.yandex.market.abo.core.startrek.model.StartrekTicketReason;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author artemmz
 *         created on 16.03.17.
 */
public class StartrekTicketManagerRealTest extends EmptyTest {
    @Autowired
    private StartrekTicketManager startrekTicketManager;
    @Autowired
    private StartrekTicketRepository startrekTicketRepository;

    @Test
    @Disabled("creates real ticket in Startrek")
    public void createTicket() throws Exception {
        StartrekTicketFactory ticketFactory = StartrekTicketFactory.newBuilder()
                .sourceId(0)
                .reason(StartrekTicketReason.SHOP_CHANGED_CATEGORIES)
                .summary("TEST summary")
                .description("keep calm, market will be fine\nwow, new line!").build();

        StartrekTicket ticket = startrekTicketManager.createTicket(ticketFactory).getAboStTicket();
        StartrekTicket dbTicket = startrekTicketRepository.findLastTicket(
                ticket.getSourceId(), ticket.getStartrekTicketReason());
        assertEquals(dbTicket, ticket);
        System.out.println(dbTicket);
    }
}