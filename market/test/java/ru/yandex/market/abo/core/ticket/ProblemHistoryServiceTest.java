package ru.yandex.market.abo.core.ticket;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.abo.core.offer.OfferState;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.problem.model.ProblemHistoryWithOfferState;
import ru.yandex.market.abo.core.problem.model.ProblemStatus;
import ru.yandex.market.abo.core.problem.model.ProblemTypeId;
import ru.yandex.market.abo.core.ticket.model.TicketTag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author artemmz
 */
class ProblemHistoryServiceTest extends AbstractCoreHierarchyTest {

    @Test
    void testHistoryAndState() {
        TicketTag tag = tagService.createTag(0L);

        Offer offer = new Offer();
        offer.setPrice(BigDecimal.valueOf(100));
        offer.setPriceCurrency(Currency.RUR);
        offerDbService.storeOffer(offer);

        offerStateManager.rememberOfferState(new OfferState(offer.getId(), 200d, true, "base_gen", tag.getId()));

        Problem p = createProblem(155L, 1, ProblemTypeId.HIGHER_PRICE, ProblemStatus.NEW, tag);

        List<ProblemHistoryWithOfferState> history = problemService.loadHistoryListWithState(p.getId());
        assertEquals(1, history.size());
        assertNotNull(history.get(0).getOfferState());
    }
}
