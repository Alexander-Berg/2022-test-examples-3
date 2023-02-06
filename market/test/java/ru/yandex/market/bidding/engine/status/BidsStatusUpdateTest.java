package ru.yandex.market.bidding.engine.status;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.bidding.ExchangeProtos;
import ru.yandex.market.bidding.TimeUtils;
import ru.yandex.market.bidding.engine.model.BidBuilder;
import ru.yandex.market.bidding.model.Place;
import ru.yandex.market.bidding.model.PublicationStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BidsStatusUpdateTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testAdd() throws Exception {
        int now = TimeUtils.nowUnixTime();
        UpdateBuffer statusUpdate = new UpdateBuffer(UpdateBuffer.Kind.DELTA);
        statusUpdate.setPublicationTime(now);

        ExchangeProtos.Bid.Builder bid = ExchangeProtos.Bid.newBuilder();
        bid.setPartnerId(1);
        bid.setPartnerType(ExchangeProtos.Bid.PartnerType.SHOP);
        bid.setDomainType(ExchangeProtos.Bid.DomainType.FEED_OFFER_ID);
        Integer offerId = 123;
        bid.setDomainId(String.valueOf(offerId));
        int feedId = 789;
        bid.setFeedId(feedId);
        bid.addDomainIds(String.valueOf(feedId));
        bid.addDomainIds(String.valueOf(offerId));
        bid.setTarget(ExchangeProtos.Bid.BidTarget.OFFER);
        ExchangeProtos.Bid.Value.Builder details = ExchangeProtos.Bid.Value.newBuilder();
        details.setValue(17);
        details.setDeltaOperation(ExchangeProtos.Bid.DeltaOperation.MODIFY);
        details.setModificationTime(now - 120);
        details.setPublicationStatus(ExchangeProtos.Bid.PublicationStatus.APPLIED);
        bid.setValueForSearch(details.build());
        statusUpdate.add(bid.build());

        List<OfferBidUpdate> updates = statusUpdate.offerBids();
        assertTrue(updates.size() == 1);
        OfferBidUpdate update = updates.get(0);
        assertTrue(update.isFast());
        assertEquals(feedId, update.feed());
        assertEquals(123, update.id());
        assertEquals(0, update.switchFeedId());
        assertNull(update.switchOfferId());
        BidBuilder.PlaceBid placeBid = update.getPlaceBid(Place.SEARCH);
        assertFalse(placeBid.none());
        Assert.assertEquals(Place.SEARCH.stopValue(), placeBid.value());
        assertEquals(now - 120, placeBid.modTime());
        assertEquals(PublicationStatus.APPLIED, placeBid.status());
        assertEquals(17, placeBid.pubValue());
        assertEquals(now, placeBid.pubTime());

        placeBid = update.getPlaceBid(Place.CARD);
        assertTrue(placeBid.none());
        placeBid = update.getPlaceBid(Place.MARKET_SEARCH);
        assertTrue(placeBid.none());
    }

    @Test
    public void testAddTitleBid() throws Exception {
        int now = TimeUtils.nowUnixTime();
        UpdateBuffer statusUpdate = new UpdateBuffer(UpdateBuffer.Kind.SNAPSHOT);
        statusUpdate.setPublicationTime(now);

        ExchangeProtos.Bid.Builder bid = ExchangeProtos.Bid.newBuilder();
        bid.setPartnerId(1);
        bid.setPartnerType(ExchangeProtos.Bid.PartnerType.SHOP);
        bid.setDomainType(ExchangeProtos.Bid.DomainType.OFFER_TITLE);
        String offerId = "TITLE";
        bid.setDomainId(offerId);
        bid.addDomainIds(offerId);
        int feedId = 789;
        bid.setSwitchFeedId(feedId);
        bid.setSwitchOfferId("123");
        bid.setTarget(ExchangeProtos.Bid.BidTarget.OFFER);
        ExchangeProtos.Bid.Value.Builder details = ExchangeProtos.Bid.Value.newBuilder();
        details.setValue(17);
        details.setDeltaOperation(ExchangeProtos.Bid.DeltaOperation.MODIFY);
        details.setModificationTime(now - 120);
        details.setPublicationStatus(ExchangeProtos.Bid.PublicationStatus.APPLIED);
        bid.setValueForSearch(details.build());
        statusUpdate.add(bid.build());

        List<OfferBidUpdate> updates = statusUpdate.offerBids();
        assertTrue(updates.size() == 1);
        OfferBidUpdate update = updates.get(0);
        assertFalse(update.isFast());
        assertEquals(0, update.feed());
        assertEquals("TITLE", update.id());
        assertEquals(feedId, update.switchFeedId());
        assertEquals("123", update.switchOfferId());
        BidBuilder.PlaceBid placeBid = update.getPlaceBid(Place.SEARCH);
        assertFalse(placeBid.none());
        Assert.assertEquals(Place.SEARCH.stopValue(), placeBid.value());
        assertEquals(now - 120, placeBid.modTime());
        assertEquals(PublicationStatus.APPLIED, placeBid.status());
        assertEquals(17, placeBid.pubValue());
        assertEquals(now, placeBid.pubTime());

        placeBid = update.getPlaceBid(Place.CARD);
        assertEquals(0, placeBid.pubValue());
        placeBid = update.getPlaceBid(Place.MARKET_SEARCH);
        assertEquals(0, placeBid.pubValue());
    }
}