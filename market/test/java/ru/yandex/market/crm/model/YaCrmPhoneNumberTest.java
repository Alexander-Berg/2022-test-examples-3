package ru.yandex.market.crm.model;

import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

/**
 * Тесты для {@link YaCrmPhoneNumber}.
 */
class YaCrmPhoneNumberTest {

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\" & \"{2}\"")
    @MethodSource("testParseData")
    void testParse(final String origin, final String expectedPhone, final String expectedPhoneExt) {
        final YaCrmPhoneNumber actual = YaCrmPhoneNumber.parse(origin);

        assertEquals(StringUtils.trimToNull(origin), actual.getOriginPhoneNumber());
        assertEquals(expectedPhone, actual.getPhoneNumber());
        assertEquals(expectedPhoneExt, actual.getPhoneExt());
    }

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @MethodSource("testIsValidData")
    void testIsValid(final String phone, final boolean expected) {
        assertEquals(expected, YaCrmPhoneNumber.isValid(phone));
    }


    static Stream<Arguments> testParseData() {
        return Stream.of(
                of("  +79134729401  ()  ", "+79134729401", null),
                of("  +79134729401  (я не добавочный код)  ", "+79134729401", null),
                of("  +79134729401 (доб. 88)", "+79134729401", "88"),
                of("  +79134729401 (678)", "+79134729401", "678"),
                of("  +79134729401 ( 6 )", "+79134729401", "6"),
                of("  +7 (913) 472 94 01 ( 6)", "+7 (913) 472 94 01", "6"),
                of("  +7 (913) 472 94 01", "+7 (913) 472 94 01", null),
                of("+79134729401", "+79134729401", null),
                of("   ", null, null),
                of("", null, null),
                of(null, null, null)
        );
    }

    static Stream<Arguments> testIsValidData() {
        return Stream.of(
                of("+79134729401", true),
                of("+7 9134729401", true),
                of("+7 1678036208", false),
                of("+7 2605357398", false),
                of("   ", false),
                of("", false, null),
                of(null, false)
        );
    }

}
