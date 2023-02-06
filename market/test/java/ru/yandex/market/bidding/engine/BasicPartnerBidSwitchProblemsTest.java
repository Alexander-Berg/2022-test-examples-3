package ru.yandex.market.bidding.engine;

import java.util.Map;
import java.util.function.Supplier;

import org.junit.Test;

import ru.yandex.market.bidding.engine.model.BidBuilder;
import ru.yandex.market.bidding.engine.model.OfferBid;
import ru.yandex.market.bidding.engine.model.OfferBidBuilder;
import ru.yandex.market.bidding.model.BidSwitchProblemCode;
import ru.yandex.market.bidding.model.Place;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BasicPartnerBidSwitchProblemsTest extends BasicShopTestCase {
    /**
     * Проблем переключения быть не может - все offerId ставки будут удалены
     */
    @Test
    public void testIdToTitle() {
        BasicPartner shop = createShop(10, idx -> idBid("" + idx, 1));
        assertEquals(0, shop.switchToOfferIdProblems().size());
    }

    @Test
    public void testTitleToIdOk() {
        assertAllCodesAre(
                idx -> bid(idx, this::appliedBid, Place.SEARCH, 1, "" + idx),
                BidSwitchProblemCode.OK);
    }

    /**
     * Добавляем ещё не применённые ставки
     */
    @Test
    public void testBidNotAppliedYet() {
        assertAllCodesAre(
                idx -> bid(idx, this::notEverAppliedBid, Place.CARD, 0, null),
                BidSwitchProblemCode.NOT_PUBLISHED);
    }

    /**
     * Добавляем применённые ставки без switchOfferId
     */
    @Test
    public void testNoOfferId() {
        assertAllCodesAre(
                idx -> appliedTitleBid("Bid_" + idx, 0, 1, null),
                BidSwitchProblemCode.NO_OFFER_ID);
    }

    /**
     * Ставка с неправильным значением
     */
    @Test
    public void testNotAllowedBidValue() {
        assertAllCodesAre(
                idx -> bid(idx, this::notAllowedValueBid, Place.MARKET_SEARCH, 0, null),
                BidSwitchProblemCode.NO_OFFER_ID);
    }

    /**
     * Добавляем ставки на товары, не найденные в индексе
     */
    @Test
    public void testNotFound() {
        assertAllCodesAre(
                idx -> bid(idx, this::notFoundBid, Place.MARKET_PLACE, 0, null),
                BidSwitchProblemCode.NOT_FOUND);
    }

    /**
     * Ставки с разными проблемами
     */
    @Test
    public void testMixedProblems() {
        BasicPartner shop = createShop(
                idx -> bid(idx, this::appliedBid, Place.SEARCH, 1, "" + idx),
                idx -> bid(idx, this::notEverAppliedBid, Place.CARD, 0, null),
                idx -> appliedTitleBid("Bid_" + idx, 0, 1, null),
                idx -> bid(idx, this::notFoundBid, Place.SEARCH, 0, null)
        );

        Map<BidSwitchProblemCode, Integer> problems = shop.switchToOfferIdProblems();
        assertEquals(problems.toString(), 4, problems.size());

        for (BidSwitchProblemCode code : BidSwitchProblemCode.values()) {
            if (code != BidSwitchProblemCode.ALL) {
                assertOne(problems, code);
            }
        }
    }

    @Test
    public void testUseWhorseStatus1() {
        /*
        Берётся минимальный статус. Вот таблица статусов:

        PENDING(1),
        APPLIED(0),
        NOT_ALLOWED(-1),
        NOT_FOUND(-2),
        WRONG_BID_VALUE(-3)
        */

        // ставка применена в индексаторе, но отсутствует switchFeedId/switchOfferId - непонятная проблема
        assertStatus(this::notEverAppliedBid, this::appliedBid, BidSwitchProblemCode.NO_OFFER_ID);
        assertStatus(this::notAllowedValueBid, this::appliedBid, BidSwitchProblemCode.NO_OFFER_ID);
        assertStatus(this::notFoundBid, this::appliedBid, BidSwitchProblemCode.NOT_FOUND);

        assertStatus(this::notFoundBid, this::notEverAppliedBid, BidSwitchProblemCode.NOT_FOUND);
        assertStatus(this::notAllowedValueBid, this::notEverAppliedBid, BidSwitchProblemCode.NO_OFFER_ID);

        assertStatus(this::notFoundBid, this::notAllowedValueBid, BidSwitchProblemCode.NOT_FOUND);
    }


    protected void assertAllCodesAre(BidFactory factory, BidSwitchProblemCode code) {
        Integer count = 10;
        BasicPartner shop = createShop(count, factory);

        Map<BidSwitchProblemCode, Integer> problems = shop.switchToOfferIdProblems();
        assertEquals(problems.toString(), 1, problems.size());
        Integer notPublishedCount = problems.get(code);
        assertNotNull(problems.toString(), notPublishedCount);
        assertEquals(count, notPublishedCount);
    }


    protected void assertStatus(Supplier<BidBuilder.PlaceBid> bid, Supplier<BidBuilder.PlaceBid> cbid, BidSwitchProblemCode code) {
        BasicPartner shop = createShop(
                idx -> bid(idx,
                        bid,
                        cbid,
                        0, null)
        );
        Map<BidSwitchProblemCode, Integer> problems = shop.switchToOfferIdProblems();
        assertEquals(problems.toString(), 1, problems.size());
        assertOne(problems, code);
    }


    private void assertOne(Map<BidSwitchProblemCode, Integer> problems, BidSwitchProblemCode code) {
        Integer cnt = problems.get(code);
        assertNotNull(code + " in " + problems.toString(), cnt);
        assertEquals(1, (int) cnt);
    }

    protected OfferBid bid(int idx,
                           Supplier<BidBuilder.PlaceBid> bid,
                           Place place,
                           int switchFeedId,
                           String switchOfferId) {
        OfferBidBuilder builder = new OfferBidBuilder();
        builder.setPlaceBid(place, bid.get());
        return titleBid("Bid_" + idx, 0, switchFeedId, switchOfferId, builder);
    }


    protected OfferBid bid(int idx,
                           Supplier<BidBuilder.PlaceBid> bid,
                           Supplier<BidBuilder.PlaceBid> cbid,
                           int switchFeedId,
                           String switchOfferId) {
        OfferBidBuilder builder = new OfferBidBuilder();
        builder.setPlaceBid(Place.SEARCH, bid.get());
        builder.setPlaceBid(Place.CARD, cbid.get());
        return titleBid("Bid_" + idx, 0, switchFeedId, switchOfferId, builder);
    }

}