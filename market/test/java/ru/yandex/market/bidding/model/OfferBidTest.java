package ru.yandex.market.bidding.model;

import java.util.EnumMap;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

class OfferBidTest extends ModelValidation {
    private static final AuctionBidComponentsLink LINK_TYPE_IRRELEVANT = AuctionBidComponentsLink.CARD_LINK_CBID_VARIABLE;

    @Test
    void invalidFeedId() {
        OfferBid bid = new OfferBid(
                1,
                0L,
                "ABC",
                mock(EnumMap.class),
                0L,
                "ABC",
                mock(GoalPlace.class),
                LINK_TYPE_IRRELEVANT
        );
        Set<ConstraintViolation<OfferBid>> constraintViolations = validator.validate(bid);
        assertEquals(1, constraintViolations.size());
        ge(1, constraintViolations.iterator().next());
    }

    @Test
    void invalidGroupId() {
        OfferBid bid = new OfferBid(
                1,
                1L,
                "ABC",
                mock(EnumMap.class),
                -1L,
                "ABC",
                mock(GoalPlace.class),
                LINK_TYPE_IRRELEVANT
        );
        Set<ConstraintViolation<OfferBid>> constraintViolations = validator.validate(bid);
        assertEquals(1, constraintViolations.size());
        ge(0, constraintViolations.iterator().next());
    }

    @Test
    void nameIsNull() {
        OfferBid bid = new OfferBid(
                1,
                1L,
                null,
                mock(EnumMap.class),
                0L,
                "ABC",
                mock(GoalPlace.class),
                LINK_TYPE_IRRELEVANT
        );
        Set<ConstraintViolation<OfferBid>> constraintViolations = validator.validate(bid);
        assertEquals(1, constraintViolations.size());
        notNull(constraintViolations.iterator().next());
    }
}