package ru.yandex.market.abo.core.problem.approve;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.abo.core.feed.model.DbFeedOfferDetails;
import ru.yandex.market.abo.core.feed.search.task.model.FeedSearchTask;
import ru.yandex.market.abo.core.feed.search.task.model.SearchTaskTarget;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.ticket.ProblemService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * created on 01.06.17.
 */
public class ProblemPriceApproverTest {

    @Mock
    private DbFeedOfferDetails offer;
    @Mock
    private Problem problem;
    @Mock
    private Offer storedOffer;
    @Mock
    private FeedSearchTask task;
    @Mock
    private ProblemService problemService;
    @Mock
    private OfferPriceOrStockProblem priceOrOnStockProblem;

    @InjectMocks
    private ProblemPriceApprover problemPriceApprover;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(task.getOffer()).thenReturn(offer);
    }

    @Test
    public void priceProblemMustNotBeApprovedWhenMarketPriceDiffersFromFeedPriceAndNoSitePrice() {
        when(offer.getPrice()).thenReturn(100d);
        when(offer.getShopPrice()).thenReturn(100d);
        when(priceOrOnStockProblem.getSitePrice()).thenReturn(0d);
        when(priceOrOnStockProblem.getMarketPrice()).thenReturn(90d);

        assertFalse(problemPriceApprover.approve(problem, storedOffer, task, priceOrOnStockProblem));
    }

    @Test
    public void priceProblemMustBeApprovedWhenMarketPriceEqualsFeedPriceAndNoSitePrice() {
        when(offer.getPrice()).thenReturn(100d);
        when(offer.getShopPrice()).thenReturn(null);
        when(priceOrOnStockProblem.getSitePrice()).thenReturn(0d);
        when(priceOrOnStockProblem.getMarketPrice()).thenReturn(100d);
        when(priceOrOnStockProblem.getMarketShopPrice()).thenReturn(null);

        assertTrue(problemPriceApprover.approve(problem, storedOffer, task, priceOrOnStockProblem));
    }

    @Test
    public void priceProblemMustNotBeApprovedWhenSitePriceEqualsFeedPrice() {
        when(offer.getPrice()).thenReturn(100d);
        when(offer.getShopPrice()).thenReturn(100d);
        when(priceOrOnStockProblem.getSitePrice()).thenReturn(100d);

        assertFalse(problemPriceApprover.approve(problem, storedOffer, task, priceOrOnStockProblem));
    }

    @Test
    public void priceProblemMustBeApprovedWhenSitePriceDiffersFromFeedPrice() {
        when(offer.getPrice()).thenReturn(100d);
        when(offer.getShopPrice()).thenReturn(100d);
        when(priceOrOnStockProblem.getSitePrice()).thenReturn(105d);

        assertTrue(problemPriceApprover.approve(problem, storedOffer, task, priceOrOnStockProblem));
    }

    @ParameterizedTest(name = "priceProblemWithDataCampOffer_{index}")
    @MethodSource("priceProblemWithDataCampOfferMethodSource")
    public void priceProblemWithDataCampOffer(
            double offerPrice, boolean expectedApprove, double marketPrice, double sitePrice, Double marketShopPrice
    ) {
        when(task.getTarget()).thenReturn(SearchTaskTarget.DATA_CAMP);
        when(offer.getPrice()).thenReturn(offerPrice);
        when(offer.getShopCurrency()).thenReturn(Currency.EUR);
        when(task.getOffer()).thenReturn(offer);
        when(priceOrOnStockProblem.getSitePrice()).thenReturn(sitePrice);
        when(priceOrOnStockProblem.getMarketPrice()).thenReturn(marketPrice);
        when(priceOrOnStockProblem.getMarketShopPrice()).thenReturn(marketShopPrice);

        assertEquals(expectedApprove, problemPriceApprover.approve(problem, storedOffer, task, priceOrOnStockProblem));
    }

    private static Stream<Arguments> priceProblemWithDataCampOfferMethodSource() {
        return Stream.of(
                Arguments.of(100, true, 100d, 0d, null),
                Arguments.of(200, false, 20d, 0d, null),
                Arguments.of(100, false, 100d, 0d, 30d),
                Arguments.of(300d, false, 10d, 300d, null),
                Arguments.of(300d, true, 10d, 30d, null)
        );
    }
}
