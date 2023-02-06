package ru.yandex.market.partner.auction.createupdate;

import java.math.BigInteger;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import ru.yandex.market.core.auction.dto.report.ReportRecommendationsAnswerOrError;
import ru.yandex.market.core.auction.err.BidValueLimitsViolationException;
import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;
import ru.yandex.market.core.auction.model.AuctionBidValuesLimits;
import ru.yandex.market.core.auction.model.AuctionOfferBid;
import ru.yandex.market.core.auction.model.BidPlace;
import ru.yandex.market.core.auction.recommend.BidRecommendations;
import ru.yandex.market.partner.auction.AuctionOffer;
import ru.yandex.market.partner.auction.BidModificationManager;
import ru.yandex.market.partner.auction.BulkUpdateRequest;
import ru.yandex.market.partner.auction.HybridGoal;
import ru.yandex.market.partner.auction.ReportRecommendationService;

import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.createOfferFromBidWithLimits;

/**
 * Тесты метода {@link BidModificationManager#createBidUpdate}
 *
 * @author vbudnev
 */
public class CreateUpdateCommon {

    /**
     * Вспомогательный метод, задача которого - проверка результатов работы метода
     * {@link BidModificationManager#createBidUpdate}, сравнивая ожидаемые значения с результатом подготовки ставки
     * {@link AuctionOfferBid}.
     */
    public static void testCreateBidUpdateMethod(
            BiFunction<HybridGoal, AuctionBidComponentsLink, BulkUpdateRequest> requestSupplier,
            Supplier<AuctionOfferBid> bidSupplier,
            AuctionBidComponentsLink auctionBidComponentsLink,
            HybridGoal goal,
            BigInteger expectedBidValue,
            BigInteger expectedCbidValue,
            BigInteger expectedFeeValue,
            AuctionBidValuesLimits limits,
            ReportRecommendationsAnswerOrError cardRecommendations,
            BidRecommendations parallelRecommendations,
            BidRecommendations marketSearchRecommendations,
            Map<BidPlace, Integer> hybridReferenceValuesMap
    ) throws BidValueLimitsViolationException {

        AuctionOfferBid bid = bidSupplier.get();
        bid.setLinkType(auctionBidComponentsLink);

        AuctionOffer auctionOffer = createOfferFromBidWithLimits(bid, limits);

        auctionOffer.setReferenceValues(hybridReferenceValuesMap);

        ReportRecommendationService.AuctionOfferMerger.mergeCardRecommendationsIntoOffer(
                auctionOffer,
                cardRecommendations
        );
        ReportRecommendationService.AuctionOfferMerger.mergeParallelSearchRecommendationsIntoOffer(auctionOffer, parallelRecommendations);
        ReportRecommendationService.AuctionOfferMerger.mergeMarketSearchRecommendationsIntoOffer(auctionOffer, marketSearchRecommendations);

        BulkUpdateRequest updateRequest = requestSupplier.apply(goal, auctionBidComponentsLink);

        AuctionOfferBid res = BidModificationManager.createBidUpdate(auctionOffer, updateRequest);

        assertNotNull(String.format("update method returned error for goal=%s and linkType=%s", goal, auctionBidComponentsLink), res);

        assertThat(String.format("invalid bid value for bid component for goal=%s and linkType=%s ", goal, auctionBidComponentsLink),
                res.getValues().getPlaceBids(), hasEntry(BidPlace.SEARCH, expectedBidValue)
        );
        assertThat(String.format("invalid bid value for cbid component for goal=%s and linkType=%s", goal, auctionBidComponentsLink),
                res.getValues().getPlaceBids(), hasEntry(BidPlace.CARD, expectedCbidValue)
        );
        assertThat(String.format("invalid bid value for fee component for goal=%s and linkType=%s ", goal, auctionBidComponentsLink),
                res.getValues().getPlaceBids(), hasEntry(BidPlace.MARKET_PLACE, expectedFeeValue)
        );

    }

}
