package ru.yandex.market.bidding.engine.statistics;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.bidding.BiddingApi;
import ru.yandex.market.bidding.engine.BasicPartner;
import ru.yandex.market.bidding.engine.OfferKeys;
import ru.yandex.market.bidding.engine.Profile;
import ru.yandex.market.bidding.engine.model.BidBuilder;
import ru.yandex.market.bidding.engine.model.OfferBid;
import ru.yandex.market.bidding.engine.model.OfferBidBuilder;
import ru.yandex.market.bidding.model.Place;
import ru.yandex.market.bidding.model.PublicationStatus;
import ru.yandex.market.metrics.TimeLine;
import ru.yandex.market.metrics.TimeLineHistogram;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.bidding.model.PublicationStatus.APPLIED;
import static ru.yandex.market.bidding.model.PublicationStatus.NOT_ALLOWED;
import static ru.yandex.market.bidding.model.PublicationStatus.NOT_FOUND;
import static ru.yandex.market.bidding.model.PublicationStatus.PENDING;

/**
 * Created with IntelliJ IDEA.
 * User: snoop
 * Date: 8/18/15
 * Time: 7:18 PM
 */
public class AuctionPerformanceStatisticsTest {

    private List<BasicPartner> shops = new ArrayList<>();
    private LocalDateTime now;
    private LocalDateTime modifiedSince;
    private LocalDateTime publishedSince;
    private LocalDateTime effectiveTime;
    private int epoch;
    private Random rnd = new Random();
    private AtomicInteger counter = new AtomicInteger(1);

    @Before
    public void setUp() {
        now = LocalDateTime.of(2015, Month.AUGUST, 10, 17, 45, 05);
        modifiedSince = now.minus(10, ChronoUnit.MINUTES);
        publishedSince = modifiedSince;
        effectiveTime = now.minus(1, ChronoUnit.HOURS);
        epoch = AuctionPerformanceStatistics.unixTime(modifiedSince);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testWrite() throws Exception {
        BasicPartner.Builder shop = new BasicPartner.Builder();
        OfferBidBuilder bid = new OfferBidBuilder().setPlaceBid(Place.SEARCH, place(10, false, APPLIED));
        bid.feedId(OfferKeys.SYNTHETIC_FEED).setId(String.valueOf(BiddingApi.CATEGORY_BOOK));
        shop.addCategoryBid(bid.build(OfferBid.class));
        OfferBidBuilder oBid = new OfferBidBuilder().setPlaceBid(Place.CARD, place(25, true, NOT_FOUND));
        shop.addOfferBid(slow(oBid).build(OfferBid.class));
        oBid = new OfferBidBuilder().setPlaceBid(Place.CARD, place(20, true, NOT_FOUND));
        oBid.setPlaceBid(Place.MARKET_SEARCH, place(20, false, NOT_ALLOWED));
        oBid.setPlaceBid(Place.MARKET_PLACE, place(-20, true, PENDING));
        shop.addOfferBid(slow(oBid).build(OfferBid.class));
        shops.add(shop.build());

        shop = new BasicPartner.Builder().profile(new Profile.Builder().marketBid(true));
        oBid = new OfferBidBuilder().setPlaceBid(Place.CARD, place(5, false, APPLIED));
        oBid.setPlaceBid(Place.MARKET_SEARCH, place(10, false, APPLIED));
        oBid.setPlaceBid(Place.MARKET_PLACE, place(-3, true, APPLIED));
        shop.addOfferBid(fast(oBid).build(OfferBid.class));
        shops.add(shop.build());

        AuctionPerformanceStatistics aps = new AuctionPerformanceStatistics(
                modifiedSince, publishedSince, effectiveTime,
                new TimeLine(1, SECONDS, 10),
                new TimeLine(10, SECONDS, 6));
        for (BasicPartner s : shops) {
            aps.accept(s);
            aps.onBid(s.getCategoryBid(BiddingApi.CATEGORY_BOOK));
            for (OfferBid offerBid : s.offerBids()) {
                aps.onOfferBid(offerBid);
            }
            aps.end(s);
        }
        assertEquals(4, aps.total().value());
        assertEquals(1, aps.failed().value());
        assertEquals(2, aps.applied().value());
        assertEquals(2, aps.modified().value());

        TimeLineHistogram tlh = aps.histogram(false, Place.SEARCH);
        List<Pair<Integer, Integer>> histogram = tlh.result();
        assertEquals(1, histogram.size());
        assertEquals(Pair.of(20, 1), histogram.get(0));

        tlh = aps.histogram(false, Place.CARD);
        histogram = tlh.result();
        assertEquals(1, histogram.size());
        assertEquals(Pair.of(30, 2), histogram.get(0));

        assertTrue(aps.histogram(false, Place.MARKET_SEARCH).result().isEmpty());
        assertTrue(aps.histogram(false, Place.MARKET_PLACE).result().isEmpty());

        tlh = aps.histogram(true, Place.CARD);
        histogram = tlh.result();
        assertEquals(1, histogram.size());
        assertEquals(Pair.of(6, 1), histogram.get(0));

        tlh = aps.histogram(true, Place.MARKET_SEARCH);
        histogram = tlh.result();
        assertEquals(1, histogram.size());
        assertEquals(Pair.of(11, 1), histogram.get(0));

        tlh = aps.histogram(true, Place.MARKET_PLACE);
        histogram = tlh.result();
        assertEquals(1, histogram.size());
        assertEquals(Pair.of(0, 1), histogram.get(0));

        assertTrue(aps.histogram(true, Place.SEARCH).result().isEmpty());
    }

    private OfferBidBuilder fast(OfferBidBuilder oBid) {
        return oBid.setId("ABC" + counter.getAndIncrement()).feedId(1);
    }

    private OfferBidBuilder slow(OfferBidBuilder oBid) {
        return oBid.setId("ABC" + counter.getAndIncrement()).feedId(0);
    }

    private BidBuilder.PlaceBid place(int delta, boolean before, PublicationStatus status) {
        final short value = value();
        final int time = time(before ? -1 : 1, SECONDS);
        return new BidBuilder.PlaceBid(value, time, value, time + delta, status);
    }

    private int time(int time, TimeUnit unit) {
        return (int) (epoch + unit.toSeconds(time));
    }

    private short value() {
        return (short) rnd.nextInt(500);
    }
}