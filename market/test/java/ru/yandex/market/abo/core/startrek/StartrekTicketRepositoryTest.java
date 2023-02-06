package ru.yandex.market.abo.core.startrek;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.startrek.model.StartrekTicket;
import ru.yandex.market.abo.core.startrek.model.StartrekTicketReason;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author artemmz
 *         created on 16.03.17.
 */
public class StartrekTicketRepositoryTest extends EmptyTest {
    @Autowired
    private StartrekTicketRepository startrekTicketRepository;

    @Test
    public void testRepo() throws Exception {
        StartrekTicket startrekTicket = initStTicket();
        startrekTicketRepository.save(startrekTicket);
        StartrekTicket dbTicket = startrekTicketRepository.findByIdOrNull(startrekTicket.getId());
        assertEquals(startrekTicket, dbTicket);
    }

    @Test
    public void testFindBySourceIdAndReason() throws Exception {
        StartrekTicket startrekTicket = initStTicket();
        startrekTicketRepository.save(startrekTicket);
        StartrekTicket dbTicket = startrekTicketRepository.findLastTicket(
                startrekTicket.getSourceId(), startrekTicket.getStartrekTicketReason());
        assertEquals(startrekTicket, dbTicket);
    }

    @Test
    public void testInsertNullable() {
        StartrekTicket ticket = initStTicket();
        ticket.setStartrekTicketReason(null);
        assertThrows(Exception.class, () -> {
            startrekTicketRepository.save(ticket);
            startrekTicketRepository.flush();
        });
    }

    private StartrekTicket initStTicket() {
        StartrekTicket startrekTicket = new StartrekTicket();
        startrekTicket.setSourceId(RND.nextLong());
        startrekTicket.setTicketName("MARKETASSESSOR/100500");
        startrekTicket.setStartrekTicketReason(StartrekTicketReason.SHOP_CHANGED_CATEGORIES);
        return startrekTicket;
    }
}
