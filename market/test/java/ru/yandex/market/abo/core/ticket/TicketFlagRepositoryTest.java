package ru.yandex.market.abo.core.ticket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.ticket.flag.TicketFlag;
import ru.yandex.market.abo.core.ticket.flag.TicketFlagRepository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author kukabara
 */
public class TicketFlagRepositoryTest extends EmptyTest {
    @Autowired
    private TicketFlagRepository ticketFlagRepository;

    @Test
    public void testTicketFlag() throws Exception {
        long hypId = -RND.nextInt(1000);
        String flagName = "asb";
        assertTrue(ticketFlagRepository.findByHypIdAndName(hypId, flagName).isEmpty());

        TicketFlag flag = new TicketFlag(hypId, flagName);
        TicketFlag save = ticketFlagRepository.save(flag);
        assertNotNull(save.getId());

        assertFalse(ticketFlagRepository.findByHypIdAndName(hypId, flagName).isEmpty());
    }
}
