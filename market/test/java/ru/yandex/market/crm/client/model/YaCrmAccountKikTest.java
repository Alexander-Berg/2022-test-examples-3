package ru.yandex.market.crm.client.model;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

/**
 * Тесты для {@link YaCrmAccountKik}.
 */
class YaCrmAccountKikTest {

    @ParameterizedTest(name = "\"{0}\" ~ \"{1}\" = {2}")
    @MethodSource("testEqualPhonesData")
    void testEqualPhones(final String phone1, final String phone2, final boolean expected) {
        assertEquals(expected, YaCrmAccountKik.equalPhones(phone1, phone2));
    }

    @ParameterizedTest(name = "\"{0}\" ~ \"{1}\" = {2}")
    @MethodSource("testEqualEmailsData")
    void testEqualEmails(final String email1, final String email2, final boolean expected) {
        assertEquals(expected, YaCrmAccountKik.equalEmails(email1, email2));
    }

    static Stream<Arguments> testEqualPhonesData() {
        return Stream.of(
                of("  +79134729401 ", " 89134729401  ", true),
                of("+79134729401", "89134729401", true),
                of("+79134729401", "9134729401", true),
                of("89134729401", "9134729401", true),
                of("8-913-472-94-01", "913 472 94 01", true),
                of("(913) 472-94-01", "913 472-94-01", true),
                of("+7 9134729401 ()", "9134729401", true),
                of("4729401", "9134729401", true),
                of("4729401", "472-94-01", true),
                of("472-9401", "472-94-01", true),
                of("472-9401", "472-94-03", false),
                of("472-9401", "", false),
                of("472-9401", null, false),
                of("", "", true),
                of(null, null, true)
        );
    }

    static Stream<Arguments> testEqualEmailsData() {
        return Stream.of(
                of("  test@yandex-team.ru ", " test@yandex-team.ru  ", true),
                of("test@yandex-team.ru", "test@yandex-team.ru", true),
                of("Test <test@yandex-team.ru>", "test@yandex-team.ru", true),
                of("Test1 <test@yandex-team.ru>", "Test2 <test@yandex-team.ru>", true),
                of("test1@yandex-team.ru", "test2@yandex-team.ru", false),
                of("", "", true),
                of(null, null, true)
        );
    }

}
