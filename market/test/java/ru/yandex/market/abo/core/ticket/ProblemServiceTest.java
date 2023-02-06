package ru.yandex.market.abo.core.ticket;

import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import ru.yandex.market.abo.core.feed.model.DbFeedOfferDetails;
import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.problem.model.ProblemApprove;
import ru.yandex.market.abo.core.problem.model.ProblemSearchRequest;
import ru.yandex.market.abo.core.problem.model.ProblemStatus;
import ru.yandex.market.abo.core.problem.model.ProblemTypeId;
import ru.yandex.market.abo.core.ticket.repository.ProblemApproveRepository;
import ru.yandex.market.abo.core.ticket.repository.ProblemHistoryRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.abo.core.problem.model.ProblemStatus.APPROVED;
import static ru.yandex.market.abo.core.problem.model.ProblemStatus.DRAFT;
import static ru.yandex.market.abo.core.problem.model.ProblemStatus.NEW;
import static ru.yandex.market.abo.core.problem.model.ProblemStatus.REJECTED_FAULT;

/**
 * @author Melnikov Ivan imelnikov@yandex-team.ru.
 */
class ProblemServiceTest extends AbstractCoreHierarchyTest {

    @Autowired
    private ProblemManager problemManager;
    @Autowired
    private ProblemApproveRepository problemApproveRepository;
    @Autowired
    private ProblemHistoryRepository problemHistoryRepository;


    @Test
    void testCreateDeleteProblem() {
        Problem problem = createProblem(1, 1, ProblemTypeId.HIGHER_PRICE, APPROVED);
        Long id = problem.getId();
        long tagId = problem.getCreationTag().getId();
        flushAndClear();
        assertNotNull(problemService.loadProblem(id));
        assertNotNull(tagService.loadTag(tagId));
        assertFalse(problemHistoryRepository.findAllByProblemId(id).isEmpty());
        problemService.deleteProblem(id);
        flushAndClear();
        assertNull(problemService.loadProblem(id));
        assertNull(tagService.loadTag(tagId));
        assertTrue(problemHistoryRepository.findAllByProblemId(id).isEmpty());
    }


    @Test
    void deleteBatch() {
        Problem problem = createProblem(1, 31, ProblemTypeId.PINGER_CONTENT_SIZE, APPROVED);
        Long id = problem.getId();

        assertNotNull(problemService.loadProblem(id));
        assertFalse(problemService.loadHistoryList(id).isEmpty());
        problemService.delete(Collections.singletonList(problem));

        entityManager.flush();
        entityManager.clear();
        assertNull(problemService.loadProblem(id));
        assertTrue(problemService.loadHistoryList(id).isEmpty());
    }

    @Test
    void updateProblem() {
        Problem p1 = createProblem(1, 1, ProblemTypeId.HIGHER_PRICE, APPROVED);

        p1.setStatus(REJECTED_FAULT);
        p1.updatePublicCommentIfNeeded("public");
        p1.addUserComment("private");
        problemManager.saveProblemAndCreateStTicketIfNeeded(p1, tagService.createTag(0L), null);
        Problem p2 = problemManager.loadProblem(p1.getId());
        assertEquals(REJECTED_FAULT, p2.getStatus());
        assertTrue(p2.getUserComment().contains("private"));
        assertEquals("public", p2.getPublicComment());
    }

    @Test
    void testStoreApproveInfo() {
        Problem problem = createProblem(1, 1, ProblemTypeId.HIGHER_PRICE, APPROVED);
        DbFeedOfferDetails offer = initDbOffer();
        double approvePrice = RND.nextDouble();
        problemService.storeApproveInfo(problem, offer, approvePrice);

        ProblemApprove approve = checkInDb(problem);
        assertFalse(approve.isRawFeed());
        assertEquals(approve.getApprovePrice(), approvePrice, 0.01);
        assertEquals(approve.getFeedPrice(), offer.getPrice(), 0.01);
        assertEquals(approve.getFeedShopPrice(), offer.getShopPrice());
        assertEquals(approve.getDelivery(), offer.getDelivery());
        assertEquals(offer.getAvailable(), approve.getOnStock());
        assertEquals(offer.getLastChecked(), approve.getFeedTime());
        assertEquals(offer.getFeedSession(), approve.getFeedSession());
        assertEquals(offer.getFeedId(), (int) approve.getFeedId());
    }

    @Test
    void testGetProblemsForAutoApprove() {
        createProblem(1, 1, ProblemTypeId.HIGHER_PRICE, NEW);
        assertFalse(problemService.getProblemsForAutoApprove().isEmpty());
    }

    @Test
    void loadPreviousStatus() {
        ProblemStatus previousStatus = DRAFT;
        Problem problem = createProblem(1, 1, ProblemTypeId.HIGHER_PRICE, DRAFT);
        long id = problem.getId();
        assertNull(problemService.loadPreviousStatus(problem.getId()));

        for (ProblemStatus status : EnumSet.complementOf(EnumSet.of(previousStatus))) {
            problem.setStatus(status);
            problemService.saveProblem(problem, tagService.createTag(status.getId()));

            ProblemStatus previousStatusFromDb = problemService.loadPreviousStatus(id);
            assertEquals(previousStatus, previousStatusFromDb);
            previousStatus = status;
        }
    }

    @Test
    void loadByRequest() {
        int typeId = ProblemTypeId.HIGHER_PRICE;
        ProblemStatus status = APPROVED;
        Problem problem = createProblem(1, 1, typeId, status);

        List<Problem> problems = problemService.load(
                ProblemSearchRequest.newBuilder()
                        .statusIds(status)
                        .typeId(typeId)
                        .build(),
                PageRequest.of(0, 10)
        ).getContent();

        assertEquals(1, problems.size());
        assertEquals(problem, problems.get(0));

        List<Problem> empty = problemService.load(
                ProblemSearchRequest.newBuilder()
                        .statusIds(status)
                        .exceptTypes(typeId)
                        .build(),
                PageRequest.of(0, 10)
        ).getContent();
        assertTrue(empty.isEmpty());
    }

    private ProblemApprove checkInDb(Problem problem) {
        ProblemApprove approve = problemApproveRepository.findByIdOrNull(problem.getId());
        assertNotNull(approve);
        assertNotNull(approve.getFeedTime());
        return approve;
    }

    private static DbFeedOfferDetails initDbOffer() {
        var offer = new DbFeedOfferDetails();
        offer.setFeedId(RND.nextInt());
        offer.setLastChecked(new Date());
        return offer;
    }
}
