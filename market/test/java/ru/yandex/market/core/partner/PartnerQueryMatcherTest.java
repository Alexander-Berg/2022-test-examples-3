package ru.yandex.market.core.partner;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Юнит тест для {@link PartnerQueryMatcher}.
 */
public class PartnerQueryMatcherTest {

    static Stream<Arguments> filterArgs() {
        return Stream.of(
                Arguments.of(null, true),
                Arguments.of("", true),
                Arguments.of("    ", true),
                Arguments.of("11-", true),
                Arguments.of("Ш", true),
                Arguments.of("Шко", true),
                Arguments.of("Не", false),
                Arguments.of("142", true),
                Arguments.of("1426", true),
                Arguments.of("14259", false),
                Arguments.of("1", true),
                Arguments.of("11-156", true),
                Arguments.of("11-156241426241", true),
                Arguments.of("156241426241-11", false),
                Arguments.of("ШКОЛА СЕМИ ГНОМОВ", true),
                Arguments.of("шКоЛа СеМи", true)
        );
    }

    @ParameterizedTest
    @DisplayName("Проверить query matching")
    @MethodSource("filterArgs")
    void filter(String query, boolean expectedMatch) {
        PartnerQueryMatcher partnerQuery = PartnerQuery.builder().withQuery(query).build().matcher();
        Assertions.assertEquals(
                expectedMatch,
                partnerQuery.matches(156241426241L, "Школа семи гномов")
        );
    }
}
