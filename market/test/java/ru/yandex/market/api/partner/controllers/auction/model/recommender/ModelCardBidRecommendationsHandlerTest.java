package ru.yandex.market.api.partner.controllers.auction.model.recommender;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import ru.yandex.market.api.partner.controllers.auction.model.AuctionOffer;
import ru.yandex.market.api.partner.controllers.auction.model.ImpossibleRecommendation;
import ru.yandex.market.api.partner.controllers.auction.model.OfferBidsRecommendation;
import ru.yandex.market.api.partner.controllers.auction.model.RecommendationTarget;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.SearchResults;
import ru.yandex.market.core.auction.model.BidPlace;
import ru.yandex.market.core.auction.recommend.BidRecommendations;
import ru.yandex.market.core.auction.recommend.OfferAuctionStats;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.BID_CENTS_2;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.BID_CENTS_222;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.BID_CENTS_3;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.BID_CENTS_333;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.BID_UE_0_02;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.BID_UE_0_03;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.BID_UE_2_22;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.BID_UE_3_33;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.SOME_AUCTION_OFFER;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.SOME_AUCTION_OFFER_ID;
import static ru.yandex.market.api.partner.controllers.auction.model.recommender.SharedRecommendationsHandler.SOME_OFFER_NAME;

class ModelCardBidRecommendationsHandlerTest {

    @Test
    void test_handleRecommendationsInternal_generalRecommendationAttributesPopulationLogic() {
        ModelCardBidRecommendationsHandler handler = new ModelCardBidRecommendationsHandler(0, new HashSet<>(Arrays.asList(RecommendationTarget.MODEL_CARD, RecommendationTarget.MODEL_CARD_CPA)));

        OfferAuctionStats stats = new OfferAuctionStats();
        FoundOffer foundOffer = new FoundOffer();
        foundOffer.setFee(BID_CENTS_222);
        foundOffer.setBid(BID_CENTS_333);
        foundOffer.setMinFee(BID_CENTS_2);
        foundOffer.setMinBid(BID_CENTS_3);
        foundOffer.setName(SOME_OFFER_NAME);
        foundOffer.setQualityFactor(new BigDecimal("0.836"));
        foundOffer.setPullUpBids(false);
        stats.setOffer(foundOffer);
        stats.setMinFee(BID_CENTS_2);
        stats.setMinBid(BID_CENTS_3);

        BidRecommendations bidRecommendations = new BidRecommendations(Arrays.asList(stats), null);

        handler.handleRecommendationsInternal(SOME_AUCTION_OFFER, bidRecommendations);
        assertThat("Recommendations size differs", handler.getRecommendations().size(), is(1));
        OfferBidsRecommendation obr = handler.getRecommendations().getRecommendations().get(0);

        assertEquals("Minimal bid value for place=SEARCH differs", BID_UE_0_03, obr.getMinimal(BidPlace.SEARCH));
        assertEquals("Actual bid value for place=SEARCH differs", BID_UE_3_33, obr.getActual(BidPlace.SEARCH));
        assertEquals("Minimal bid value for place=MARKET_PLACE differs", BID_UE_0_02, obr.getMinimal(BidPlace.MARKET_PLACE));
        assertEquals("Actual bid value for place=MARKET_PLACE differs", BID_UE_2_22, obr.getActual(BidPlace.MARKET_PLACE));
        assertEquals("Offer name must be transferred as search query to recommendation block", SOME_OFFER_NAME, obr.getSearchQuery());
        assertEquals("OfferId must be transferred to recommendation block", SOME_AUCTION_OFFER_ID, obr.getOfferId());
        assertEquals("QualityFactor must be transferred to recommendation block with rescale", new BigDecimal("0.84"), obr.getQualityFactor());
        assertEquals("OfferId contain correct flag value ", true, obr.getDontPullUpBids());

    }

    @Test
    void test_handleRecommendationsInternal_when_recommendationsIsEmpty_should_returnNotFound() {
        ModelCardBidRecommendationsHandler handler = new ModelCardBidRecommendationsHandler(0, new HashSet<>(Arrays.asList(RecommendationTarget.MODEL_CARD, RecommendationTarget.MODEL_CARD_CPA)));

        BidRecommendations bidRecommendations = new BidRecommendations(Collections.emptyList(), null);
        handler.handleRecommendationsInternal(SOME_AUCTION_OFFER, bidRecommendations);
        assertThat(handler.getRecommendations().getRecommendations().get(0).getError(), is(ImpossibleRecommendation.OFFER_NOT_FOUND));
    }

    @Test
    void shouldReturnOfferNotMatchedWhenHyperIdIsNull() {
        ModelCardBidRecommendationsHandler handler =
                new ModelCardBidRecommendationsHandler(0, EnumSet.of(RecommendationTarget.MODEL_CARD));
        AuctionOffer offer = new AuctionOffer();
        OfferAuctionStats offerAuctionStats = new OfferAuctionStats();
        FoundOffer foundOffer = new FoundOffer();
        offerAuctionStats.setOffer(foundOffer);
        SearchResults searchResults = new SearchResults();
        BidRecommendations recommendations =
                new BidRecommendations(
                        Arrays.asList(offerAuctionStats),
                        searchResults,
                        Arrays.asList(foundOffer)
                );
        handler.handleRecommendations(offer, recommendations);
        assertEquals(
                handler.getRecommendations().getRecommendations().get(0).getError(),
                ImpossibleRecommendation.OFFER_NOT_MATCHED
        );
    }

    @Test
    void shouldNotReturnOfferNotMatchedWhenHyperIdIsNotNull() {
        ModelCardBidRecommendationsHandler handler =
                new ModelCardBidRecommendationsHandler(0, EnumSet.of(RecommendationTarget.MODEL_CARD));
        AuctionOffer offer = new AuctionOffer();
        OfferAuctionStats offerAuctionStats = new OfferAuctionStats();
        FoundOffer foundOffer = new FoundOffer();
        foundOffer.setHyperId(1L);
        offerAuctionStats.setOffer(foundOffer);
        SearchResults searchResults = new SearchResults();
        BidRecommendations recommendations =
                new BidRecommendations(
                        Arrays.asList(offerAuctionStats),
                        searchResults,
                        Arrays.asList(foundOffer)
                );
        handler.handleRecommendations(offer, recommendations);
        assertNull(handler.getRecommendations().getRecommendations().get(0).getError());
    }
}
