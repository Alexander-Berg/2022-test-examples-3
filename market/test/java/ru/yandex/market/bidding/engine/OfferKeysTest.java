package ru.yandex.market.bidding.engine;

import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.apache.commons.lang.RandomStringUtils;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OfferKeysTest {

    private static final Gt GT = new Gt();
    private static final Lt LT = new Lt();
    private static final boolean FAST_BID = true;
    private static final boolean SLOW_BID = false;
    private static final boolean VALID = true;
    private static final boolean NOT_VALID = false;

    static Stream<Arguments> testComparatorArgs() {
        return Stream.of(
                Arguments.of(
                        "1:123 > 0:124",
                        OfferKeys.of(1, "123"),
                        OfferKeys.of(0, "124"),
                        GT
                ),
                Arguments.of(
                        "0:123 < 0:124",
                        OfferKeys.of(0, "123"),
                        OfferKeys.of(0, "124"),
                        LT
                ),
                Arguments.of(
                        "0:123 < 0:ABC - numbers first",
                        OfferKeys.of(0, "123"),
                        OfferKeys.of(0, "ABC"),
                        LT
                ),
                Arguments.of("mutableKey: 0:123 < 0:124",
                        OfferKeys.mutableKey(0, "123"),
                        OfferKeys.mutableKey(0, "124"),
                        LT
                ),
                Arguments.of("mutableKey: 1:123 > 0:124",
                        OfferKeys.mutableKey(1, "123"),
                        OfferKeys.mutableKey(0, "124"),
                        GT
                ),
                Arguments.of("mutableKey: 0:123 < 0:ABC - numbers first",
                        OfferKeys.mutableKey(0, "123"),
                        OfferKeys.mutableKey(0, "ABC"),
                        LT
                )
        );
    }

    static Stream<Arguments> testValidationArgs() {
        return Stream.of(
                Arguments.of(SLOW_BID, VALID, "Плоскогубцы"),
                Arguments.of(SLOW_BID, VALID, "Новогодний костюм Супермен (детский)"),
                Arguments.of(SLOW_BID, NOT_VALID, 123),
                Arguments.of(SLOW_BID, NOT_VALID, DateTime.now()),
                Arguments.of(SLOW_BID, NOT_VALID, ""),
                Arguments.of(SLOW_BID, NOT_VALID, " "),

                Arguments.of(FAST_BID, VALID, "AB123DEF45xy678zw90N"),
                Arguments.of(FAST_BID, VALID, "AB123DEF45xy678zw-_."),
                Arguments.of(FAST_BID, VALID, 123),
                /**
                 * > {@link OfferKeys#MAX_OFFER_ID_LENGTH} length.
                 */
                Arguments.of(FAST_BID, VALID, "фыва"),
                Arguments.of(FAST_BID, VALID, "AB123DEF45xy678zw90NP"),
                Arguments.of(FAST_BID, NOT_VALID, "###"),
                Arguments.of(FAST_BID, NOT_VALID, RandomStringUtils.randomAlphanumeric(121)),
                Arguments.of(FAST_BID, NOT_VALID, "abcd\n"),
                Arguments.of(FAST_BID, NOT_VALID, "abcd\t"),
                Arguments.of(FAST_BID, NOT_VALID, DateTime.now()),
                Arguments.of(FAST_BID, VALID, "AB123Ю"),
                Arguments.of(FAST_BID, NOT_VALID, 345L),
                Arguments.of(FAST_BID, NOT_VALID, ""),
                Arguments.of(FAST_BID, NOT_VALID, " ")
        );
    }

    @DisplayName("testComparation")
    @ParameterizedTest(name = "{0}")
    @MethodSource("testComparatorArgs")
    void test_comparator(
            String desc,
            OfferKey firstKey,
            OfferKey secondKey,
            BiConsumer<OfferKey, OfferKey> cmp
    ) {
        cmp.accept(firstKey, secondKey);
    }

    @DisplayName("testValidation")
    @ParameterizedTest(name = "[{index}]")
    @MethodSource("testValidationArgs")
    void test_validation(
            boolean pace,
            Boolean expectedValidationResult,
            Comparable offerId
    ) {
        Assertions.assertEquals(expectedValidationResult, OfferKeys.isValidId(offerId, pace));
    }

    private static class Gt implements BiConsumer<OfferKey, OfferKey> {
        @Override
        public void accept(OfferKey firstKey, OfferKey secondKey) {
            Assertions.assertTrue(OfferKeys.CMP.compare(firstKey, secondKey) > 0);
        }
    }

    private static class Lt implements BiConsumer<OfferKey, OfferKey> {
        @Override
        public void accept(OfferKey firstKey, OfferKey secondKey) {
            Assertions.assertTrue(OfferKeys.CMP.compare(firstKey, secondKey) < 0);
        }
    }

}
