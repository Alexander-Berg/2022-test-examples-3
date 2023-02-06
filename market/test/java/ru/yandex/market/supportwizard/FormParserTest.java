package ru.yandex.market.supportwizard;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.supportwizard.utils.ParsingUtils;

/**
 * Тесты для {@link ParsingUtils}
 */
public class FormParserTest {
    private static Stream<Arguments> idsArgs() {
        return Stream.of(
                Arguments.of("", List.of()),
                Arguments.of("  ", List.of()),
                Arguments.of("11", List.of(11L)),
                Arguments.of("11 21", List.of(11L, 21L)),
                Arguments.of("  11   21  ", List.of(11L, 21L)),
                Arguments.of("  11  21  ", List.of(11L, 21L)),
                Arguments.of("  11,  21  ", List.of(11L, 21L)),
                Arguments.of("123,", List.of(123L)),
                Arguments.of("11-123 11-321", List.of(123L, 321L)),
                Arguments.of("123,abc,321", List.of(123L, 321L))
        );
    }

    private static Stream<Arguments> namesArgs() {
        return Stream.of(
                Arguments.of("", List.of()),
                Arguments.of("  ", List.of()),
                Arguments.of("Name", List.of("Name")),
                Arguments.of("   Name      ", List.of("Name")),
                Arguments.of("  FirstName  StillFirstName  ", List.of("FirstName  StillFirstName")),
                Arguments.of("  FirstName , SecondName   ", List.of("FirstName", "SecondName")),
                Arguments.of(" #!$%>ltd. ", List.of("#!$%>ltd."))
        );
    }

    @ParameterizedTest
    @MethodSource("idsArgs")
    void testIds(String ids, List<String> parsedIds) {
        Assertions.assertEquals(parsedIds, ParsingUtils.parseIds(ids));
    }

    @ParameterizedTest
    @MethodSource("namesArgs")
    void testNames(String names, List<String> parsedNames) {
        Assertions.assertEquals(parsedNames, ParsingUtils.parseAgencies(names));
    }
}
