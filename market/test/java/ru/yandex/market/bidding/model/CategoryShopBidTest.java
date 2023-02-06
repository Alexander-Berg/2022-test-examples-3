package ru.yandex.market.bidding.model;

import java.util.EnumMap;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

class CategoryShopBidTest extends ModelValidation {

    @Test
    void invalidCategoryId() {
        CategoryShopBid bid = new CategoryShopBid(1, -1, mock(EnumMap.class));
        Set<ConstraintViolation<CategoryShopBid>> constraintViolations = validator.validate(bid);
        assertEquals(1, constraintViolations.size());
        ge(0, constraintViolations.iterator().next());
    }
}