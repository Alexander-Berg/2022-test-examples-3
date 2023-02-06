package ru.yandex.market.core.util;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit-тесты на {@link CaseUtils}.
 *
 * @author fbokovikov
 */
public class CaseUtilsTest {

    static Stream<Arguments> camelCaseArgs() {
        return Stream.of(
                Arguments.of("fromDate", true),
                Arguments.of("from_date", false),
                Arguments.of("from-date", false),
                Arguments.of("fromLastDate", true),
                Arguments.of("token", false),
                Arguments.of("veryLongExpressionForTest", true),
                Arguments.of("mixedArgs-with-kebab", false),
                Arguments.of("mixedArgs_with_snake", false)
        );
    }

    static Stream<Arguments> kebabCaseArgs() {
        return Stream.of(
                Arguments.of("from-date", true),
                Arguments.of("from_date", false),
                Arguments.of("fromDate", false),
                Arguments.of("from-last-date", true),
                Arguments.of("from-last-Date", false),
                Arguments.of("token", false),
                Arguments.of("very-long-expression-for-test", true),
                Arguments.of("mixed-args-withCamel", false),
                Arguments.of("mixed-args-with_snake", false)
        );
    }

    static Stream<Arguments> snakeCaseArgs() {
        return Stream.of(
                Arguments.of("from_date", true),
                Arguments.of("from-date", false),
                Arguments.of("fromDate", false),
                Arguments.of("from_last_date", true),
                Arguments.of("from_last_Date", false),
                Arguments.of("token", false),
                Arguments.of("very_long_expression_for_test", true),
                Arguments.of("mixed_args_withCamel", false),
                Arguments.of("mixed_args_with-kebab", false)
        );
    }

    @ParameterizedTest
    @MethodSource("camelCaseArgs")
    void isCamelCase(String word, boolean expected) {
        boolean actual = CaseUtils.isCamelCase(word);
        Assertions.assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("kebabCaseArgs")
    void isKebabCase(String word, boolean expected) {
        boolean actual = CaseUtils.isKebabCase(word);
        Assertions.assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("snakeCaseArgs")
    void isSnakeCase(String word, boolean expected) {
        boolean actual = CaseUtils.isSnakeCase(word);
        Assertions.assertEquals(expected, actual);
    }
}
