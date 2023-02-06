package ru.yandex.market.partner.auction.createupdate;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import ru.yandex.market.core.AbstractParserTest;
import ru.yandex.market.core.auction.err.BidValueLimitsViolationException;
import ru.yandex.market.core.auction.model.AuctionOfferBid;
import ru.yandex.market.partner.auction.AuctionOffer;
import ru.yandex.market.partner.auction.BidModificationManager;
import ru.yandex.market.partner.auction.BulkUpdateRequest;
import ru.yandex.market.partner.auction.HybridGoal;

import static ru.yandex.market.core.auction.model.AuctionBidStatus.PUBLISHED;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.GROUP_ID_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.HYBRID_CARD_FIRST_PAGE;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.HYBRID_CARD_FIRST_PLACE;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.HYBRID_CARD_PREM_FIRST_PLACE;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.LIMITS;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.MARKET_SEARCH_FIRST_PLACE;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.OFFER_NAME_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.PARALLEL_SEARCH_FIRST_PLACE;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.SHOP_ID_774;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.createAuctionOfferBidWithoutValues;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.createOfferFromBidWithLimits;
import static ru.yandex.market.partner.auction.BulkUpdateRequest.Builder.builder;

/**
 * Общие тесты изменения значений ставок с использованием целей.
 *
 * @author vbudnev
 */
public class GeneralGoalModificationTest extends AbstractParserTest {

    /**
     * Цель {@link HybridGoal} задана, но в {@link AuctionOffer} нет рекомендаций этой цели (в тесте не мокаем)
     * - {@link BidModificationManager#createBidUpdate} должен возвращать ошибку (null)
     *
     * @throws BidValueLimitsViolationException
     */
    @Test
    public void test_createBidUpdate_should_returnError_when_passedGoalTargetButRecommendationsFailed()
            throws BidValueLimitsViolationException {
        List<HybridGoal> goals = ImmutableList.of(
                HYBRID_CARD_PREM_FIRST_PLACE,
                HYBRID_CARD_FIRST_PLACE,
                HYBRID_CARD_FIRST_PAGE,
                PARALLEL_SEARCH_FIRST_PLACE,
                MARKET_SEARCH_FIRST_PLACE
        );

        for (HybridGoal goal : goals) {

            AuctionOfferBid bidToBeFilled = createAuctionOfferBidWithoutValues(SHOP_ID_774, GROUP_ID_1, OFFER_NAME_1, PUBLISHED);

            AuctionOffer auctionOffer = createOfferFromBidWithLimits(bidToBeFilled, LIMITS);

            BulkUpdateRequest req = builder().withOfferName(OFFER_NAME_1).withGoal(goal).build();

            AuctionOfferBid res = BidModificationManager.createBidUpdate(auctionOffer, req);
            assertNull(String.format("update method does not return error for goal=%s", goal), res);
        }
    }

    /**
     * Цель {@link HYBRID_CARD_FIRST_PLACE} должна возвращать ошибку, так как репорт на текущий момент не
     * предоставляет эту инфу.
     */
    // @Test
    public void test_createBidUpdate_should_returnError_when_passedFirstPlaceInPriceBlock()
            throws BidValueLimitsViolationException {
    }

}
