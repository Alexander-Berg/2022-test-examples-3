package ru.yandex.market.core.param.model.validators;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.core.auction.model.AuctionType;

/**
 * Test for {@link HasCodeValueValidator}.
 *
 * @author Ilya Reznikov richard@yandex-team.ru
 */
class HasCodeValueValidatorTest {

    private static ParamValueValidator validator;

    @BeforeAll
    static void setUp() {
        validator = new HasCodeValueValidator<>(AuctionType.class);
    }

    @ParameterizedTest
    @EnumSource(AuctionType.class)
    void testValidValue(AuctionType auctionType) {
        Assertions.assertTrue(validator.isValid(auctionType.getId()));
    }

    @Test
    void testInvalidValue() {
        Assertions.assertFalse(validator.isValid("some_unknown_auction_type"));
    }
}
