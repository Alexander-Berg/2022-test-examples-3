package ru.yandex.market.crm.misc;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

/**
 * Тесты для {@link YaCrmPhoneCleaner}.
 */
class YaCrmPhoneCleanerTest {

    @ParameterizedTest(name = "\"{0}\"")
    @MethodSource("testCleanData")
    void testClean(final String origin, final String expected) {
        final String actual = YaCrmPhoneCleaner.clean(origin);
        assertEquals(expected, actual);
    }


    static Stream<Arguments> testCleanData() {
        return Stream.of(
                of("  +79134729401  ()  ", "+79134729401"),
                of("  +79134729401  ()  ", "+79134729401"),
                of("  +79134729401  (  )  ", "+79134729401"),
                of("  +79134729401 ", "+79134729401"),
                of("I'm not a phone number", null),
                of("тел. +913-472-9401", "+913-472-9401"),
                of("тел. ", null),
                of("  ", null),
                of("", null),
                of(null, null)
        );
    }

}
