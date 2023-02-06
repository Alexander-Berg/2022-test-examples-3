package ru.yandex.market.abo.core.premod;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.premod.model.PremodCheckType;
import ru.yandex.market.abo.core.premod.model.PremodTicket;
import ru.yandex.market.abo.core.premod.model.PremodTicketStatus;
import ru.yandex.market.abo.core.premod.service.PremodRepo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author imelnikov
 */
public class PremodTicketServiceTest extends EmptyTest {
    private static final long SHOP_ID = 774;

    @Autowired
    PremodRepo.PremodTicketRepo premodTicketRepo;

    @Autowired
    PremodTicketService service;

    @Test
    void testRepo() {
        PremodTicket t = new PremodTicket(0, 0, PremodCheckType.SERP);
        t.setStatus(PremodTicketStatus.IN_PROGRESS);
        premodTicketRepo.save(t);

        assertFalse(premodTicketRepo.findAllByCheckTypeIdAndStatusIdIn(PremodCheckType.SERP.getId(),
                Arrays.asList(PremodTicketStatus.IN_PROGRESS.getId())).isEmpty());

        assertTrue(premodTicketRepo.findAllByCheckTypeIdAndStatusIdIn(PremodCheckType.CPC_PREMODERATION.getId(),
                Arrays.asList(PremodTicketStatus.IN_PROGRESS.getId())).isEmpty());

        assertTrue(premodTicketRepo.findAllByCheckTypeIdNotAndStatusIdIn(PremodCheckType.SERP.getId(),
                Arrays.asList(PremodTicketStatus.IN_PROGRESS.getId())).isEmpty());

    }

    @Test
    void notFinished() {
        PremodTicket ticket = new PremodTicket(0, 0, PremodCheckType.CPC_PREMODERATION);
        ticket.setStatus(PremodTicketStatus.IN_PROGRESS);
        service.save(ticket);

        ticket.setStatus(PremodTicketStatus.PASS);
        service.ensureTicketNotFinished(ticket.getId());
    }

    @Test
    void previousTicketTest() {
        List<PremodTicket> tickets = IntStream.range(0, 5)
                .mapToObj(i -> createTicket())
                .collect(Collectors.toList());
        PremodTicket ticket = tickets.get(3);
        PremodTicket prevTicket = tickets.get(2);
        assertEquals(prevTicket.getId(), service.getPreviousTicket(ticket).getId());
    }

    @Test
    void previousPassTest() {
        var ticket = createTicket();
        flushAndClear();
        assertFalse(service.existsPreviousPassTicket(ticket));

        ticket.setStatus(PremodTicketStatus.PASS);
        service.save(ticket);
        ticket = createTicket();
        flushAndClear();
        assertTrue(service.existsPreviousPassTicket(ticket));
    }

    private PremodTicket createTicket() {
        PremodTicket ticket = new PremodTicket(SHOP_ID, 0, PremodCheckType.CPC_PREMODERATION);
        ticket.setShopId(SHOP_ID);
        return premodTicketRepo.save(ticket);
    }
}
