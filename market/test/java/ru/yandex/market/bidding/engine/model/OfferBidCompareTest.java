package ru.yandex.market.bidding.engine.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.bidding.engine.OfferKeys;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class OfferBidCompareTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testTitleTypes() throws Exception {
        OfferBidBuilder builder = new OfferBidBuilder();
        builder.setId("ABC");
        OfferBid firstBid = builder.build(OfferBid.class);
        builder = new OfferBidBuilder();
        builder.setId("DEF");
        OfferBid secondBid = builder.build(OfferBid.class);
        assertTrue(OfferKeys.CMP.compare(secondBid, firstBid) > 0);

        builder = new OfferBidBuilder();
        builder.setId("DEF");
        firstBid = builder.build(OfferBid.class);
        builder = new OfferBidBuilder();
        builder.setId("ABC");
        secondBid = builder.build(OfferBid.class);
        assertTrue(OfferKeys.CMP.compare(firstBid, secondBid) > 0);

        builder = new OfferBidBuilder();
        builder.setId("DEF");
        firstBid = builder.build(OfferBid.class);
        builder = new OfferBidBuilder();
        builder.setId("DEF");
        secondBid = builder.build(OfferBid.class);
        assertTrue(OfferKeys.CMP.compare(firstBid, secondBid) == 0);
    }

    @Test
    public void testAlphaIdTypes() throws Exception {
        OfferBidBuilder builder = new OfferBidBuilder();
        builder.feedId(10);
        builder.setId("ABC");
        OfferBid firstBid = builder.build(OfferBid.class);
        builder = new OfferBidBuilder();
        builder.feedId(10);
        builder.setId("DEF");
        OfferBid secondBid = builder.build(OfferBid.class);
        assertTrue(OfferKeys.CMP.compare(secondBid, firstBid) > 0);

        builder = new OfferBidBuilder();
        builder.feedId(10);
        builder.setId("ABC");
        firstBid = builder.build(OfferBid.class);
        builder = new OfferBidBuilder();
        builder.feedId(9);
        builder.setId("DEF");
        secondBid = builder.build(OfferBid.class);
        assertTrue(OfferKeys.CMP.compare(firstBid, secondBid) > 0);

        builder = new OfferBidBuilder();
        builder.feedId(10);
        builder.setId("ABC");
        firstBid = builder.build(OfferBid.class);
        builder = new OfferBidBuilder();
        builder.feedId(10);
        builder.setId("ABC");
        secondBid = builder.build(OfferBid.class);
        assertTrue(OfferKeys.CMP.compare(firstBid, secondBid) == 0);
    }

    @Test
    public void testNumberIdTypes() throws Exception {
        OfferBidBuilder builder = new OfferBidBuilder();
        builder.feedId(10);
        builder.setId("1223");
        OfferBid firstBid = builder.build(OfferBid.class);
        builder = new OfferBidBuilder();
        builder.feedId(10);
        builder.setId("1213");
        OfferBid secondBid = builder.build(OfferBid.class);
        assertTrue(OfferKeys.CMP.compare(firstBid, secondBid) > 0);

        builder = new OfferBidBuilder();
        builder.feedId(10);
        builder.setId("1223");
        firstBid = builder.build(OfferBid.class);
        builder = new OfferBidBuilder();
        builder.feedId(11);
        builder.setId("1213");
        secondBid = builder.build(OfferBid.class);
        assertTrue(OfferKeys.CMP.compare(secondBid, firstBid) > 0);
    }

    @Test
    public void testNumberMixTypes() throws Exception {
        OfferBidBuilder builder = new OfferBidBuilder();
        builder.feedId(10);
        builder.setId("1223");
        OfferBid firstBid = builder.build(OfferBid.class);
        builder = new OfferBidBuilder();
        builder.feedId(10);
        builder.setId("001");
        OfferBid secondBid = builder.build(OfferBid.class);
        assertTrue(OfferKeys.CMP.compare(firstBid, secondBid) > 0);

        builder = new OfferBidBuilder();
        builder.feedId(9);
        builder.setId("1223");
        firstBid = builder.build(OfferBid.class);
        builder = new OfferBidBuilder();
        builder.feedId(10);
        builder.setId("001");
        secondBid = builder.build(OfferBid.class);
        assertTrue(OfferKeys.CMP.compare(secondBid, firstBid) > 0);
    }

    @Test
    public void testOfferBidMixTypes() throws Exception {
        OfferBidBuilder builder = new OfferBidBuilder();
        builder.setId("ABC");
        OfferBid titleBid = builder.build(OfferBid.class);
        builder = new OfferBidBuilder();
        builder.feedId(10);
        builder.setId("ABC");
        OfferBid alphaIdBid = builder.build(OfferBid.class);
        builder = new OfferBidBuilder();
        builder.feedId(10);
        builder.setId("123");
        OfferBid numIdBid = builder.build(OfferBid.class);
        List<OfferBid> bids = new ArrayList<>();
        bids.add(numIdBid);
        bids.add(alphaIdBid);
        bids.add(titleBid);
        Collections.sort(bids, OfferKeys.CMP);
        assertSame(titleBid, bids.get(0));
        assertSame(alphaIdBid, bids.get(1));
        assertSame(numIdBid, bids.get(2));
    }
}