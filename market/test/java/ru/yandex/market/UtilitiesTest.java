package ru.yandex.market;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.params.provider.Arguments.of;

/**
 * Тесты для {@link Utilities}
 */
class UtilitiesTest {

    private static Stream<Arguments> validStrings() {
        return Stream.of(
                of(Long.MIN_VALUE, String.valueOf(Long.MIN_VALUE)),
                of(-123L, "-123"),
                of(1L, "1"),
                of(0L, "0")
        );
    }

    private static Stream<Arguments> invalidStrings() {
        return Stream.of(
                Arguments.of((String) null),
                of(""),
                of("  "),
                of("-0"),
                of(" +123"),
                of(" 123"),
                of(" 0123"),
                of("+123"),
                of("+0123"),
                of("+"),
                of("+a123"),
                of("+123a"),
                of(" -0123"),
                of(" 123"),
                of(" 0123"),
                of("-0123"),
                of("-"),
                of("-a123"),
                of("-123a")
        );
    }

    private static Stream<Arguments> threadNameArguments() {
        return Stream.of(
                Arguments.of(
                        "2-Update-Status-Worker2-[gid=622236]",
                        "2-Update-Status-Worker2-[gid=1234]"
                ),
                Arguments.of(
                        "2-Update-Status-Worker2",
                        "2-Update-Status-Worker2-[gid=1234]"
                ),
                Arguments.of(
                        "2-Update-Status-Worker2-[gid=1234]",
                        "2-Update-Status-Worker2-[gid=1234]"
                )
        );
    }

    @DisplayName("Parse broken values")
    @ParameterizedTest(name = "parse \"{0}\"")
    @MethodSource("invalidStrings")
    void test_parseBroken(String stringTobeParsed) {
        Assertions.assertThrows(
                NumberFormatException.class,
                () -> Utilities.safeParseLong(stringTobeParsed)
        );
    }

    @DisplayName("Parse correct values")
    @ParameterizedTest(name = "parse \"{1}\"")
    @MethodSource("validStrings")
    void test_parseValid(long expectedValue, String stringTobeParsed) {
        assertEquals(expectedValue, Utilities.safeParseLong(stringTobeParsed));
    }

    @DisplayName("Проверка, что название треда меняется корректно")
    @MethodSource("threadNameArguments")
    @ParameterizedTest
    void testCheckThreadName(String oldThreadName, String newThreadName) {
        final long newGid = 1234L;
        final Thread thread = new Thread(oldThreadName);
        Utilities.addGidToThreadName(thread, newGid);
        assertThat(thread.getName(), is(newThreadName));
    }
}
