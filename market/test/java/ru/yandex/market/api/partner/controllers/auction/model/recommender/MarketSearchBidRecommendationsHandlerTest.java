package ru.yandex.market.api.partner.controllers.auction.model.recommender;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ru.yandex.market.api.partner.controllers.auction.model.BidRecommendation;
import ru.yandex.market.api.partner.controllers.auction.model.ImpossibleRecommendation;
import ru.yandex.market.api.partner.controllers.auction.model.OfferBidsRecommendation;
import ru.yandex.market.api.partner.controllers.auction.model.RecommendationTarget;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.core.auction.model.BidPlace;
import ru.yandex.market.core.auction.model.ComplexBid;
import ru.yandex.market.core.auction.model.StatusCode;
import ru.yandex.market.core.auction.recommend.BidRecommendations;
import ru.yandex.market.core.auction.recommend.OfferAuctionStats;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.BID_CENTS_1;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.BID_CENTS_111;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.BID_CENTS_2;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.BID_CENTS_222;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.BID_CENTS_3;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.BID_CENTS_333;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.BID_UE_0_01;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.BID_UE_0_02;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.BID_UE_1_11;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.BID_UE_2_22;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.BID_UE_3_33;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.SOME_AUCTION_OFFER;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.SOME_AUCTION_OFFER_ID;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.SOME_OFFER_NAME;

/**
 * @author vbudnev
 */
public class MarketSearchBidRecommendationsHandlerTest {
    private static MarketSearchBidRecommendationsHandler HANDLER;


    @BeforeAll
    public static void initOnce() {
        HANDLER = new MarketSearchBidRecommendationsHandler(0);
    }

    @Test
    void test_fillTargetRecommendationPositionsAndPlaces_when_errorStatusIsSet_should_setErrorMarket() {
        OfferBidsRecommendation.TargetRecommendation rcm = new OfferBidsRecommendation.TargetRecommendation();
        OfferAuctionStats.TargetAuctionStats stats = new OfferAuctionStats.TargetAuctionStats();
        stats.setUnavailable(true);
        HANDLER.fillTargetRecommendationPositionsAndPlaces(rcm, stats);

        assertThat(rcm.getError(), is(ImpossibleRecommendation.UNAVAILABLE));
    }

    @Test
    void test_fillTargetRecommendationPositionsAndPlaces_should_forwardPosInfo() {
        OfferBidsRecommendation.TargetRecommendation rcm = new OfferBidsRecommendation.TargetRecommendation();
        OfferAuctionStats.TargetAuctionStats stats = new OfferAuctionStats.TargetAuctionStats();
        stats.setModelCount(15);
        stats.setCurrentPosAll(100);
        HANDLER.fillTargetRecommendationPositionsAndPlaces(rcm, stats);

        assertThat(rcm.getModelCount(), is(15));
        assertThat(rcm.getCurrentPosAll(), is(100));
    }

    @Test
    void test_fillTargetRecommendationPositionsAndPlaces_should_fillPositionBids() {
        OfferBidsRecommendation.TargetRecommendation rcm = new OfferBidsRecommendation.TargetRecommendation();
        OfferAuctionStats.TargetAuctionStats stats = new OfferAuctionStats.TargetAuctionStats();

        stats.setPositionComplexBid(3, new ComplexBid(BID_CENTS_1, null, StatusCode.OK));
        stats.setPositionComplexBid(5, new ComplexBid(BID_CENTS_2, null, StatusCode.OK));
        stats.setPositionComplexBid(7, new ComplexBid(BID_CENTS_3, null, StatusCode.OK));

        HANDLER.fillTargetRecommendationPositionsAndPlaces(rcm, stats);

        assertThat(rcm.getRecommendations().size(), is(3));
    }

    @Test
    void test_createCpcNCpaRecommendation_when_complexBidContainsUnreachableCode_should_setUnreachableErrorMarker() {
        ComplexBid cb = new ComplexBid(null, null, StatusCode.FEE_IS_NOT_REACHABLE);
        BidRecommendation rec = HANDLER.createSearchCpcNCpaRecommendation(cb);
        assertThat(rec.getError(), is(ImpossibleRecommendation.UNREACHABLE));
    }

    @Test
    void test_createCpcNCpaRecommendation_when_bidComponentIsNegative_should_setErrorMarker() {
        ComplexBid hasNegativeBid = new ComplexBid(-1, null, StatusCode.OK);
        BidRecommendation recForNegativeBid = HANDLER.createSearchCpcNCpaRecommendation(hasNegativeBid);
        assertNotNull(recForNegativeBid.getError(), "Error marker must be set");

        ComplexBid hasNegativeFee = new ComplexBid(null, -1, StatusCode.OK);
        BidRecommendation recForNegativeFee = HANDLER.createSearchCpcNCpaRecommendation(hasNegativeFee);
        assertNotNull(recForNegativeFee.getError(), "Error marker must be set");
    }

    @Test
    void test_createCpcNCpaRecommendation_when_zeroValues_should_setRecComponentsToNulls() {
        ComplexBid cb = new ComplexBid(0, 0, StatusCode.OK);
        BidRecommendation rec = HANDLER.createSearchCpcNCpaRecommendation(cb);
        assertNull(rec.getBid());
        assertNull(rec.getFee());
        assertNull(rec.getError(), "Error marker must NOT be set");
    }

    @Test
    void test_createCpcNCpaRecommendation_when_nullValues_shluld_setRecComponentsToNulls() {
        ComplexBid cb = new ComplexBid(null, null, StatusCode.OK);
        BidRecommendation rec = HANDLER.createSearchCpcNCpaRecommendation(cb);
        assertNull(rec.getBid());
        assertNull(rec.getFee());
        assertNull(rec.getError(), "Error marker must NOT be set");
    }

    @Test
    void test_createCpcNCpaRecommendation_when_dataOk_should_convertRecValuesToUE_and_noErrorMarker() {
        ComplexBid cb = new ComplexBid(BID_CENTS_111, BID_CENTS_333, StatusCode.OK);
        BidRecommendation rec = HANDLER.createSearchCpcNCpaRecommendation(cb);
        assertThat(rec.getBid(), is(BID_UE_1_11));
        assertThat(rec.getFee(), is(BID_UE_3_33));
        assertNull(rec.getError(), "Error marker must NOT be set");
    }

    @Test
    void test_getTarget_should_be_overwritten() {
        assertThat(HANDLER.getTarget(), is(RecommendationTarget.MARKET_SEARCH));
    }

    @Test
    void test_handleRecommendationsInternal_generalRecommendationAttributesPopulationLogic() {
        MarketSearchBidRecommendationsHandler handler = new MarketSearchBidRecommendationsHandler(0);

        OfferAuctionStats stats = new OfferAuctionStats();
        FoundOffer foundOffer = new FoundOffer();
        foundOffer.setBid(BID_CENTS_111);
        foundOffer.setFee(BID_CENTS_222);
        foundOffer.setMinBid(BID_CENTS_1);
        foundOffer.setMinFee(BID_CENTS_2);
        foundOffer.setName(SOME_OFFER_NAME);
        foundOffer.setQualityFactor(new BigDecimal("0.836"));
        foundOffer.setPullUpBids(false);
        stats.setOffer(foundOffer);

        BidRecommendations bidRecommendations = new BidRecommendations(Collections.singletonList(stats), null);

        handler.handleRecommendationsInternal(SOME_AUCTION_OFFER, bidRecommendations);
        assertThat(handler.getRecommendations().size(), is(1));
        OfferBidsRecommendation obr = handler.getRecommendations().getRecommendations().get(0);

        assertEquals(BID_UE_0_01, obr.getMinimal(BidPlace.SEARCH), "Minimal bid value for place=SEARCH differs");
        assertEquals(BID_UE_1_11, obr.getActual(BidPlace.SEARCH), "Actual bid value for place=SEARCH differs");
        assertEquals(BID_UE_0_02, obr.getMinimal(BidPlace.MARKET_PLACE), "Minimal bid value for place=MARKET_PLACE differs");
        assertEquals(BID_UE_2_22, obr.getActual(BidPlace.MARKET_PLACE), "Actual bid value for place=MARKET_PLACE differs");
        assertEquals(SOME_OFFER_NAME, obr.getSearchQuery(), "Offer name must be transferred as search query to recommendation block");
        assertEquals(SOME_AUCTION_OFFER_ID, obr.getOfferId(), "OfferId must be transferred to recommendation block");
        assertEquals(new BigDecimal("0.84"), obr.getQualityFactor(), "QualityFactor must be transferred to recommendation block with rescale");
        assertEquals(true, obr.getDontPullUpBids(), "OfferId contain correct flag value ");

    }

    @Test
    void test_handleRecommendationsInternal_when_recommendationsIsEmpty_should_returnNotFound() {
        MarketSearchBidRecommendationsHandler handler = new MarketSearchBidRecommendationsHandler(0);

        BidRecommendations bidRecommendations = new BidRecommendations(Collections.emptyList(), null);
        handler.handleRecommendationsInternal(SOME_AUCTION_OFFER, bidRecommendations);
        assertThat(handler.getRecommendations().getRecommendations().get(0).getError(), is(ImpossibleRecommendation.OFFER_NOT_FOUND));
    }
}