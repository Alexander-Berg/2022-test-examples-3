package ru.yandex.market.partner.auction.servantlet.bulk.actions;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.xml.sax.SAXException;

import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;
import ru.yandex.market.core.auction.model.AuctionOfferBid;
import ru.yandex.market.core.auction.model.BidPlace;
import ru.yandex.market.partner.auction.ReportRecommendationService;
import ru.yandex.market.partner.auction.servantlet.bulk.AuctionBulkServantletlMockBase;
import ru.yandex.market.partner.auction.servantlet.bulk.PartiallyRecommendatorsFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasLinkType;
import static ru.yandex.market.core.auction.matchers.AuctionOfferBidFeatureMatchers.hasPlaceBid;
import static ru.yandex.market.core.auction.model.AuctionBidValues.KEEP_OLD_BID_VALUE;
import static ru.yandex.market.core.auction.model.AuctionGoalPlace.FIRST_PAGE;
import static ru.yandex.market.core.auction.model.AuctionGoalPlace.FIRST_PLACE;
import static ru.yandex.market.core.auction.model.AuctionGoalPlace.PREMIUM_FIRST_PLACE;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.AUCTION_OFFER_BID_VALUE_3;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.AUCTION_OFFER_BID_VALUE_80;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.assertSuccessValidBidUpdate;
import static ru.yandex.market.partner.auction.HybridGoal.GoalType.HYBRID_CARD;
import static ru.yandex.market.partner.auction.HybridGoal.GoalType.MARKET_SEARCH;
import static ru.yandex.market.partner.auction.HybridGoal.GoalType.PARALLEL_SEARCH;

/**
 * Тесты на ограничение рекомендованного значения полученного от репорта.
 * Детально комбинации покрыты в {@link ru.yandex.market.partner.auction.createupdate.limits}, здесь просто проверяем
 * что для каждого типа рекомендаций эта механика действиетльно задействована.
 *
 * @author vbudnev
 */
public class UpdateBidsByRecommendationWithMaxLimitsSmokeTest extends AuctionBulkServantletlMockBase {

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

    /**
     * Ограничение cbid компоненты при использовании цели на карточке, независимо от указанного типа связи.
     */
    @Test
    public void test_updateBid_when_setExplicitMaxCbidValue() {
        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.goal.type=" + HYBRID_CARD +
                "&req1.goal.value=" + PREMIUM_FIRST_PLACE +
                "&req1.linkType=" + AuctionBidComponentsLink.CARD_NO_LINK_CBID_PRIORITY +
                "&req1.fee.value=" + AUCTION_OFFER_BID_VALUE_3 +
                "&req1.cbid.max=" + AUCTION_OFFER_BID_VALUE_80
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));

        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);

        assertThat(passedBidValue, hasPlaceBid(BidPlace.SEARCH, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_PLACE, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.CARD, AUCTION_OFFER_BID_VALUE_80));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_SEARCH, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasLinkType(AuctionBidComponentsLink.CARD_NO_LINK_CPC_ONLY));

        assertSuccessValidBidUpdate(usefullServResponse, 1);
    }

    /**
     * Ограничение bid компоненты при использовании цели на параллельном поиске.
     */
    @Test
    public void test_updateBid_when_setExplicitMaxBidValue_and_marketSearchGoal() {
        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.goal.type=" + PARALLEL_SEARCH +
                "&req1.goal.value=" + FIRST_PLACE +
                "&req1.bid.max=" + AUCTION_OFFER_BID_VALUE_3
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));

        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);

        assertThat(passedBidValue, hasPlaceBid(BidPlace.SEARCH, AUCTION_OFFER_BID_VALUE_3));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_PLACE, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.CARD, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_SEARCH, KEEP_OLD_BID_VALUE));

        assertSuccessValidBidUpdate(usefullServResponse, 1);
    }

    /**
     * Ограничение bid компоненты при использовании цели на маркетном поиске.
     */
    @Test
    public void test_updateBid_when_setExplicitMaxBidValue_and_parallelSearchGoal() {
        mockServantletPassedArgs("" +
                "req.size=1" +
                "&req1.offerName=" + SOME_OFFER_NAME +
                "&req1.goal.type=" + MARKET_SEARCH +
                "&req1.goal.value=" + FIRST_PAGE +
                "&req1.bid.max=" + AUCTION_OFFER_BID_VALUE_3
        );

        auctionBulkOfferBidsServantlet.process(servRequest, usefullServResponse);

        List<AuctionOfferBid> setOfferBidsArgument = extractAuctionSetOffersBids();

        assertThat(setOfferBidsArgument, hasSize(1));

        AuctionOfferBid passedBidValue = setOfferBidsArgument.get(0);

        assertThat(passedBidValue, hasPlaceBid(BidPlace.SEARCH, AUCTION_OFFER_BID_VALUE_3));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_PLACE, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.CARD, KEEP_OLD_BID_VALUE));
        assertThat(passedBidValue, hasPlaceBid(BidPlace.MARKET_SEARCH, KEEP_OLD_BID_VALUE));

        assertSuccessValidBidUpdate(usefullServResponse, 1);
    }
}
