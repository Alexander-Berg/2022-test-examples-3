package ru.yandex.market.abo.core.ticket;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.core.problem.pinger.PingerProblemType;
import ru.yandex.market.abo.core.ticket.model.CheckMethod;
import ru.yandex.market.abo.core.ticket.model.Ticket;
import ru.yandex.market.abo.core.ticket.model.TicketStatus;
import ru.yandex.market.abo.core.ticket.model.TicketTag;
import ru.yandex.market.abo.gen.model.GenId;
import ru.yandex.market.abo.gen.model.Hypothesis;
import ru.yandex.market.abo.util.FakeUsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author imelnikov
 */
public class TicketServiceTest extends AbstractCoreHierarchyTest {

    @BeforeEach
    public void init() {
        openMocks(this);
    }

    @Test
    public void ticketHistory() {
        final long UID = FakeUsers.TICKET_CANCELLATOR_BY_TIME.getId();
        Hypothesis hypothesis = new Hypothesis(-1L, 0, 1, null, 0, 0, null);
        hypothesisService.createHypothesis(hypothesis);
        TicketTag tag = tagService.createTag(UID);


        Ticket t = new Ticket(hypothesis, null, 0, CheckMethod.DEFAULT);
        t.setStatus(TicketStatus.NEW);
        t.setCreationTime(DateUtils.addDays(new Date(), -10));
        ticketService.saveTicket(t, tag);

        String text = RandomStringUtils.randomAlphanumeric(4001);
        t.setUserComment(text);
        ticketService.addTicketComment(t.getId(), "vanek", text);

        t.setCheckMethod(CheckMethod.BASKET);
        t.setStatus(TicketStatus.ANSWER_WAITING);
        ticketService.saveTicket(t, tagService.createTag(UID));

        assertTrue(ticketService.hasHistoryForTicket(t.getId(), UID));
        assertFalse(ticketService.hasHistoryForTicket(t.getId(), UID - 1));

        assertEquals(2, ticketService.loadAllTicketHistory(t.getId()).size());
    }

    @Test
    void testLoadNewPingerTickets() {
        Set<Long> expected = IntStream.range(0, 3)
                .mapToLong(i -> createTicket(GenId.PRICE_PINGER_GEN, TicketStatus.NEW))
                .boxed()
                .collect(Collectors.toSet());

        IntStream.range(0, 3).forEach(i -> createTicket(GenId.RANDOM_GEN, TicketStatus.NEW));
        IntStream.range(0, 3).forEach(i -> createTicket(GenId.PRICE_PINGER_GEN, TicketStatus.CANCELED));

        entityManager.flush();

        List<Ticket> tickets = ticketService.loadNewTicketsFromHypGenerator(PingerProblemType.PRICE.getGenId());
        assertEquals(expected, tickets.stream().map(Ticket::getId).collect(Collectors.toSet()));
    }

    @Test
    public void getHypothesis() {
        int genId = 1;
        createTicket(1L, genId);
        entityManager.flush();
        entityManager.clear();

        Ticket loaded = ticketService.loadNewTicketsCreatedBefore(DateUtil.addDay(new Date(), 1)).get(0);
        assertEquals(genId, loaded.getHypothesis().getGeneratorId());
    }

    @Test
    public void testLoadTicketsForOfferAbsenceCheck() {
        var newRegularTicket = createTicket(1, TicketStatus.NEW);
        createTicket(1, TicketStatus.NEW, CheckMethod.AUTO_ORDER);
        createTicket(1, TicketStatus.FINISHED);
        createTicket(GenId.DSBS_CANCELLED_ORDER, TicketStatus.NEW);
        var tickets = ticketService.loadTicketsForOfferAbsenceCheck();
        assertEquals(1, tickets.size());
        assertEquals(newRegularTicket, tickets.get(0).getId());
    }

    private long createTicket(int genId, TicketStatus status) {
        return createTicket(genId, status, CheckMethod.DEFAULT);
    }

    private long createTicket(int genId, TicketStatus status, CheckMethod checkMethod) {
        Hypothesis hypothesis = new Hypothesis(-1L, 0, genId, null, 0, 0, "");
        hypothesisService.createHypothesis(hypothesis);

        TicketTag tag = tagService.createTag(1);
        Ticket t = new Ticket(hypothesis, null, 0, checkMethod);
        t.setStatus(status);
        ticketService.saveTicket(t, tag);

        return hypothesis.getId();
    }
}
