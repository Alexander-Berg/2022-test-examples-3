package ru.yandex.market.partner.auction.createupdate;

import org.junit.Test;

import ru.yandex.market.core.AbstractParserTest;
import ru.yandex.market.core.auction.err.BidValueLimitsViolationException;
import ru.yandex.market.core.auction.model.AuctionOfferBid;
import ru.yandex.market.core.auction.model.AuctionOfferId;
import ru.yandex.market.core.auction.model.AuctionOfferIdType;
import ru.yandex.market.core.auction.model.BidPlace;
import ru.yandex.market.partner.auction.AuctionOffer;
import ru.yandex.market.partner.auction.BidModificationManager;
import ru.yandex.market.partner.auction.BulkUpdateRequest;

import static java.math.BigInteger.valueOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.core.auction.model.AuctionBidStatus.PUBLISHED;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BIDREQ_111;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BIDREQ_222;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BIDREQ_333;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BID_CENTS_111;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BID_CENTS_222;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BID_CENTS_333;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.GROUP_ID_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.LIMITS;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.OFFER_NAME_1;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.SHOP_ID_774;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.createAuctionOfferBidWithoutValues;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.createOfferFromBidWithLimits;
import static ru.yandex.market.partner.auction.BulkUpdateRequest.Builder.builder;

/**
 * Изменение значений ставок с использованием явно заданных значений.
 *
 * @author vbudnev
 */
public class CreateUpdateFromValueTest extends AbstractParserTest {

    /**
     * Для офффера с типом идентификации {@link AuctionOfferIdType#TITLE} проверяем, что запрошенные компоненты ставок
     * корректно доезжают через createBidUpdate, если цели не заданы.
     */
    @Test
    public void test_createBidUpdate_should_setAllComponents_whenNoGoalAndTitleOffer() throws BidValueLimitsViolationException {

        BulkUpdateRequest req = builder().withOfferName(OFFER_NAME_1)
                .withBid(BIDREQ_111)
                .withCbid(BIDREQ_222)
                .withFee(BIDREQ_333)
                .build();

        AuctionOfferBid bidToBeFilled = createAuctionOfferBidWithoutValues(SHOP_ID_774, GROUP_ID_1, OFFER_NAME_1, PUBLISHED);

        AuctionOffer auctionOffer = createOfferFromBidWithLimits(bidToBeFilled, LIMITS);

        Object res = BidModificationManager.createBidUpdate(auctionOffer, req);
        assertNotNull("update method returned error", res);
        assertThat("bid комопнента", bidToBeFilled.getValues().getPlaceBids(), hasEntry(BidPlace.SEARCH, valueOf(BID_CENTS_111)));
        assertThat("cbid компонента", bidToBeFilled.getValues().getPlaceBids(), hasEntry(BidPlace.CARD, valueOf(BID_CENTS_222)));
        assertThat("fee компонента", bidToBeFilled.getValues().getPlaceBids(), hasEntry(BidPlace.MARKET_PLACE, valueOf(BID_CENTS_333)));
        assertThat("offerTitle", bidToBeFilled.getOfferId().getId(), is(OFFER_NAME_1));
    }

    /**
     * Для офффера с типом идентификации {@link AuctionOfferIdType#SHOP_OFFER_ID} проверяем, что запрошенные компоненты ставок
     * корректно доезжают через createBidUpdate, если цели не заданы.
     */
    @Test
    public void test_createBidUpdate_should_setAllComponents_whenNoGoalAndIdOffer() throws BidValueLimitsViolationException {

        final AuctionOfferId offerId = new AuctionOfferId(1234L, "someOfferId");
        BulkUpdateRequest req = builder()
                .withOfferId(offerId.getId())
                .withFeedId(offerId.getFeedId())
                .withBid(BIDREQ_111)
                .withCbid(BIDREQ_222)
                .build();

        AuctionOfferBid bidToBeFilled = createAuctionOfferBidWithoutValues(SHOP_ID_774, GROUP_ID_1, offerId, PUBLISHED);

        AuctionOffer auctionOffer = createOfferFromBidWithLimits(bidToBeFilled, LIMITS);

        Object res = BidModificationManager.createBidUpdate(auctionOffer, req);
        assertNotNull("update method returned error", res);
        assertThat("bid комопнента", bidToBeFilled.getValues().getPlaceBids(),
                hasEntry(BidPlace.SEARCH, valueOf(BID_CENTS_111)));
        assertThat("cbid компонента", bidToBeFilled.getValues().getPlaceBids(),
                hasEntry(BidPlace.CARD, valueOf(BID_CENTS_222)));
        assertThat("id", bidToBeFilled.getOfferId().getId(), is("someOfferId"));
        assertThat("feedId", bidToBeFilled.getOfferId().getFeedId(), is(1234L));
    }
}