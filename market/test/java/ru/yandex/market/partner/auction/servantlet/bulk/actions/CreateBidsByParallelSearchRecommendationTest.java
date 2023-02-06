package ru.yandex.market.partner.auction.servantlet.bulk.actions;

import java.io.IOException;
import java.math.BigInteger;
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
import ru.yandex.market.core.auction.model.AuctionGoalPlace;
import ru.yandex.market.core.auction.model.AuctionOfferBid;
import ru.yandex.market.core.auction.model.AuctionOfferIdType;
import ru.yandex.market.core.auction.model.BidPlace;
import ru.yandex.market.partner.auction.ReportRecommendationService;
import ru.yandex.market.partner.auction.servantlet.bulk.AuctionBulkServantletlMockBase;
import ru.yandex.market.partner.auction.servantlet.bulk.PartiallyRecommendatorsFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.market.core.auction.AuctionService.DEFAULT_GROUP_ID;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasGroupId;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasId;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasLinkType;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasPlaceBid;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasSearchQuery;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasShopId;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_NO_LINK_CPC_ONLY;
import static ru.yandex.market.core.auction.model.AuctionBidValues.KEEP_OLD_BID_VALUE;
import static ru.yandex.market.core.auction.model.AuctionGoalPlace.FIRST_PAGE;
import static ru.yandex.market.core.auction.model.AuctionGoalPlace.FIRST_PLACE;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.AUCTION_OFFER_BID_VALUE_200;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.AUCTION_OFFER_BID_VALUE_2000;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.SOME_QUERY;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.assertSuccessValidBidCreation;
import static ru.yandex.market.partner.auction.HybridGoal.GoalType.PARALLEL_SEARCH;

/**
 * При создании ставки для параллельных рекомендаций проверяем, что при установке ставки с использованием целей
 * аргументы для вызова метода в биддинге формируются на основе ответа репорта по позициям.
 * Ответ репорта замокан в файле.
 *
 * @author vbudnev
 */
@RunWith(Parameterized.class)
public class CreateBidsByParallelSearchRecommendationTest extends AuctionBulkServantletlMockBase {

    private final AuctionGoalPlace requestGoalValue;
    private final BigInteger expectedBidValue;

    public CreateBidsByParallelSearchRecommendationTest(
            AuctionGoalPlace requestGoalValue,
            BigInteger expectedBidValue
    ) {
        this.requestGoalValue = requestGoalValue;
        this.expectedBidValue = expectedBidValue;
    }

    @Parameterized.Parameters(name = "{index}: goalValue={0} expectedBidValue={1}")
    public static Collection<Object[]> testCases() {
        return Arrays.asList(
                new Object[][]{
                        {FIRST_PLACE, AUCTION_OFFER_BID_VALUE_2000},
                        {FIRST_PAGE, AUCTION_OFFER_BID_VALUE_200},
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
                marketSearchBidRecommendator,
                modelCardBidRecommendator,
                mockedExistenceChecker
        );

        auctionBulkOfferBidsServantlet.configure();
        generalBulkServantletInit(recommendationsService);
        mockOfferExists();

        usefullServResponse = new MockServResponse();
        mockBidLimits();

        mockServRequestCrudActionUPDATE();
        mockServRequestIdentificationParams();
        mockRegionsAndTariff();
        mockShopAuctionType(AuctionOfferIdType.TITLE);
    }

    @Test
    public void test_updateBid_when_updateViaPSGoal_should_calculateFromReportAndPassToBidding() {
        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.goal.type=" + PARALLEL_SEARCH +
                "&req1.goal.value=" + requestGoalValue +
                "&searchQuery=" + SOME_QUERY
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));

        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);
        assertThat(passedBidValue, hasId(SOME_TITLE_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(DEFAULT_GROUP_ID));
        assertThat(passedBidValue, hasSearchQuery(SOME_QUERY));
        assertThat(passedBidValue, hasShopId(PARAM_DATASOURCE_ID));

        assertThat(passedBidValue, hasPlaceBid(BidPlace.SEARCH, expectedBidValue));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_PLACE, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.CARD, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_SEARCH, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasLinkType(CARD_NO_LINK_CPC_ONLY));

        assertSuccessValidBidCreation(usefullServResponse, 1);
    }

    /**
     * При offer_title_as_search_query=true прикапываемый поисковый запрос соответсвует тайтловому описанию ТП
     * {@link AuctionOfferBid#offerId}.
     */
    @Test
    public void test_createBid_when_updateViaPSGoal_and_explicitSearchQueryFlag() {
        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.goal.type=" + PARALLEL_SEARCH +
                "&req1.goal.value=" + requestGoalValue +
                "&searchQuery=" + SOME_QUERY +
                "&offer_title_as_search_query=true"
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));

        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);
        assertThat(passedBidValue, hasId(SOME_TITLE_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(DEFAULT_GROUP_ID));
        assertThat(passedBidValue, hasSearchQuery(SOME_OFFER_NAME));
        assertThat(passedBidValue, hasShopId(PARAM_DATASOURCE_ID));

        assertSuccessValidBidCreation(usefullServResponse, 1);
    }

}
