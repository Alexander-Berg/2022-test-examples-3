package ru.yandex.market.partner.auction.createupdate;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import ru.yandex.market.core.auction.dto.report.ReportRecommendationsAnswerOrError;
import ru.yandex.market.core.auction.err.BidValueLimitsViolationException;
import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;
import ru.yandex.market.core.auction.model.AuctionGoalPlace;
import ru.yandex.market.core.auction.model.AuctionOfferBid;
import ru.yandex.market.core.auction.model.BidPlace;
import ru.yandex.market.partner.auction.AbstractParserTestExtended;
import ru.yandex.market.partner.auction.AuctionBulkCommon;
import ru.yandex.market.partner.auction.AuctionOffer;
import ru.yandex.market.partner.auction.BidModificationManager;
import ru.yandex.market.partner.auction.BulkUpdateRequest;
import ru.yandex.market.partner.auction.HybridGoal;
import ru.yandex.market.partner.auction.ReportRecommendationService;

import static java.math.BigInteger.valueOf;
import static org.hamcrest.Matchers.hasEntry;
import static ru.yandex.market.core.auction.model.AuctionBidComponentsLink.CARD_NO_LINK_FEE_PRIORITY;
import static ru.yandex.market.core.auction.model.AuctionBidStatus.PUBLISHED;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BIDREQ_111;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BIDREQ_333;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BID_CENTS_222;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BID_CENTS_555;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.GROUP_ID_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.LIMITS;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.OFFER_NAME_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.SHOP_ID_774;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.buildCardRecommendationsFromFile;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.createOfferFromBidWithLimits;
import static ru.yandex.market.partner.auction.BulkUpdateRequest.Builder.builder;
import static ru.yandex.market.partner.auction.HybridGoal.GoalType.HYBRID_CARD;

/**
 * @author vbudnev
 */
class AuctionBulkOfferBidsServantletRecommendationsTest extends AbstractParserTestExtended {

    private static final String HYBRID_FILE_REC_OK = "Card_Recommendations_ok.xml";

    /**
     * Если для компоненты вдруг передаются и цель для рекомендованного и явное значение компоненты
     * - результирующие ставки будут расчитаны на основе цели, явно переданные значения не играют роли.
     */
    @DisplayName("Указание цели имеет больший приоритет чем явное значение")
    @Test
    void test_updateBid_should_setRecommendedComponentValue_when_bothValueAndGoalSpecified()
            throws BidValueLimitsViolationException, InterruptedException, SAXException, ExecutionException, IOException {

        Map<BidPlace, Integer> hybridReferenceValuesMap = ImmutableMap.of(
                BidPlace.CARD, BID_CENTS_222,
                BidPlace.MARKET_PLACE, BID_CENTS_555
        );

        ReportRecommendationsAnswerOrError hybridCardRecommendations
                = buildCardRecommendationsFromFile(getContentStreamFromExplicitFile(HYBRID_FILE_REC_OK));

        AuctionBidComponentsLink testLinkType = CARD_NO_LINK_FEE_PRIORITY;
        AuctionOfferBid bid = AuctionBulkCommon.createAuctionOfferBidWithoutValues(SHOP_ID_774, GROUP_ID_1, OFFER_NAME_1, PUBLISHED);
        bid.setLinkType(testLinkType);
        AuctionOffer auctionOffer = createOfferFromBidWithLimits(bid, LIMITS);

        ReportRecommendationService.AuctionOfferMerger.mergeCardRecommendationsIntoOffer(auctionOffer, hybridCardRecommendations);
        auctionOffer.setReferenceValues(hybridReferenceValuesMap);

        HybridGoal goal = new HybridGoal(HYBRID_CARD, AuctionGoalPlace.PREMIUM_FIRST_PLACE);
        BulkUpdateRequest req = builder().withOfferName(OFFER_NAME_1)
                .withCbid(BIDREQ_111)
                .withFee(BIDREQ_333)
                .withGoal(goal)
                .build();

        AuctionOfferBid res = BidModificationManager.createBidUpdate(auctionOffer, req);
        assertNotNull("update method returned error", res);
        assertThat(String.format("invalid bid value for cbid component for goal=%s", goal),
                res.getValues().getPlaceBids(),
                hasEntry(BidPlace.CARD, valueOf(701))
        );
    }

}