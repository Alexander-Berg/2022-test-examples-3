package ru.yandex.market.logistics.lom.utils;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractTest;

@DisplayName("Unit тесты для PhoneUtils")
class PhoneUtilsTest extends AbstractTest {

    // Проверить телефон: https://libphonenumber.appspot.com/
    @ParameterizedTest
    @MethodSource({
        "validRussianAndKazakh",
        "invalidRussianAndKazakh",
        "validBelarus",
        "invalidBelarus",
        "validArmenia",
        "invalidArmenia",
        "validKyrgyzstan",
        "invalidKyrgyzstan",
        "justInvalid",
    })
    @DisplayName("Форматирование номера телефона")
    void normalizePhoneNumber(String phoneNumber, String expectedPhoneNumber) {
        String actualPhoneNumber = PhoneUtils.getFormattedPhoneNumber(phoneNumber);
        softly.assertThat(actualPhoneNumber).isEqualTo(expectedPhoneNumber);
    }

    @Nonnull
    private static Stream<Arguments> validRussianAndKazakh() {
        return Stream.of(
            Arguments.of("8005553535", "+78005553535"),
            Arguments.of("  800-555-35-35  ", "+78005553535"),
            Arguments.of("(800)-555-35-35", "+78005553535"),
            Arguments.of("8(800)-555-35-35", "+78005553535"),
            Arguments.of("8-999-555-35-35", "+79995553535"),
            Arguments.of("+7-800-555-35-35", "+78005553535"),
            Arguments.of("+7(800)-555-35-35", "+78005553535"),
            Arguments.of("-7800)(-555-35-35", "+78005553535"),
            Arguments.of("+7(700)-555-35-35", "+77005553535"),
            Arguments.of("+7(701)-555-35-35", "+77015553535"),
            Arguments.of("700-555-35-35", "+77005553535"),
            Arguments.of("701-555-35-35", "+77015553535"),
            Arguments.of("8700-555-35-35", "+77005553535"),
            Arguments.of("8701-555-35-35", "+77015553535"),
            Arguments.of("7700-555-35-35", "+77005553535")
        );
    }

    @Nonnull
    private static Stream<Arguments> invalidRussianAndKazakh() {
        return Stream.of(
            Arguments.of("+7-810-555-35-35", "+7-810-555-35-35"),
            Arguments.of("+7-600-555-35-35", "+7-600-555-35-35"),
            Arguments.of("810-555-35-35", "810-555-35-35"),
            Arguments.of("600-555-35-35", "600-555-35-35")
        );
    }

    @Nonnull
    private static Stream<Arguments> validBelarus() {
        return Stream.of(
            Arguments.of("+375-44-999-88-77", "+375449998877"),
            Arguments.of("+375 44 999 88 77", "+375449998877")
        );
    }

    @Nonnull
    private static Stream<Arguments> invalidBelarus() {
        return Stream.of(
            Arguments.of("+375-45-999-88-77", "+375-45-999-88-77"),
            Arguments.of("8 025 325 45 17", "8 025 325 45 17"),
            Arguments.of("375449998877", "375449998877")
        );
    }

    @Nonnull
    private static Stream<Arguments> validArmenia() {
        return Stream.of(
            Arguments.of("+374 238 119 21", "+37423811921"),
            Arguments.of("+37423811921", "+37423811921")
        );
    }

    @Nonnull
    private static Stream<Arguments> invalidArmenia() {
        return Stream.of(
            Arguments.of("+374 131 119 21", "+374 131 119 21"),
            Arguments.of("+374 238 119 211", "+374 238 119 211"),
            Arguments.of("37423811921", "37423811921"),
            Arguments.of("8 131 119 21", "8 131 119 21")
        );
    }

    @Nonnull
    private static Stream<Arguments> validKyrgyzstan() {
        return Stream.of(
            Arguments.of("+996 990 313 184", "+996990313184"),
            Arguments.of("+996 990 313 184", "+996990313184")
        );
    }

    @Nonnull
    private static Stream<Arguments> invalidKyrgyzstan() {
        return Stream.of(
            Arguments.of("996 990 313 184", "996 990 313 184"),
            Arguments.of("+996-115-313-184", "+996-115-313-184"),
            Arguments.of("+996 990 313 1844", "+996 990 313 1844"),
            Arguments.of("8 990 313 184", "8 990 313 184")
        );
    }

    @Nonnull
    private static Stream<Arguments> justInvalid() {
        return Stream.of(
            Arguments.of("", ""),
            Arguments.of(" ", " "),
            Arguments.of("1", "1"),
            Arguments.of("55-35-35", "55-35-35"),
            Arguments.of("123-55-35-35", "123-55-35-35")
        );
    }
}
