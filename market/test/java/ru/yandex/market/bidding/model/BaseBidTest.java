package ru.yandex.market.bidding.model;

import java.util.EnumMap;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.junit.jupiter.api.Test;

import ru.yandex.market.bidding.TimeUtils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

class BaseBidTest extends ModelSpringValidation {

    @Test
    void invalidShopId() {
        BaseBid bid = new BaseShopBid(0, mock(EnumMap.class));
        Set<ConstraintViolation<BaseBid>> constraintViolations = validator.validate(bid);
        assertEquals(1, constraintViolations.size());
        ge(1, constraintViolations.iterator().next());
    }

    @Test
    void invalidDetailsValue() {
        BaseBid bid = of(Place.SEARCH, new BaseBid.Details((short) -1, TimeUtils.nowUnixTime(), PublicationStatus.APPLIED));
        Set<ConstraintViolation<BaseBid>> constraintViolations = validator.validate(bid);
        assertEquals(1, constraintViolations.size());
        notInRange(constraintViolations.iterator().next());

        bid = of(Place.CARD, new BaseBid.Details((short) (maxBid + 1), TimeUtils.nowUnixTime(), PublicationStatus.APPLIED));
        constraintViolations = validator.validate(bid);
        assertEquals(1, constraintViolations.size());
        notInRange(constraintViolations.iterator().next());
    }

    @Test
    void invalidDetailsAt() {
        BaseBid.Details details = new BaseBid.Details((short) 10, -1, PublicationStatus.APPLIED);
        Set<ConstraintViolation<BaseBid.Details>> constraintViolations = validator.validate(details);
        assertEquals(1, constraintViolations.size());
        ge(0, constraintViolations.iterator().next());
    }

    @Test
    void requestIsNull() {
        BaseBid.Details details = new BaseBid.Details((short) 10, TimeUtils.nowUnixTime(), null);
        Set<ConstraintViolation<BaseBid.Details>> constraintViolations = validator.validate(details);
        assertEquals(1, constraintViolations.size());
        notNull(constraintViolations.iterator().next());
    }

    @Test
    void statusIsNull() {
        BaseBid.Details request = new BaseBid.Details((short) 10, TimeUtils.nowUnixTime(), null);
        Set<ConstraintViolation<BaseBid.Details>> constraintViolations = validator.validate(request);
        assertEquals(1, constraintViolations.size());
        notNull(constraintViolations.iterator().next());
    }

    private BaseBid of(Place place, BaseBid.Details details) {
        final EnumMap<Place, BaseBid.Details> pp = new EnumMap<>(Place.class);
        pp.put(place, details);
        return new BaseShopBid(1, pp);
    }
}