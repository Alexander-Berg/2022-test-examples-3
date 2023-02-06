package ru.yandex.market.abo.core.ticket;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.core.offer.OfferStateManager;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.problem.model.ProblemStatus;
import ru.yandex.market.abo.core.ticket.model.CheckMethod;
import ru.yandex.market.abo.core.ticket.model.Ticket;
import ru.yandex.market.abo.core.ticket.model.TicketTag;
import ru.yandex.market.abo.core.ticket.repository.ProblemRepo;
import ru.yandex.market.abo.gen.HypothesisService;
import ru.yandex.market.abo.gen.model.Hypothesis;

/**
 * Contains several useful method for creating full core_ entities:
 * hypothesis, core_ticket, core_offer, core_problem, core_tag.
 *
 * @author antipov93.
 * @date 23.01.19.
 */
public abstract class AbstractCoreHierarchyTest extends EmptyTest {

    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @PersistenceContext
    protected EntityManager entityManager;
    @Autowired
    protected HypothesisService hypothesisService;
    @Autowired
    protected TicketService ticketService;
    @Autowired
    protected OfferDbService offerDbService;
    @Autowired
    protected OfferStateManager offerStateManager;
    @Autowired
    protected ProblemService problemService;
    @Autowired
    protected TicketTagService tagService;
    @Autowired
    protected ProblemRepo problemRepo;

    protected Hypothesis createHypothesis(long shopId, int genId) {
        return createHypothesis(shopId, genId, 0);
    }

    protected Hypothesis createHypothesis(long shopId, int genId, long sourceId) {
        var hyp = Hypothesis.builder(shopId, genId)
                .withSourceId(sourceId)
                .build();
        hypothesisService.createHypothesis(hyp);
        return hyp;
    }

    protected long createTicket(long shopId, int genId) {
        return createTicket(shopId, genId, 0);
    }

    protected long createTicket(long shopId, int genId, long sourceId) {
        return createTicket(createHypothesis(shopId, genId, sourceId));
    }

    protected long createTicket(Hypothesis h) {
        Offer offer = findOffer(h.getShopId());
        offerDbService.storeOffer(offer);
        TicketTag tag = tagService.createTag(0);
        offerStateManager.rememberCurrentOfferState(offer, tag.getId());
        Ticket ticket = new Ticket(h, offer.getId(), 213, CheckMethod.BASKET);
        ticketService.saveTicket(ticket, tag);
        return ticket.getId();
    }

    protected Problem createProblem(long shopId, int genId, int problemTypeId, ProblemStatus status) {
        return createProblem(shopId, genId, problemTypeId, status, tagService.createTag(0));
    }

    protected Problem createProblem(long shopId, int genId, int problemTypeId,
                                    ProblemStatus status, LocalDateTime problemCreationTime) {
        return createProblem(shopId, genId, problemTypeId, status, tagService.createTag(0), problemCreationTime);
    }

    @SuppressWarnings("SameParameterValue")
    protected Problem createProblem(long shopId, int genId, int problemTypeId, ProblemStatus status, TicketTag tag) {
        return createProblem(shopId, genId, problemTypeId, status, tag, LocalDateTime.now());
    }

    protected Problem createProblem(long shopId, int genId, int problemTypeId, ProblemStatus status,
                                    TicketTag tag, LocalDateTime problemCreationTime) {
        long hypId = createTicket(shopId, genId);
        Problem problem = Problem.newBuilder()
                .ticketId(hypId)
                .problemTypeId(problemTypeId)
                .creationTime(DateUtil.asDate(problemCreationTime))
                .status(status)
                .build();
        return problemService.saveProblem(problem, tag);
    }

    @SuppressWarnings("SameParameterValue")
    protected Problem createProblem(long shopId, int genId, int problemTypeId, ProblemStatus status,
                                    TicketTag tag, long sourceId) {
        long hypId = createTicket(shopId, genId, sourceId);
        Problem problem = Problem.newBuilder()
                .ticketId(hypId)
                .problemTypeId(problemTypeId)
                .status(status)
                .build();
        return problemService.saveProblem(problem, tag);
    }

    protected static Offer findOffer(long shopId) {
        Offer reportOffer = new Offer();
        reportOffer.setShopId(shopId);
        reportOffer.setName("name");
        reportOffer.setShopOfferId("asdf");
        reportOffer.setDirectUrl("https://random.shop.url.com/test.html");
        reportOffer.setFeedId(1235L);
        reportOffer.setFeedCategoryId("category");
        reportOffer.setPriceCurrency(ru.yandex.common.util.currency.Currency.RUR);
        reportOffer.setPrice(new BigDecimal(1234d));
        reportOffer.setFeeShow("feeShow");
        reportOffer.setPriorityRegionId(213L);
        reportOffer.setOnStock(true);
        reportOffer.setBaseGeneration("generation");
        return reportOffer;
    }
}
