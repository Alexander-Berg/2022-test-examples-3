package ru.yandex.market.bidding.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.bidding.ExchangeProtos;
import ru.yandex.market.bidding.TimeUtils;
import ru.yandex.market.bidding.model.Domain;
import ru.yandex.market.bidding.model.Place;
import ru.yandex.market.bidding.model.RuleResult;

import static org.junit.Assert.assertEquals;

public class ResultQueueTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testPoll() throws Exception {
        Transformer transformer = new Transformer(100) {
            @Override
            public RuleResult transform(ExchangeProtos.Bid bid, Place place) {
                ExchangeProtos.Bid.Value value = null;
                if (place == Place.SEARCH) {
                    value = bid.getValueForSearch();
                } else if (place == Place.CARD) {
                    value = bid.getValueForCard();
                } else if (place == Place.MARKET_PLACE) {
                    value = bid.getValueForMarketplace();
                } else if (place == Place.MARKET_SEARCH) {
                    value = bid.getValueForMarketSearchOnly();
                } else if (place == Place.MODEL_SEARCH) {
                    value = bid.getValueForModelSearch();
                } else {
                    throw new RuntimeException("place " + place + " is unkmown");
                }
                return createRule(bid.getDomainId(), place, value.getValue(), value.getModificationTime());
            }
        };

        List<ExchangeProtos.Parcel> parcels = new ArrayList<>();

        ExchangeProtos.Parcel.Builder parcel = ExchangeProtos.Parcel.newBuilder();

        List<RuleResult> expectedRules = new ArrayList<>();

        RuleResult nextRule = createRule("A", Place.SEARCH, 5, TimeUtils.nowUnixTime());
        expectedRules.add(nextRule);
        ExchangeProtos.Bid.Builder bid = ExchangeProtos.Bid.newBuilder();
        bid.setDomainId(nextRule.domainId);
        bid.setValueForSearch(
                ExchangeProtos.Bid.Value.newBuilder().
                        setValue(nextRule.value).setModificationTime(nextRule.modifiedDate).buildPartial());

        parcel.addBids(bid).buildPartial();

        nextRule = createRule("B", Place.SEARCH, 7, TimeUtils.nowUnixTime() + 1);
        expectedRules.add(nextRule);
        bid = ExchangeProtos.Bid.newBuilder();
        bid.setDomainId(nextRule.domainId);
        bid.setValueForSearch(ExchangeProtos.Bid.Value.newBuilder().
                setValue(nextRule.value).setModificationTime(nextRule.modifiedDate).buildPartial());

        nextRule = createRule("B", Place.CARD, 11, TimeUtils.nowUnixTime() + 2);
        expectedRules.add(nextRule);
        bid.setValueForCard(ExchangeProtos.Bid.Value.newBuilder().
                setValue(nextRule.value).setModificationTime(nextRule.modifiedDate).buildPartial());

        nextRule = createRule("B", Place.MARKET_PLACE, 17, TimeUtils.nowUnixTime() + 3);
        expectedRules.add(nextRule);
        bid.setValueForMarketplace(ExchangeProtos.Bid.Value.newBuilder().
                setModificationTime(nextRule.modifiedDate).setValue(nextRule.value).buildPartial());

        nextRule = createRule("B", Place.MARKET_SEARCH, 15, TimeUtils.nowUnixTime() + 4);
        expectedRules.add(nextRule);
        bid.setValueForMarketSearchOnly(ExchangeProtos.Bid.Value.newBuilder().
                setValue(nextRule.value).setModificationTime(nextRule.modifiedDate).buildPartial());

        parcel.addBids(bid).buildPartial();

        nextRule = createRule("C", Place.CARD, 3, TimeUtils.nowUnixTime() + 5);
        expectedRules.add(nextRule);
        bid = ExchangeProtos.Bid.newBuilder();
        bid.setDomainId(nextRule.domainId);
        bid.setValueForCard(ExchangeProtos.Bid.Value.newBuilder().
                setModificationTime(nextRule.modifiedDate).setValue(nextRule.value).buildPartial());

        parcel.addBids(bid).buildPartial();

        parcels.add(parcel.buildPartial());

        //search - 1 - 1
        //search - 9 - 9
        //card - 9 - 9
        parcel = ExchangeProtos.Parcel.newBuilder();

        nextRule = createRule("D", Place.CARD, 1, TimeUtils.nowUnixTime() + 6);
        expectedRules.add(nextRule);
        bid = ExchangeProtos.Bid.newBuilder();
        bid.setDomainId(nextRule.domainId);
        bid.setValueForCard(ExchangeProtos.Bid.Value.newBuilder().
                setValue(nextRule.value).setModificationTime(nextRule.modifiedDate).buildPartial());

        nextRule = createRule("D", Place.MARKET_SEARCH, 10, TimeUtils.nowUnixTime() + 7);
        expectedRules.add(nextRule);
        bid.setValueForMarketSearchOnly(ExchangeProtos.Bid.Value.newBuilder().
                setValue(nextRule.value).setModificationTime(nextRule.modifiedDate).buildPartial());

        parcel.addBids(bid).buildPartial();

        nextRule = createRule("F", Place.SEARCH, 9, TimeUtils.nowUnixTime() + 8);
        expectedRules.add(nextRule);
        bid = ExchangeProtos.Bid.newBuilder();
        bid.setDomainId(nextRule.domainId);
        bid.setValueForSearch(ExchangeProtos.Bid.Value.newBuilder().
                setValue(nextRule.value).setModificationTime(nextRule.modifiedDate).buildPartial());

        nextRule = createRule("F", Place.CARD, 8, TimeUtils.nowUnixTime() + 9);
        expectedRules.add(nextRule);
        bid.setValueForCard(ExchangeProtos.Bid.Value.newBuilder().
                setModificationTime(nextRule.modifiedDate).setValue(nextRule.value).buildPartial());

        parcel.addBids(bid).buildPartial();

        parcels.add(parcel.buildPartial());

        final Iterator<ExchangeProtos.Parcel> parcelIterator = parcels.iterator();
        ResultQueue queue = new ResultQueue(parcelIterator, transformer);

        List<RuleResult> actualRules = new ArrayList<>();

        while ((nextRule = queue.pull()) != null) {
            actualRules.add(nextRule);
        }

        assertEquals(expectedRules.size(), actualRules.size());

        ListIterator<RuleResult> ruleIterator = expectedRules.listIterator();
        for (RuleResult actualRule : actualRules) {
            compareRules(ruleIterator.next(), actualRule);
        }
    }

    private RuleResult createRule(String id, Place place, int value, int modifiedDate) {
        return new RuleResult(1, Domain.FEED_OFFER_ID, id, 1, place, value, 0, null, 10, modifiedDate);
    }

    private void compareRules(RuleResult expected, RuleResult actual) {
        assertEquals(expected.domainId, actual.domainId);
        assertEquals(expected.value, actual.value);
        assertEquals(expected.bidType, actual.bidType);
    }
}