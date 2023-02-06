package ru.yandex.market.partner.auction.servantlet.bulk.actions;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;
import ru.yandex.market.core.auction.model.AuctionGoalPlace;
import ru.yandex.market.core.auction.model.AuctionOfferBid;
import ru.yandex.market.core.auction.model.AuctionOfferIdType;
import ru.yandex.market.core.auction.model.BidPlace;
import ru.yandex.market.partner.auction.HybridGoal;
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
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_LINK_CBID_VARIABLE;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_LINK_FEE_VARIABLE;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_NO_LINK_CBID_PRIORITY;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_NO_LINK_CPC_ONLY;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_NO_LINK_FEE_PRIORITY;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.NOT_SPECIFIED;
import static ru.yandex.market.core.auction.model.AuctionBidValues.KEEP_OLD_BID_VALUE;
import static ru.yandex.market.core.auction.model.AuctionGoalPlace.PREMIUM_FIRST_PLACE;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.AUCTION_OFFER_BID_VALUE_200;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.SOME_QUERY;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.assertSuccessValidBidCreation;
import static ru.yandex.market.partner.auction.HybridGoal.GoalType.HYBRID_CARD;

/**
 * При создании ставки для карточных рекомендаций проверяем, что аргументы для вызова метода в биддинге формируются на
 * основе ответа репорта по позициям.
 * Ответ репорта замокан в файле.
 *
 * @author vbudnev
 */
@RunWith(Parameterized.class)
public class CreateBidsByCardRecommendationTest extends AuctionBulkServantletlMockBase {

    private final AuctionGoalPlace requestCardGoalValue;
    private final AuctionBidComponentsLink requestLinkType;
    private final BigInteger expectedCbidValue;
    private final BigInteger expectedFeeValue;

    public CreateBidsByCardRecommendationTest(
            AuctionBidComponentsLink requestLinkType,
            AuctionGoalPlace requestCardGoalValue,
            BigInteger expectedCbidValue,
            BigInteger expectedFeeValue
    ) {
        this.requestLinkType = requestLinkType;
        this.requestCardGoalValue = requestCardGoalValue;
        this.expectedCbidValue = expectedCbidValue;
        this.expectedFeeValue = expectedFeeValue;
    }

    @Parameterized.Parameters(name = "{index}: linkType={0} cardGoalValue={1} expectedCbidValue={2} expectedFeeValue={3}")
    public static Collection<Object[]> testCases() {
        return Arrays.asList(
                new Object[][]{
                        /**
                         * Тип связи более ничего не значит для карточных рекомендаций. Всегда изменятеся только cbid
                         * компонента (если не задан {@link HybridGoal#tied}).
                        */
                        {CARD_NO_LINK_FEE_PRIORITY, PREMIUM_FIRST_PLACE, AUCTION_OFFER_BID_VALUE_200, KEEP_OLD_BID_VALUE},
                        {CARD_NO_LINK_CBID_PRIORITY, PREMIUM_FIRST_PLACE, AUCTION_OFFER_BID_VALUE_200, KEEP_OLD_BID_VALUE},
                        {CARD_LINK_FEE_VARIABLE, PREMIUM_FIRST_PLACE, AUCTION_OFFER_BID_VALUE_200, KEEP_OLD_BID_VALUE},
                        {CARD_LINK_CBID_VARIABLE, PREMIUM_FIRST_PLACE, AUCTION_OFFER_BID_VALUE_200, KEEP_OLD_BID_VALUE},
                        {CARD_NO_LINK_CPC_ONLY, PREMIUM_FIRST_PLACE, AUCTION_OFFER_BID_VALUE_200, KEEP_OLD_BID_VALUE},
                        {CARD_LINK_FEE_VARIABLE, PREMIUM_FIRST_PLACE, AUCTION_OFFER_BID_VALUE_200, KEEP_OLD_BID_VALUE},
                        {NOT_SPECIFIED, PREMIUM_FIRST_PLACE, AUCTION_OFFER_BID_VALUE_200, KEEP_OLD_BID_VALUE},
                }
        );
    }

    @Before
    public void before() throws IOException {
        MockitoAnnotations.initMocks(this);

        ReportRecommendationService recommendationsService = new ReportRecommendationService(
                parallelSearchBidRecommendator,
                marketSearchBidRecommendator,
                PartiallyRecommendatorsFactory.buildCardRecommendator(
                        this.getClass().getResourceAsStream("./resources/hybrid_card_ok.xml")
                ),
                mockedExistenceChecker
        );

        auctionBulkOfferBidsServantlet.configure();
        generalBulkServantletInit(recommendationsService);

        usefullServResponse = new MockServResponse();
        mockBidLimits();
        mockOfferExists();

        mockServRequestCrudActionUPDATE();
        mockServRequestIdentificationParams();
        mockRegionsAndTariff();
    }

    @Test
    public void test_createBid_when_updateViaHCGoal_should_calculateFromReportAndPassToBidding() {
        mockShopAuctionType(AuctionOfferIdType.TITLE);

        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.goal.type=" + HYBRID_CARD +
                "&req1.goal.value=" + requestCardGoalValue +
                "&req1.linkType=" + requestLinkType +
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

        assertThat(passedBidValue, hasPlaceBid(BidPlace.SEARCH, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.CARD, expectedCbidValue));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_PLACE, expectedFeeValue));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_SEARCH, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasLinkType(CARD_NO_LINK_CPC_ONLY));

        assertSuccessValidBidCreation(usefullServResponse, 1);
    }

    /**
     * При offer_title_as_search_query=true прикапываемый поисковый запрос соответсвует тайтловому описанию ТП
     * {@link AuctionOfferBid#offerId}.
     */
    @Test
    public void test_createBid_when_updateViaHCGoal_and_explicitSearchQueryFlag() {
        mockShopAuctionType(AuctionOfferIdType.TITLE);

        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.goal.type=" + HYBRID_CARD +
                "&req1.goal.value=" + requestCardGoalValue +
                "&req1.linkType=" + requestLinkType +
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

    /**
     * Для типа идентификации {@link AuctionOfferIdType#SHOP_OFFER_ID} при offer_title_as_search_query=true
     * прикапываемый поисковый запрос соответсвует тайтловому описанию ТП {@link AuctionOfferBid#offerId}.
     */
    @Test
    public void test_createBid_when_updateByOfferIdViaHCGoal_and_explicitSearchQueryFlag() {
        mockShopAuctionType(AuctionOfferIdType.SHOP_OFFER_ID);
        mockTitles(ImmutableMap.of(SOME_FEED_OFFER_ID, "reportObtainedTitle"));

        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerId=" + SOME_FEED_OFFER_ID.getId() +
                "&req1.feedId=" + SOME_FEED_OFFER_ID.getFeedId() +
                "&req1.goal.type=" + HYBRID_CARD +
                "&req1.goal.value=" + requestCardGoalValue +
                "&req1.linkType=" + requestLinkType +
                "&searchQuery=" + SOME_QUERY +
                "&offer_title_as_search_query=true"
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));

        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);
        assertThat(passedBidValue, hasId(SOME_FEED_OFFER_ID));
        assertThat(passedBidValue, hasGroupId(DEFAULT_GROUP_ID));
        assertThat(passedBidValue, hasSearchQuery("reportObtainedTitle"));
        assertThat(passedBidValue, hasShopId(PARAM_DATASOURCE_ID));

        assertSuccessValidBidCreation(usefullServResponse, 1);
    }
}
