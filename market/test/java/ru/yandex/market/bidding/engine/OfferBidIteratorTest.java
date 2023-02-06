package ru.yandex.market.bidding.engine;

import java.util.Arrays;
import java.util.Iterator;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.UnmodifiableIterator;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.bidding.engine.model.OfferBid;

public class OfferBidIteratorTest {
    @Test
    public void doTestBlank() {
        UnmodifiableIterator<OfferBid> empty = ImmutableSet.<OfferBid>of().iterator();
        BasicPartner.OfferBidIterator iter = new BasicPartner.OfferBidIterator(empty);
        Assert.assertFalse(iter.hasNext());
    }

    @Test
    public void doTestDead1() {
        Iterator<OfferBid> dead = dead1();
        BasicPartner.OfferBidIterator iter = new BasicPartner.OfferBidIterator(dead);
        Assert.assertFalse(iter.hasNext());
    }

    private Iterator<OfferBid> dead1() {
        return Arrays.asList(deadBid()).iterator();
    }

    @Test
    public void doTestDeadOnly() {
        Iterator<OfferBid> dead = deadOnly();
        BasicPartner.OfferBidIterator iter = new BasicPartner.OfferBidIterator(dead);
        Assert.assertFalse(iter.hasNext());
    }

    private Iterator<OfferBid> deadOnly() {
        return Arrays.asList(deadBid(), deadBid(), deadBid(), deadBid()).iterator();
    }

    @Test
    public void doTest() {
        BasicPartner.OfferBidIterator iter = new BasicPartner.OfferBidIterator(source());
        int i = 0;
        while (iter.hasNext()) {
            OfferBid next = iter.next();
            Assert.assertNotNull(next);
            Assert.assertFalse(deleted(next));
            i++;
        }
        Assert.assertEquals(3, i);
    }


    private Iterator<OfferBid> source() {
        return Arrays.asList(deadBid(), notApplied(), notEmpty(), notAppliedAndNotEmpty()).iterator();
    }

    private boolean deleted(OfferBid next) {
        return next.applied() && next.empty();
    }


    private OfferBid deadBid() {
        OfferBid bid = Mockito.mock(OfferBid.class);
        Mockito.when(bid.empty()).thenReturn(true);
        Mockito.when(bid.applied()).thenReturn(true);
        return bid;
    }

    private OfferBid notEmpty() {
        OfferBid bid = Mockito.mock(OfferBid.class);
        Mockito.when(bid.empty()).thenReturn(false);
        Mockito.when(bid.applied()).thenReturn(true);
        return bid;
    }

    private OfferBid notApplied() {
        OfferBid bid = Mockito.mock(OfferBid.class);
        Mockito.when(bid.empty()).thenReturn(true);
        Mockito.when(bid.applied()).thenReturn(false);
        return bid;
    }

    private OfferBid notAppliedAndNotEmpty() {
        OfferBid bid = Mockito.mock(OfferBid.class);
        Mockito.when(bid.empty()).thenReturn(false);
        Mockito.when(bid.applied()).thenReturn(false);
        return bid;
    }
}
