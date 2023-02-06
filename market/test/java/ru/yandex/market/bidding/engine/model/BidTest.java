package ru.yandex.market.bidding.engine.model;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.bidding.TimeUtils;
import ru.yandex.market.bidding.model.Place;
import ru.yandex.market.bidding.model.PublicationStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BidTest {

    static BidBuilder fill(BidBuilder bid, Set<Place> places, int modTime) {
        for (Place place : places) {
            bid.setPlaceBid(place, new BidBuilder.PlaceBid((short) 15).setModTime(modTime));
        }
        return bid;
    }

    static BidBuilder fill(BidBuilder bid, Set<Place> places) {
        for (Place place : places) {
            bid.setPlaceBid(place, new BidBuilder.PlaceBid((short) 15));
        }
        return bid;
    }

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testApplied() {
        BidBuilder bid = ModelLayoutTest.exampleBid();
        assertTrue(fill(bid, Place.ALL).build(Bid.class).applied());
    }

    @Test
    public void testCompress() {
        BidBuilder bid = ModelLayoutTest.exampleBid();
        fill(bid, Place.ALL);
        bid.setPlaceBid(Place.SEARCH, BidBuilder.PlaceBid.undefined());
        bid.compress();
        assertEquals(Place.ALL.size() - 1, bid.count());
        final Iterator<Map.Entry<Place, BidBuilder.PlaceBid>> data = bid.data();
        EnumSet<Place> places = EnumSet.allOf(Place.class);
        while (data.hasNext()) {
            places.remove(data.next().getKey());
        }
        assertEquals(1, places.size());
        assertEquals(Place.SEARCH, places.iterator().next());
    }

    @Test
    public void testCleanDummy() {
        BidBuilder bid = ModelLayoutTest.exampleBid();
        fill(bid, Place.ALL);
        int now = TimeUtils.nowUnixTime();
        bid.setPlaceBid(Place.SEARCH,
                BidBuilder.PlaceBid.undefined().
                        setValue(Place.SEARCH.stopValue()).
                        setStatus(PublicationStatus.APPLIED).setModTime(now - 1_000 * 60 * 60));
        bid.cleanDummy(now);
        assertEquals(Place.ALL.size() - 1, bid.count());
        final Iterator<Map.Entry<Place, BidBuilder.PlaceBid>> data = bid.data();
        EnumSet<Place> places = EnumSet.allOf(Place.class);
        while (data.hasNext()) {
            places.remove(data.next().getKey());
        }
        assertEquals(1, places.size());
        assertEquals(Place.SEARCH, places.iterator().next());
    }

    @Test
    public void testPending() {
        BidBuilder bid = ModelLayoutTest.exampleBid();
        fill(bid, Place.ALL, TimeUtils.nowUnixTime());
        bid.getPlaceBid(Place.MARKET_SEARCH).setStatus(PublicationStatus.PENDING);
        final Bid item = bid.build(Bid.class);
        assertTrue(item.pending(true) > 0);
        assertFalse(item.pending(false) > 0);
    }

    @Test
    public void testPendingTime() {
        BidBuilder bid = ModelLayoutTest.exampleBid();
        fill(bid, Place.ALL);
        int now = TimeUtils.nowUnixTime();
        bid.getPlaceBid(Place.MARKET_SEARCH).setStatus(PublicationStatus.PENDING).setModTime(now - 10);
        bid.getPlaceBid(Place.SEARCH).setStatus(PublicationStatus.PENDING).setModTime(now);
        final Bid item = bid.build(Bid.class);
        assertTrue(item.pending(true) > 0);
        assertEquals(now - 10, item.pending(true));
        assertTrue(item.pending(false) > 0);
        assertEquals(now, item.pending(false));
    }

    @Test
    public void testLastModificationTime() {
        BidBuilder bid = ModelLayoutTest.exampleBid();
        fill(bid, Place.ALL);
        int now = TimeUtils.nowUnixTime();
        bid.getPlaceBid(Place.SEARCH).setModTime(now - 10);
        bid.getPlaceBid(Place.MARKET_SEARCH).setModTime(now);
        final Bid item = bid.build(Bid.class);
        assertEquals(now, item.lastModificationTime(true));
        assertEquals(now - 10, item.lastModificationTime(false));
    }
}