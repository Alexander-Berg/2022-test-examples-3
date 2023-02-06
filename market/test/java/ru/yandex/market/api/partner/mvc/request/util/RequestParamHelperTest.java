package ru.yandex.market.api.partner.mvc.request.util;

import java.util.Set;
import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Юнит-тесты на {@link RequestParamHelper}.
 *
 * @author fbokovikov
 */
public class RequestParamHelperTest {

    static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of("strangeName-for_test", new String[]{"strangeName-for_test"}),
                Arguments.of("fromDate", new String[]{"fromDate", "from_date"}),
                Arguments.of("from_date", new String[]{"from_date", "fromDate"}),
                Arguments.of("token", new String[]{"token"})
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("args")
    void resolvePossibleNames(String paramNames, String... perturbationNames) {
        Set<String> possibleNames = RequestParamHelper.resolvePossibleNames(paramNames);
        MatcherAssert.assertThat(
                possibleNames,
                Matchers.containsInAnyOrder(perturbationNames)
        );
    }
}
