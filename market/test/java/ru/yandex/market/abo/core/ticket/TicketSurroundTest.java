package ru.yandex.market.abo.core.ticket;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.problem.model.ProblemStatus;
import ru.yandex.market.abo.core.problem.model.ProblemTypeId;
import ru.yandex.market.abo.core.ticket.model.CheckMethod;
import ru.yandex.market.abo.core.ticket.model.Ticket;
import ru.yandex.market.abo.core.ticket.model.TicketStatus;
import ru.yandex.market.abo.core.ticket.model.TicketTag;
import ru.yandex.market.abo.core.ticket.repository.TicketRepo;
import ru.yandex.market.abo.gen.model.Hypothesis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author mixey
 * @date 21.02.2008
 */
public class TicketSurroundTest extends AbstractCoreHierarchyTest {

    private static final int GEN_ID = 1;
    private static final long SHOP_ID = 774;

    @Autowired
    TicketRepo ticketRepo;

    @Test
    @Transactional
    public void testOfferDbServiceLoadOfferByHypId() {
        long hypId = createTicket(SHOP_ID, GEN_ID);
        ticketRepo.flush();

        Offer offer = offerDbService.loadOfferByHypId(hypId);
        assertNotNull(offer);
    }

    @Test
    public void testOfferStateManager() {
        Offer offer = findOffer(SHOP_ID);
        offerDbService.storeOffer(offer);
        TicketTag tag = tagService.createTag(0L);
        offerStateManager.rememberCurrentOfferState(offer, tag.getId());
    }

    @Test
    public void testProblemService() {
        long ticketId = createTicket(SHOP_ID, GEN_ID);
        String publicComment = "PublicComment" + UUID.randomUUID();
        Problem problem = Problem.newBuilder()
                .ticketId(ticketId)
                .problemTypeId(43)
                .publicComment(publicComment)
                .status(ProblemStatus.NEW)
                .build();
        TicketTag tag = tagService.createTag(0L);
        problemService.saveProblem(problem, tag);
        Long id = problem.getId();

        Problem loadedById = problemService.loadProblem(id);
        assertEquals(problem, loadedById);
        assertEquals(publicComment, loadedById.getPublicComment());

        List<Problem> loadedByHypId = problemService.loadProblemsByTicketId(ticketId);
        assertEquals(loadedByHypId.size(), 1);
        assertEquals(problem, loadedByHypId.get(0));

        loadedById.setStatus(ProblemStatus.REJECTED);
        publicComment = "PublicComment" + UUID.randomUUID();
        loadedById.setPublicComment(publicComment);
        tag = tagService.createTagAndRememberOfferStateByHypId(ticketId, 0L);
        problemService.saveProblem(loadedById, tag);
        Long id2 = loadedById.getId();
        assertEquals(id, id2);
        Problem loaded = problemService.loadProblem(id);
        assertEquals(loadedById, loaded);
        assertEquals(publicComment, loaded.getPublicComment());
    }

    /**
     * Check that we don't load problems if method in {@link ru.yandex.market.abo.core.ticket.repository.TicketRepo}
     * doesn't annotated by {@link org.springframework.data.jpa.repository.EntityGraph}.
     * In first we flush changes to database.
     * In second we clear context to load ticket directly from db, not from jpa cache.
     */
    @Test
    public void testLoadTicketWithoutProblems() {
        long hypId = createTicketWithProblem();

        Ticket loaded = ticketService.loadTicketById(hypId);
        assertFalse(isInitialized(loaded.getProblems()));
    }

    /**
     * Check that we load ticket with problems when it's needed.
     */
    @Test
    public void testLoadTicketWithProblems() {
        createTicketWithProblem();

        Ticket loaded = ticketService.loadTicketsWithProblems(SHOP_ID, 30).stream()
                .findFirst().orElseThrow(RuntimeException::new);
        assertTrue(isInitialized(loaded.getProblems()));
    }

    @Test
    public void testTicketService() {
        Hypothesis h = new Hypothesis(SHOP_ID, 0, GEN_ID, "For unit test purposes", 1, 0, null);
        hypothesisService.createHypothesis(h);

        Offer offer = findOffer(SHOP_ID);
        offerDbService.storeOffer(offer);
        TicketTag tag = tagService.createTagAndRememberOfferState(offer, 0L);
        Ticket ticket = new Ticket(h, offer.getId(), 213, CheckMethod.BASKET);
        ticketService.saveTicket(ticket, tag);

        Ticket loaded = ticketService.loadTicketById(ticket.getId());
        assertEquals(ticket, loaded);

        ticket.setStatus(TicketStatus.FINISHED);
        ticket.setCheckMethod(CheckMethod.BY_SIGHT);
        tag = tagService.createTagAndRememberOfferState(offer, 0L);
        ticketService.saveTicket(ticket, tag);
        loaded = ticketService.loadTicketById(ticket.getId());
        assertEquals(ticket, loaded);

        assertNotNull(ticketService.loadAllTicketHistory(ticket.getId()).stream()
                .filter(th -> th.getStatus() == TicketStatus.NEW)
                .findAny().orElse(null));
    }

    @Test
    void testCreateTicketWithTheSameId() {
        long hypId = createTicketWithProblem();
        Ticket loaded = ticketService.loadTicketById(hypId);
        assertEquals(1, loaded.getProblems().size());

        assertThrows(DataIntegrityViolationException.class,
                () -> createTicket(hypothesisService.loadHypothesis(hypId)));

        entityManager.flush();
        entityManager.clear();

        Ticket loadedAgain = ticketService.loadTicketById(hypId);
        assertEquals(1, loadedAgain.getProblems().size());
    }

    private long createTicketWithProblem() {
        long hypId = createTicket(SHOP_ID, GEN_ID);

        TicketTag problemTag = tagService.createTag(0);
        Problem problem = Problem.newBuilder()
                .ticketId(hypId)
                .problemTypeId(ProblemTypeId.BAD_DELIVERY_DATE)
                .status(ProblemStatus.NEW)
                .build();
        problemService.saveProblem(problem, problemTag);
        entityManager.flush();
        entityManager.clear();
        return hypId;
    }
}
