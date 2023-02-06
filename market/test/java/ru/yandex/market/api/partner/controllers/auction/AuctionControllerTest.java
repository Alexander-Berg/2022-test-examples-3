package ru.yandex.market.api.partner.controllers.auction;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

import ru.yandex.market.api.partner.auth.AuthPrincipal;
import ru.yandex.market.api.partner.controllers.auction.model.BidAdjustment;
import ru.yandex.market.api.partner.controllers.auction.model.BidRecommendation;
import ru.yandex.market.api.partner.controllers.auction.model.ImpossibleRecommendation;
import ru.yandex.market.api.partner.controllers.auction.model.OfferBid;
import ru.yandex.market.api.partner.controllers.auction.model.OfferBids;
import ru.yandex.market.api.partner.controllers.auction.model.OfferBidsRecommendation;
import ru.yandex.market.api.partner.controllers.auction.model.OfferBidsRecommendations;
import ru.yandex.market.api.partner.controllers.auction.model.RecommendationTarget;
import ru.yandex.market.api.partner.controllers.auction.model.recommender.ApiAuctionAbstractRecommender;
import ru.yandex.market.api.partner.controllers.util.FeedHelper;
import ru.yandex.market.api.partner.request.PartnerServletRequest;
import ru.yandex.market.core.auction.BidLimits;
import ru.yandex.market.core.auction.BidLimitsService;
import ru.yandex.market.core.auction.bidding.BiddingRemoteAuctionService;
import ru.yandex.market.core.auction.err.AuctionValidationException;
import ru.yandex.market.core.auction.model.AuctionBidValuesLimits;
import ru.yandex.market.core.auction.model.AuctionNAReason;
import ru.yandex.market.core.auction.model.AuctionOfferId;
import ru.yandex.market.core.auction.model.AuctionOfferIdType;
import ru.yandex.market.core.auction.model.BidPlace;
import ru.yandex.market.core.auction.recommend.BidRecommendator;

import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.auction.model.BidPlace.CARD;
import static ru.yandex.market.core.auction.model.BidPlace.MARKET_PLACE;
import static ru.yandex.market.core.auction.model.BidPlace.SEARCH;
import static ru.yandex.market.mbi.util.MoneyValuesHelper.centsToUE;

/**
 * @author kudrale
 */
@ExtendWith(MockitoExtension.class)
public class AuctionControllerTest {
    private static final BigDecimal BID_UE_1_11 = centsToUE(111);
    private static final BigDecimal BID_UE_2_22 = centsToUE(222);
    private static final BigDecimal BID_UE_3_33 = centsToUE(333);
    private static final BigDecimal BID_UE_4_44 = centsToUE(444);


    @Mock
    private PartnerServletRequest partnerServletRequest;

    @Spy
    private BiddingRemoteAuctionService auctionService = new BiddingRemoteAuctionService();

    @Mock
    private FeedHelper feedHelper;

    @Mock
    private BidRecommendator marketCardBidRecommendator;

    @Mock
    private BidLimits bidLimits;

    @InjectMocks
    private AuctionController auctionController = new AuctionController() {
        {
            setMaxOffersBulkSize(100);
        }
    };


    public void assertThrows(Runnable block) {
        try {
            block.run();
            fail("Block didn't throw.");
        } catch (Exception ignored) {
        }
    }

    @Test
    void setBidsTest() throws AuctionValidationException {
        final OfferBids offerBids = new OfferBids();
        OfferBid offerBid = new OfferBid();
        offerBid.setBidIfNotNull(SEARCH, BigDecimal.ONE);
        offerBid.setOfferId(new AuctionOfferId(100L, "offer"));
        offerBids.setBids(singleton(offerBid));

        final AuthPrincipal user = new AuthPrincipal(10000L) {

        };

        when(feedHelper.getFeedIds(anyLong())).thenReturn(new HashSet<>(singleton(100L)));
        when(bidLimits.limits()).thenReturn(new AuctionBidValuesLimits());
        when(marketCardBidRecommendator.calculate(Mockito.any())).thenReturn(
                CompletableFuture.completedFuture(ApiAuctionAbstractRecommender.RECOMMENDATIONS_FAILED));

        //can't use when...then on spy objects
        doReturn(AuctionNAReason.NONE).when(auctionService).canManageAuction(anyLong());
        doReturn(AuctionOfferIdType.SHOP_OFFER_ID).when(auctionService).getAuctionOfferIdType(anyLong());
        doReturn(false).when(auctionService).canMakeMbid(anyLong());
        assertThrows(() -> {
            try {
                auctionController.setBids(partnerServletRequest, 1000L, offerBids, user);
            } catch (AuctionValidationException e) {
                throw new RuntimeException(e);
            }
        });
        verify(auctionService, never()).setOfferBids(anyLong(), anyList(), anyLong());
    }

    @Test
    void buildOfferBids() {
        AuctionController controller = new AuctionControllerV1();
        BidLimitsService limits = new BidLimitsService();
        limits.setDefaultMaxBid(8400);
        limits.setDefaultMinBid(1);
        Map<BidPlace, Integer> minBids = new EnumMap<>(BidPlace.class);
        minBids.put(MARKET_PLACE, 100);
        limits.setMinBids(minBids);
        Map<BidPlace, Integer> maxBids = new EnumMap<>(BidPlace.class);
        maxBids.put(MARKET_PLACE, 100);
        limits.setMaxBids(maxBids);
        limits.afterPropertiesSet();
        controller.setBidLimits(limits);

        OfferBidsRecommendation recommendation = new OfferBidsRecommendation(RecommendationTarget.MODEL_CARD);
        recommendation.setMinimal(SEARCH, BigDecimal.valueOf(5, 2));
        recommendation.setMinimal(CARD, BigDecimal.valueOf(7, 2));
        final BigDecimal bid = BigDecimal.valueOf(60, 2);
        BidRecommendation position = new BidRecommendation(bid, null);
        recommendation.setRecommendation(2, position);
        OfferBidsRecommendations recommendations = new OfferBidsRecommendations(Collections.singletonList(recommendation), false);
        BidAdjustment adjustment = new BidAdjustment(null, null, null);
        OfferBids bids = controller.buildOfferBids(recommendations, adjustment, RecommendationTarget.MODEL_CARD);
        final OfferBid offerBid = bids.getBids().iterator().next();
        assertEquals(bid, offerBid.getBid(CARD));
    }

    @DisplayName("Объединеие двух блоков поисковых рекомендаций")
    @Test
    void test_mergeParallelSearchAndMarketSearchRecommendations_should_mergeMarketSearchInfoIntoParallel() {
        //prepare
        AuctionController controller = new AuctionControllerV1();
        OfferBidsRecommendation parallelRecommendation = new OfferBidsRecommendation(RecommendationTarget.SEARCH);
        OfferBidsRecommendation marketSearchRecommendation
                = new OfferBidsRecommendation(RecommendationTarget.MARKET_SEARCH);

        parallelRecommendation.setActual(SEARCH, BID_UE_1_11);
        parallelRecommendation.setMinimal(SEARCH, BID_UE_2_22);
        final OfferBidsRecommendation.TargetRecommendation SOME_TARGET_SEARCH_RECOMMENDATION = new OfferBidsRecommendation.TargetRecommendation();
        parallelRecommendation.setTargetRecommendation(RecommendationTarget.SEARCH, SOME_TARGET_SEARCH_RECOMMENDATION);

        marketSearchRecommendation.setActual(SEARCH, BID_UE_2_22);
        marketSearchRecommendation.setMinimal(SEARCH, BID_UE_1_11);
        marketSearchRecommendation.setActual(MARKET_PLACE, BID_UE_3_33);
        marketSearchRecommendation.setMinimal(MARKET_PLACE, BID_UE_4_44);

        final OfferBidsRecommendation.TargetRecommendation SOME_TARGET_MARKET_SEARCH_RECOMMENDATION = new OfferBidsRecommendation.TargetRecommendation();
        marketSearchRecommendation.setTargetRecommendation(RecommendationTarget.MARKET_SEARCH, SOME_TARGET_MARKET_SEARCH_RECOMMENDATION);

        //run tested method
        controller.mergeParallelSearchAndMarketSearchRecommendations(
                new OfferBidsRecommendations(Collections.singletonList(parallelRecommendation), true),
                new OfferBidsRecommendations(Collections.singletonList(marketSearchRecommendation), true)
        );

        assertThat("Market_search actual fee component must be merged if set", parallelRecommendation.getActual(MARKET_PLACE), is(BID_UE_3_33));
        assertThat("Market_search minimal fee component must be merged if set", parallelRecommendation.getMinimal(MARKET_PLACE), is(BID_UE_4_44));
        assertThat("Market_search minimal bid must replace parallel actual bid if set", parallelRecommendation.getMinimal(SEARCH), is(BID_UE_1_11));
        assertThat("Market_search actual bid must replace parallel actual bid if set", parallelRecommendation.getActual(SEARCH), is(BID_UE_2_22));
        assertEquals(SOME_TARGET_MARKET_SEARCH_RECOMMENDATION, parallelRecommendation.getTargetRecommendation(RecommendationTarget.MARKET_SEARCH), "Market_search target block must be merged");
        assertEquals(SOME_TARGET_SEARCH_RECOMMENDATION, parallelRecommendation.getTargetRecommendation(RecommendationTarget.SEARCH), "Parallel search target block must not be lost");
    }

    /**
     * Тест для {@link AuctionController#mergeParallelSearchAndMarketSearchRecommendations}.
     */
    @DisplayName("Объединение ошибок для нескольких поисковых target'ов")
    @Test
    void test_mergeMultipleTargetErrors() {

        final OfferBidsRecommendation parallelRecommendation = new OfferBidsRecommendation(RecommendationTarget.SEARCH);
        final OfferBidsRecommendation marketSearchRecommendation =
                new OfferBidsRecommendation(RecommendationTarget.MARKET_SEARCH);

        for (ImpossibleRecommendation one : ImpossibleRecommendation.values()) {
            for (ImpossibleRecommendation other : ImpossibleRecommendation.values()) {
                parallelRecommendation.setError(one);
                marketSearchRecommendation.setError(other);
                auctionController.mergeParallelSearchAndMarketSearchRecommendations(
                        new OfferBidsRecommendations(Collections.singletonList(parallelRecommendation), true),
                        new OfferBidsRecommendations(Collections.singletonList(marketSearchRecommendation), true)
                );

                if (one.equals(other)) {
                    assertThat(
                            "Resulting error must be set when both are equal",
                            parallelRecommendation.getError(),
                            is(one)
                    );
                } else {
                    assertThat(
                            "Resulting error must NOT be set when both are NOT equal",
                            parallelRecommendation.getError(),
                            is(nullValue())
                    );
                }
            }
        }

    }
}
