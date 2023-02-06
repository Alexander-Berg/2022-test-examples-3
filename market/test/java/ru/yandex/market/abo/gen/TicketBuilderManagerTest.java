package ru.yandex.market.abo.gen;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.core.ticket.AbstractCoreHierarchyTest;
import ru.yandex.market.abo.gen.model.Hypothesis;
import ru.yandex.market.common.report.model.FeedOfferId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Nikolay Malevanny nmalevanny@yandex-team.ru
 */
public class TicketBuilderManagerTest extends AbstractCoreHierarchyTest {

    private static final double WEIGHT = 0.1;
    private static final long TEST_SHOP = 774L;

    @Autowired
    private TicketBuilderManager ticketBuilderManager;
    @Autowired
    private HypothesisService hypothesisService;

    @Test
    public void testBuildComplainByHypothesis() {
        Hypothesis h = new Hypothesis(TEST_SHOP, 0, 1, "test", WEIGHT, 0, null);

        long ids = hypothesisService.createHypothesis(h);
        h = hypothesisService.loadHypothesis(ids);

        ticketBuilderManager.initTicketBuilders();
        ticketBuilderManager.buildTicket(h);
    }

    @Test
    public void testSetDup() {
        long masterHypId = hypWithTicket().getId();
        long dupHypId = hypothesisService.createHypothesis(hypWithSameOffer());

        Stream.of(masterHypId, dupHypId).forEach(hId -> assertFalse(hypothesisService.loadHypothesis(hId).getFailed()));
        flushAndClear();
        hypothesisService.setHypothesisDups();

        assertFalse(hypothesisService.loadHypothesis(masterHypId).getFailed());
        assertTrue(hypothesisService.loadHypothesis(dupHypId).getFailed());

        // удаляем гипотезу мастер, вторичные дубликаты не должны создаваться от оставшегося dupHypId
        ticketService.deleteTicketsById(List.of(masterHypId));
        hypothesisService.deleteHypothesisesById(List.of(masterHypId));
        long newHypId = hypothesisService.createHypothesis(hypWithSameOffer());

        flushAndClear();
        hypothesisService.setHypothesisDups();
        assertFalse(hypothesisService.loadHypothesis(newHypId).getFailed());
    }

    @Test
    public void testPokupkiTicketFailed() {
        Hypothesis h = createHypothesis(431782, 0);
        assertFalse(ticketBuilderManager.buildTicket(h));
        assertTrue(h.getFailed());
    }

    private Hypothesis hypWithTicket() {
        Hypothesis hyp = hypWithSameOffer();
        hypothesisService.createHypothesis(hyp);
        createTicket(hyp);
        return hyp;
    }

    private static Hypothesis hypWithSameOffer() {
        return Hypothesis.builder(TEST_SHOP, 0)
                .withFeedOfferId(new FeedOfferId("offer_id", 12321L))
                .build();
    }

}
