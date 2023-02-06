package ru.yandex.market.crm.model;

import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

/**
 * Тесты для {@link YaCrmEmailAddress}.
 */
class YaCrmEmailAddressTest {

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\" & \"{2}\"")
    @MethodSource("testParseData")
    void testParse(final String origin, final String expectedEmail, final boolean expectedValid) {
        final YaCrmEmailAddress actual = YaCrmEmailAddress.parse(origin);

        assertEquals(StringUtils.trimToNull(origin), actual.getOriginEmail());
        assertEquals(expectedEmail, actual.getEmail());
        assertEquals(expectedValid, actual.isValid());
    }

    static Stream<Arguments> testParseData() {
        return Stream.of(
                of("  test@yandex-team.ru ", "test@yandex-team.ru", true),
                of("test@yandex-team.ru", "test@yandex-team.ru", true),
                of("Test <test@yandex-team.ru>", "test@yandex-team.ru", true),
                of("I'm not an email", null, false),
                of("  ", null, false),
                of("", null, false),
                of(null, null, false)
        );
    }

}
