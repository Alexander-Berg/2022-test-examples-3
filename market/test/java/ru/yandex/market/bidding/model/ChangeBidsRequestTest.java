package ru.yandex.market.bidding.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import ru.yandex.market.bidding.model.ChangeBidsRequest.Extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ChangeBidsRequestTest extends ModelSpringValidation {

    private static Map<Place, Integer> C_BID = BiddingRequestBuilder.builder().setCBid(10).build();
    private static Map<Place, Integer> SM_BID = BiddingRequestBuilder.builder().setBid(10).setMBid(10).build();
    private static Map<Place, Integer> SC_BID = BiddingRequestBuilder.builder().setBid(10).setCBid(10).build();

    @Mock
    private Extension extension;

    @Test
    void invalidBidValue() {
        ChangeBidsRequest.Bid bid = new ChangeBidsRequest.Bid(createBidValues(maxBid + 1, null, -1, null));
        Set<ConstraintViolation<ChangeBidsRequest.Bid>> constraintViolations = validator.validate(bid);
        assertEquals(1, constraintViolations.size());
        eqSet(Collections.singletonList(notInRangeMsg()), constraintViolations);

        bid = new ChangeBidsRequest.Bid(createBidValues(null, maxBid + 1, maxBid + 1, null));
        constraintViolations = validator.validate(bid);
        assertEquals(1, constraintViolations.size());
        eqSet(Collections.singletonList(notInRangeMsg()), constraintViolations);

        bid = new ChangeBidsRequest.Bid(createBidValues(-1, -1, null, null));
        constraintViolations = validator.validate(bid);
        assertEquals(1, constraintViolations.size());
        eqSet(Collections.singletonList(notInRangeMsg()), constraintViolations);

        bid = new ChangeBidsRequest.Bid(createBidValues(null, -1, null, -1));
        constraintViolations = validator.validate(bid);
        assertEquals(1, constraintViolations.size());
        eqSet(Collections.singletonList(notInRangeMsg()), constraintViolations);
    }

    @Test
    void titleNameIsNull() {
        ChangeBidsRequest.Title title = new ChangeBidsRequest.Title(SC_BID, null, extension);
        ChangeBidsRequest changeBidsRequest = ChangeBidsRequest.of(
                Collections.singletonList(title),
                null,
                false
        );

        Set<ConstraintViolation<ChangeBidsRequest>> constraintViolations = validator.validate(changeBidsRequest);
        assertEquals(1, constraintViolations.size());
        notNull(constraintViolations.iterator().next());
    }

    @Test
    void titleNameTooLong() {
        Set<ConstraintViolation<ChangeBidsRequest>> violations = prepareTitleTest(513);
        assertEquals(1, violations.size());
        badSize(violations.iterator().next(), 1, 512);
    }

    @Test
    void validLongTitleName() {
        assertTrue(prepareTitleTest(512).isEmpty());
    }

    private Set<ConstraintViolation<ChangeBidsRequest>> prepareTitleTest(int titleLength) {
        String prefixName = "TooLongName";
        String longTitle = prefixName + StringUtils.repeat('A', titleLength - prefixName.length());
        ChangeBidsRequest.Title title =
                new ChangeBidsRequest.Title(SC_BID, longTitle, mock(Extension.class));
        ChangeBidsRequest changeBidsRequest = ChangeBidsRequest.of(
                Collections.singletonList(title), null, false);
        return validator.validate(changeBidsRequest);
    }

    @Test
    void searchQueryTooLong() {
        Set<ConstraintViolation<ChangeBidsRequest.Title>> violations = prepareSearchQueryTest(2001);
        assertEquals(1, violations.size());
        badSize(violations.iterator().next(), 2, 2000);
    }

    @Test
    void searchQueryOneChar() {
        Set<ConstraintViolation<ChangeBidsRequest.Title>> violations = prepareSearchQueryTest(1);
        assertEquals(1, violations.size());
        badSize(violations.iterator().next(), 2, 2000);
    }

    @Test
    void emptySearchQuery() {
        Set<ConstraintViolation<ChangeBidsRequest.Title>> violations = prepareSearchQueryTest(0);
        assertEquals(1, violations.size());
        badSize(violations.iterator().next(), 2, 2000);
    }

    @Test
    void unsetSearchQuery() {
        assertTrue(prepareSearchQueryTest(null).isEmpty());
    }

    @Test
    void validLongSearchQuery() {
        assertTrue(prepareSearchQueryTest(2000).isEmpty());
    }

    private Set<ConstraintViolation<ChangeBidsRequest.Title>> prepareSearchQueryTest(Integer searchQueryLength) {
        String longQuery = searchQueryLength == null ? null : StringUtils.repeat('A', searchQueryLength);
        Extension extension = new Extension(1L, longQuery, null, null);
        Map<Place, Integer> bidValues = createBidValues(10, 10, null, null);
        ChangeBidsRequest.Title title = new ChangeBidsRequest.Title(bidValues, "OfferName", extension);
        return validator.validate(title);
    }

    @Test
    void titleExtensionIsNull() {
        ChangeBidsRequest.Title title = new ChangeBidsRequest.Title(SC_BID, "OfferName", null);
        Set<ConstraintViolation<ChangeBidsRequest.Title>> constraintViolations = validator.validate(title);
        assertEquals(1, constraintViolations.size());
        notNull(constraintViolations.iterator().next());
    }

    @Test
    void feedExtensionIsNull() {
        ChangeBidsRequest.FeedOfferId feed =
                new ChangeBidsRequest.FeedOfferId(C_BID, 1, "A1", null);
        ChangeBidsRequest changeBidsRequest = ChangeBidsRequest.of(null, Collections.singletonList(feed), true);
        Set<ConstraintViolation<ChangeBidsRequest>> constraintViolations = validator.validate(changeBidsRequest);
        assertEquals(1, constraintViolations.size());
        notNull(constraintViolations.iterator().next());
    }

    @Test
    void invalidFeedId() {
        ChangeBidsRequest.FeedOfferId feed =
                new ChangeBidsRequest.FeedOfferId(C_BID, 0, "A1", extension);
        Set<ConstraintViolation<ChangeBidsRequest.FeedOfferId>> constraintViolations = validator.validate(feed);
        assertEquals(1, constraintViolations.size());
        ge(1, constraintViolations.iterator().next());
    }

    @Test
    void offerIdIsBlank() {
        ChangeBidsRequest.FeedOfferId feed =
                new ChangeBidsRequest.FeedOfferId(C_BID, 1, "", extension);
        Set<ConstraintViolation<ChangeBidsRequest.FeedOfferId>> constraintViolations = validator.validate(feed);
        assertEquals(1, constraintViolations.size());
        assertEquals("size must be between 1 and 120", constraintViolations.iterator().next().getMessage());
    }

    @Test
    void offerHasWrongCharacters() {
        ChangeBidsRequest.FeedOfferId feed = new ChangeBidsRequest.FeedOfferId(
                C_BID,
                1,
                "A1-_/B2#",
                mock(Extension.class)
        );
        Set<ConstraintViolation<ChangeBidsRequest.FeedOfferId>> constraintViolations = validator.validate(feed);
        assertEquals(1, constraintViolations.size());
        assertEquals("must match [0-9a-zа-яёА-ЯЁA-Z.,\\/()\\[\\]\\-=_]*", constraintViolations.iterator().next().getMessage());
    }

    @Test
    void offerIdSupportUnderscoreAndHyphen() {
        ChangeBidsRequest.FeedOfferId feed =
                new ChangeBidsRequest.FeedOfferId(C_BID, 1, "A1_-.B2", extension);
        Set<ConstraintViolation<ChangeBidsRequest.FeedOfferId>> constraintViolations = validator.validate(feed);
        assertEquals(0, constraintViolations.size());
    }

    @Test
    void invalidCategoryId() {
        ChangeBidsRequest.Category bid = new ChangeBidsRequest.Category(SM_BID, -1, -1L);
        ChangeBidsRequest changeBidsRequest = ChangeBidsRequest.of(Collections.singletonList(bid));
        Set<ConstraintViolation<ChangeBidsRequest>> constraintViolations = validator.validate(changeBidsRequest);
        assertEquals(1, constraintViolations.size());
        ge(0, constraintViolations.iterator().next());
    }

    private Map<Place, Integer> createBidValues(Integer search, Integer card, Integer msearch, Integer fee) {
        return BiddingRequestBuilder.builder()
                .setBid(search)
                .setCBid(card)
                .setMBid(msearch)
                .setFee(fee)
                .build();
    }

    static class BiddingRequestBuilder {
        private final Map<Place, Integer> bids;

        BiddingRequestBuilder() {
            bids = new HashMap<>();
        }

        BiddingRequestBuilder(Integer valueIfNotSpecified) {
            this();
            bids.put(Place.SEARCH, valueIfNotSpecified);
            bids.put(Place.CARD, valueIfNotSpecified);
            bids.put(Place.MARKET_PLACE, valueIfNotSpecified);
            bids.put(Place.MARKET_SEARCH, valueIfNotSpecified);
        }

        static BiddingRequestBuilder builder(Integer valueIfNotSpecified) {
            return new BiddingRequestBuilder(valueIfNotSpecified);
        }

        static BiddingRequestBuilder builder() {
            return new BiddingRequestBuilder();
        }

        BiddingRequestBuilder setBid(Integer value) {
            bids.put(Place.SEARCH, value);
            return this;
        }

        BiddingRequestBuilder setCBid(Integer value) {
            bids.put(Place.CARD, value);
            return this;
        }

        BiddingRequestBuilder setMBid(Integer value) {
            bids.put(Place.MARKET_SEARCH, value);
            return this;
        }

        BiddingRequestBuilder setFee(Integer value) {
            bids.put(Place.MARKET_PLACE, value);
            return this;
        }

        Map<Place, Integer> build() {
            return bids;
        }
    }

}