package ru.yandex.market.partner.auction.servantlet.bulk.actions;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockitoAnnotations;
import org.xml.sax.SAXException;

import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.market.core.auction.AuctionService;
import ru.yandex.market.core.auction.model.AuctionOfferBid;
import ru.yandex.market.partner.auction.ReportRecommendationService;
import ru.yandex.market.partner.auction.servantlet.bulk.AuctionBulkServantletlMockBase;
import ru.yandex.market.partner.auction.servantlet.bulk.PartiallyRecommendatorsFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.market.core.auction.AuctionService.DEFAULT_GROUP_ID;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasGroupId;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasId;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasLinkType;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasSearchQuery;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasShopId;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_NO_LINK_CPC_ONLY;
import static ru.yandex.market.core.auction.model.AuctionGoalPlace.FIRST_PLACE;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.AUCTION_OFFER_BID_VALUE_3;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.assertSuccessValidBidUpdate;
import static ru.yandex.market.partner.auction.HybridGoal.GoalType.MARKET_SEARCH;
import static ru.yandex.market.partner.auction.HybridGoal.GoalType.PARALLEL_SEARCH;

/**
 * Приоритет аргументов задающих поисковый запрос при обновлении ставок.
 * Проверяются итоговые аргументы вызова в {@link AuctionService#setOfferBids(long, List, long)}.
 *
 * @author vbudnev
 */
@RunWith(Parameterized.class)
public class UpdateBidQueryOverloadTest extends AuctionBulkServantletlMockBase {

    private final String testDescription;
    private final String servantletQueryMixin;
    private final String expectedSetQuery;

    public UpdateBidQueryOverloadTest(String testDescription, String servantletQueryMixin, String expectedSetQuery) {
        this.testDescription = testDescription;
        this.servantletQueryMixin = servantletQueryMixin;
        this.expectedSetQuery = expectedSetQuery;
    }

    @Parameterized.Parameters(name = "{index}: description=\"{0}\"")
    public static Collection<Object[]> testCases() {
        return Arrays.asList(
                new Object[][]{
                        //изменение ставки с использованием цели для параллельных рекомендаций
                        {
                                "ps goal. no explicit query.",
                                "&req1.goal.type=" + PARALLEL_SEARCH +
                                        "&req1.goal.value=" + FIRST_PLACE,
                                SOME_OFFER_NAME
                        },
                        {
                                "ps goal. explicit offer query and general query.",
                                "&req1.goal.type=" + PARALLEL_SEARCH +
                                        "&req1.goal.value=" + FIRST_PLACE +
                                        "&req1.searchQuery=newOfferQuery" +
                                        "&searchQuery=newGeneralQuery",
                                "newOfferQuery"
                        },
                        {
                                "ps goal. explicit offer query.",
                                "&req1.goal.type=" + PARALLEL_SEARCH +
                                        "&req1.goal.value=" + FIRST_PLACE +
                                        "&searchQuery=newGeneralQuery",
                                "newGeneralQuery"
                        },

                        //изменение ставки с использованием цели для маркетных рекомендаций
                        {
                                "ms goal. no explicit query.",
                                "&req1.goal.type=" + MARKET_SEARCH +
                                        "&req1.goal.value=" + FIRST_PLACE,
                                SOME_OFFER_NAME
                        },
                        {
                                "ms goal. explicit offer query and general query.",
                                "&req1.goal.type=" + MARKET_SEARCH +
                                        "&req1.goal.value=" + FIRST_PLACE +
                                        "&req1.searchQuery=newOfferQuery" +
                                        "&searchQuery=newGeneralQuery",
                                "newOfferQuery"
                        },
                        {
                                "ms goal. explicit offer query.",
                                "&req1.goal.type=" + MARKET_SEARCH +
                                        "&req1.goal.value=" + FIRST_PLACE +
                                        "&searchQuery=newGeneralQuery",
                                "newGeneralQuery"
                        },

                        //изменение ставки с указанием явного значения
                        {
                                "no goal. no explicit query.",
                                "&req1.bid.value=" + AUCTION_OFFER_BID_VALUE_3,
                                SOME_OFFER_NAME
                        },
                        {
                                "no goal. explicit offer query and general query.",
                                "&req1.bid.value=" + AUCTION_OFFER_BID_VALUE_3 +
                                        "&req1.searchQuery=newOfferQuery" +
                                        "&searchQuery=newGeneralQuery",
                                "newOfferQuery"
                        },
                        {
                                "no goal. explicit offer query.",
                                "&req1.bid.value=" + AUCTION_OFFER_BID_VALUE_3 +
                                        "&searchQuery=newGeneralQuery",
                                "newGeneralQuery"
                        }

                }
        );
    }

    @Before
    public void before() throws IOException, SAXException {
        MockitoAnnotations.initMocks(this);

        ReportRecommendationService recommendationsService = new ReportRecommendationService(
                PartiallyRecommendatorsFactory.buildParallelSearchRecommendator(
                        this.getClass().getResourceAsStream("./resources/parallel_search_ok.xml")
                ),
                PartiallyRecommendatorsFactory.buildMarketSearchRecommendator(
                        this.getClass().getResourceAsStream("./resources/market_search_ok.xml")
                ),
                PartiallyRecommendatorsFactory.buildCardRecommendator(
                        this.getClass().getResourceAsStream("./resources/hybrid_card_ok.xml")
                ),
                mockedExistenceChecker
        );
        mockOfferExists();

        auctionBulkOfferBidsServantlet.configure();
        generalBulkServantletInit(recommendationsService);

        usefullServResponse = new MockServResponse();
        mockBidLimits();
        mockAuctionExistingBid(SOME_TITLE_OFFER_ID, PARAM_DATASOURCE_ID);

        mockServRequestCrudActionUPDATE();
        mockServRequestIdentificationParams();
        mockRegionsAndTariff();
    }

    @Test
    public void test_queryOverloadOnBidUpdate() {
        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                servantletQueryMixin
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        assertSuccessValidBidUpdate(usefullServResponse, 1);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));

        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);
        assertThat(passedBidValue, hasId(SOME_TITLE_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(DEFAULT_GROUP_ID));
        assertThat(passedBidValue, hasSearchQuery(expectedSetQuery));
        assertThat(passedBidValue, hasShopId(PARAM_DATASOURCE_ID));
        assertThat(passedBidValue, hasLinkType(CARD_NO_LINK_CPC_ONLY));
    }
}
