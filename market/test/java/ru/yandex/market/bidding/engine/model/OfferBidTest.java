package ru.yandex.market.bidding.engine.model;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: snoop
 * Date: 4/1/15
 * Time: 1:44 PM
 */
public class OfferBidTest {

    @Test
    public void testToBuilder() throws Exception {
        //TODO
    }

    @Test
    public void testSwitchTitleToNumId() {
        OfferBid titleBid = new OfferBidBuilder().
                feedId(0).
                setId("Test").
                setSwitchFeedId(10).
                setSwitchOfferId("20").build(OfferBid.class);

        OfferBid idBid = titleBid.toBuilder().switchTitleToOfferId().build(OfferBid.class);

        Assert.assertEquals("20", String.valueOf(idBid.id()));
        Assert.assertEquals(10, idBid.feed());
        Assert.assertEquals(0, idBid.switchFeedId());
        Assert.assertNull(idBid.switchOfferId());
    }

    @Test
    public void testSwitchTitleToAlphaId() {
        OfferBid titleBid = new OfferBidBuilder().
                feedId(0).
                setId("Test").
                setSwitchFeedId(10).
                setSwitchOfferId("aaa").build(OfferBid.class);

        OfferBid idBid = titleBid.toBuilder().switchTitleToOfferId().build(OfferBid.class);

        Assert.assertEquals("aaa", String.valueOf(idBid.id()));
        Assert.assertEquals(10, idBid.feed());
        Assert.assertEquals(0, idBid.switchFeedId());
        Assert.assertNull(idBid.switchOfferId());
    }
}