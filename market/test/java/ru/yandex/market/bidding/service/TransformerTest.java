package ru.yandex.market.bidding.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.bidding.ExchangeProtos;
import ru.yandex.market.bidding.engine.OfferKeys;
import ru.yandex.market.bidding.model.Domain;
import ru.yandex.market.bidding.model.Place;
import ru.yandex.market.bidding.model.PublicationStatus;
import ru.yandex.market.bidding.model.RuleResult;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.bidding.ExchangeProtos.Bid.PublicationStatus.APPLIED;
import static ru.yandex.market.bidding.ExchangeProtos.Bid.PublicationStatus.NOT_ALLOWED;

public class TransformerTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testShop() throws Exception {
        ExchangeProtos.Bid.Builder bid =
                ExchangeProtos.Bid.newBuilder().setDomainType(ExchangeProtos.Bid.DomainType.SHOP_ID).
                        setPartnerId(123).setDomainId("123");

        ExchangeProtos.Bid.Value.Builder details =
                ExchangeProtos.Bid.Value.newBuilder().setValue(7).setModificationTime(12456);

        bid.setValueForSearch(details.setPublicationStatus(APPLIED).build());
        RuleResult expected = new RuleResult(
                123, Domain.SHOP_ID, "123", 0,
                Place.SEARCH, 7, PublicationStatus.APPLIED.getCode(),
                null, 11, 12456);
        checkEqual(expected, new Transformer(11).transform(bid.build(), Place.SEARCH));
    }

    @Test
    public void testCategory() throws Exception {
        ExchangeProtos.Bid.Builder bid =
                ExchangeProtos.Bid.newBuilder().setDomainType(ExchangeProtos.Bid.DomainType.CATEGORY_ID).
                        setPartnerId(123).setDomainId("9");

        ExchangeProtos.Bid.Value.Builder details =
                ExchangeProtos.Bid.Value.newBuilder().setValue(7).setModificationTime(12456);

        bid.setValueForMarketplace(details.setPublicationStatus(
                ExchangeProtos.Bid.PublicationStatus.NOT_FOUND).build());
        RuleResult expected = new RuleResult(
                123, Domain.CATEGORY_ID, "9", 0,
                Place.MARKET_PLACE, 7, -PublicationStatus.NOT_FOUND.getCode(),
                null, 11, 12456);
        checkEqual(expected, new Transformer(11).transform(bid.build(), Place.MARKET_PLACE));
    }

    @Test
    public void testTitle() throws Exception {
        ExchangeProtos.Bid.Builder bid =
                ExchangeProtos.Bid.newBuilder().setDomainType(ExchangeProtos.Bid.DomainType.OFFER_TITLE).
                        setPartnerId(123).setDomainId("345");

        ExchangeProtos.Bid.Value.Builder details =
                ExchangeProtos.Bid.Value.newBuilder().setValue(3).setModificationTime(12456);
        bid.setValueForMarketSearchOnly(details.setPublicationStatus(NOT_ALLOWED).build());

        RuleResult expected = new RuleResult(
                123, Domain.OFFER_TITLE, "345", 0,
                Place.MARKET_SEARCH, 3, -PublicationStatus.NOT_ALLOWED.getCode(),
                null, 11, 12456);
        checkEqual(expected, new Transformer(11).transform(bid.build(), Place.MARKET_SEARCH));

        bid.setValueForMarketSearchOnly(details.setPublicationStatus(APPLIED).build());
        bid.setSwitchOfferId("12876378");
        bid.setSwitchFeedId(784);
        expected = new RuleResult(
                123, Domain.OFFER_TITLE, "345", 784,
                Place.MARKET_SEARCH, 3, PublicationStatus.APPLIED.getCode(),
                "12876378", 11, 12456);
        checkEqual(expected, new Transformer(11).transform(bid.build(), Place.MARKET_SEARCH));
    }

    @Test
    public void testIdLegacy() throws Exception {
        String offerId = "345";
        ExchangeProtos.Bid.Builder bid =
                ExchangeProtos.Bid.newBuilder().setDomainType(ExchangeProtos.Bid.DomainType.FEED_OFFER_ID).
                        setPartnerId(123).setDomainId(offerId);

        ExchangeProtos.Bid.Value.Builder details =
                ExchangeProtos.Bid.Value.newBuilder().setValue(3).setModificationTime(12456);
        int feedId = 784;
        bid.setFeedId(feedId);

        bid.setValueForCard(details.setPublicationStatus(ExchangeProtos.Bid.PublicationStatus.NOT_ALLOWED).build());
        RuleResult expected = new RuleResult(
                123, Domain.FEED_OFFER_ID, offerId, feedId,
                Place.CARD, 3, -PublicationStatus.NOT_ALLOWED.getCode(),
                null, 11, 12456);
        checkEqual(expected, new Transformer(11).transform(bid.build(), Place.CARD));

        bid.setValueForCard(details.setPublicationStatus(APPLIED).build());
        bid.setSwitchOfferId("Sony XPeria Z2 Compact");
        expected = new RuleResult(
                123, Domain.FEED_OFFER_ID, offerId, feedId,
                Place.CARD, 3, PublicationStatus.APPLIED.getCode(),
                "Sony XPeria Z2 Compact", 11, 12456);
        checkEqual(expected, new Transformer(11).transform(bid.build(), Place.CARD));
    }

    @Test
    public void testId() throws Exception {
        String offerId = "345";
        ExchangeProtos.Bid.Builder bid =
                ExchangeProtos.Bid.newBuilder().setDomainType(ExchangeProtos.Bid.DomainType.FEED_OFFER_ID).
                        setPartnerId(123);

        ExchangeProtos.Bid.Value.Builder details =
                ExchangeProtos.Bid.Value.newBuilder().setValue(3).setModificationTime(12456);
        int feedId = 784;
        bid.addDomainIds(String.valueOf(feedId)).addDomainIds(offerId);

        bid.setValueForCard(details.setPublicationStatus(ExchangeProtos.Bid.PublicationStatus.NOT_ALLOWED).build());
        RuleResult expected = new RuleResult(
                123, Domain.FEED_OFFER_ID, offerId, feedId,
                Place.CARD, 3, -PublicationStatus.NOT_ALLOWED.getCode(),
                null, 11, 12456);
        checkEqual(expected, new Transformer(11).transform(bid.build(), Place.CARD));

        bid.setValueForCard(details.setPublicationStatus(APPLIED).build());
        bid.setSwitchOfferId("Sony XPeria Z2 Compact");
        expected = new RuleResult(
                123, Domain.FEED_OFFER_ID, offerId, feedId,
                Place.CARD, 3, PublicationStatus.APPLIED.getCode(),
                "Sony XPeria Z2 Compact", 11, 12456);
        checkEqual(expected, new Transformer(11).transform(bid.build(), Place.CARD));
    }

    @Test
    public void testModelId() throws Exception {
        String modelId = "345";
        ExchangeProtos.Bid.Builder bid =
                ExchangeProtos.Bid.newBuilder().setDomainType(ExchangeProtos.Bid.DomainType.MODEL_ID).
                        setPartnerId(123);

        ExchangeProtos.Bid.Value.Builder details =
                ExchangeProtos.Bid.Value.newBuilder().setValue(3).setModificationTime(12456);

        bid.addDomainIds(modelId);

        bid.setValueForModelSearch(details.setPublicationStatus(ExchangeProtos.Bid.PublicationStatus.NOT_ALLOWED).build());
        RuleResult expected = new RuleResult(
                123, Domain.MODEL_ID, modelId, OfferKeys.SYNTHETIC_FEED,
                Place.MODEL_SEARCH, 3, -PublicationStatus.NOT_ALLOWED.getCode(),
                null, 11, 12456);
        checkEqual(expected, new Transformer(11).transform(bid.build(), Place.MODEL_SEARCH));

        bid.setValueForModelSearch(details.setPublicationStatus(APPLIED).build());

        expected = new RuleResult(
                123, Domain.MODEL_ID, modelId, OfferKeys.SYNTHETIC_FEED,
                Place.MODEL_SEARCH, 3, PublicationStatus.APPLIED.getCode(),
                null, 11, 12456);
        checkEqual(expected, new Transformer(11).transform(bid.build(), Place.MODEL_SEARCH));
    }

    @Test
    public void testVendorCategoryId() throws Exception {
        String hyperId = "345";
        long vendorId = 123;
        ExchangeProtos.Bid.Builder bid =
                ExchangeProtos.Bid.newBuilder().setDomainType(ExchangeProtos.Bid.DomainType.VENDOR_CATEGORY_ID).
                        setPartnerId(vendorId);

        ExchangeProtos.Bid.Value.Builder details =
                ExchangeProtos.Bid.Value.newBuilder().setValue(3).setModificationTime(12456);

        bid.addDomainIds(String.valueOf(vendorId));
        bid.addDomainIds(hyperId);

        bid.setValueForModelSearch(details.setPublicationStatus(ExchangeProtos.Bid.PublicationStatus.NOT_ALLOWED).build());
        RuleResult expected = new RuleResult(
                vendorId, Domain.VENDOR_CATEGORY_ID, hyperId, OfferKeys.SYNTHETIC_FEED,
                Place.MODEL_SEARCH, 3, -PublicationStatus.NOT_ALLOWED.getCode(),
                null, 11, 12456);
        checkEqual(expected, new Transformer(11).transform(bid.build(), Place.MODEL_SEARCH));

        bid.setValueForModelSearch(details.setPublicationStatus(APPLIED).build());

        expected = new RuleResult(
                vendorId, Domain.VENDOR_CATEGORY_ID, hyperId, OfferKeys.SYNTHETIC_FEED,
                Place.MODEL_SEARCH, 3, PublicationStatus.APPLIED.getCode(),
                null, 11, 12456);
        checkEqual(expected, new Transformer(11).transform(bid.build(), Place.MODEL_SEARCH));
    }

    private void checkEqual(RuleResult expected, RuleResult actual) {
        assertEquals(expected.partnerId, actual.partnerId);
        assertEquals(expected.domainType, actual.domainType);
        assertEquals(expected.domainId, actual.domainId);
        assertEquals(expected.feedId, actual.feedId);
        assertEquals(expected.bidType, actual.bidType);
        assertEquals(expected.value, actual.value);
        assertEquals(expected.modifiedDate, actual.modifiedDate);
        assertEquals(expected.resultCode, actual.resultCode);
        assertEquals(expected.otherId, actual.otherId);
    }
}