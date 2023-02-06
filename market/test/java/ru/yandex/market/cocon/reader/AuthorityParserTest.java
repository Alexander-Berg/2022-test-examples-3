package ru.yandex.market.cocon.reader;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.cocon.model.AuthorityKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class AuthorityParserTest {

    @ParameterizedTest
    @MethodSource("positiveArgs")
    void positive(String original, AuthorityKey expected) {
        assertEquals(expected, AuthorityParser.parse(original));
    }

    @ParameterizedTest
    @MethodSource("negativeArgs")
    void negative(String original) {
        assertThrows(IllegalArgumentException.class, () -> AuthorityParser.parse(original));
    }

    static Stream<Arguments> positiveArgs() {
        return Stream.of(
                Arguments.of("simple", new AuthorityKey("simple")),
                Arguments.of("noparam( )", new AuthorityKey("noparam")),
                Arguments.of("param(p)", new AuthorityKey("param", "p")),
                Arguments.of("param_space (p)", new AuthorityKey("param_space", "p")),
                Arguments.of("  param_spaces  (p)  ", new AuthorityKey("param_spaces", "p"))
        );
    }

    static Stream<Arguments> negativeArgs() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of(new Object[]{null}),
                Arguments.of(" "),
                Arguments.of("aaa("),
                Arguments.of("aaa(())"),
                Arguments.of("aaa(()"),
                Arguments.of("aaa)))"),
                Arguments.of("aaa)"),
                Arguments.of("aaa)("),
                Arguments.of("()"),
                Arguments.of("(")
        );
    }

}
