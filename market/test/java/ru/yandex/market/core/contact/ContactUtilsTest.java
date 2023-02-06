package ru.yandex.market.core.contact;

import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.contact.utils.ContactUtils;

/**
 * Тесты для {@link ContactUtils}.
 */
class ContactUtilsTest {

    @ParameterizedTest(name = "\"{0}\" = \"{1}\" + \"{2}\" + \"{3}\"")
    @MethodSource("dataGetLastNameWithInitials")
    void testGetLastNameWithInitials(
            final String expected,
            final String last, final String first, final String middle
    ) {
        Assertions.assertThat(ContactUtils.getLastNameWithInitials(last, first, middle))
                .isEqualTo(expected);
    }

    static Stream<Arguments> dataGetLastNameWithInitials() {
        return Stream.of(
                Arguments.of("Иванов И.И.", "Иванов", "Иван", "Иванович"),
                Arguments.of("Иванов И.И.", "иванов", "иван", "иванович"),
                Arguments.of("Иванов И.", "Иванов", "Иван", ""),
                Arguments.of("Иванов И.", "Иванов", " ", "Иванович"),
                Arguments.of("Иванов", "Иванов", null, null),
                Arguments.of("", "", "", ""),
                Arguments.of("", null, null, null)
        );
    }

}
