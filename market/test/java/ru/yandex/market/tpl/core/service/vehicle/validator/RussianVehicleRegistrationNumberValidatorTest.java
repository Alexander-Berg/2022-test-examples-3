package ru.yandex.market.tpl.core.service.vehicle.validator;

import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RequiredArgsConstructor
public class RussianVehicleRegistrationNumberValidatorTest extends TplAbstractTest {
    private final RussianVehicleRegistrationNumberValidator validator;

    @ParameterizedTest
    @ValueSource(strings = {
            "X123EH",
            "x123EH",
            "X123eH",
            "X123Eh",
    })
    void happyPathNumberTest(String validNumber) {
        var validRegion = "77";

        assertDoesNotThrow(() -> validator.validate(validNumber, validRegion));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "77",
            "077",
            "777",
    })
    void happyPathRegionTest(String validRegion) {
        var validNumber = "X123EH";

        assertDoesNotThrow(() -> validator.validate(validNumber, validRegion));
    }

    @ParameterizedTest
    @MethodSource("blankStrings")
    void invalidNumberIsNullOrEmptyTest(String invalidNumber) {
        var validRegion = "77";
        var exception =
                assertThrows(TplIllegalArgumentException.class, () -> validator.validate(invalidNumber, validRegion));

        assertThat(exception.getMessage()).contains("Регистрационный номер ТС не может быть пустым");
    }

    @ParameterizedTest
    @MethodSource("blankStrings")
    void invalidRegionIsNullOrEmptyTest(String invalidRegion) {
        var validNumber = "X123EH";
        var exception =
                assertThrows(TplIllegalArgumentException.class, () -> validator.validate(validNumber, invalidRegion));

        assertThat(exception.getMessage()).contains("Регистрационный номер ТС не может быть пустым");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Q123EE",
            "П123EE",
            "8123EE",
            "+123EE"
    })
    void invalidNumberFirstLetterTest(String invalidNumber) {
        var validRegion = "77";
        var exception =
                assertThrows(TplIllegalArgumentException.class, () -> validator.validate(invalidNumber, validRegion));

        assertThat(exception.getMessage()).contains("Неверный символ", "в госномере", "Список доступных значений");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "A123E1",
            "A1231E",
            "A123QE",
            "A123EW",
            "A123EЙ",
            "A123ЙЕ"
    })
    void invalidNumberLastLetterTest(String invalidNumber) {
        var validRegion = "77";
        var exception =
                assertThrows(TplIllegalArgumentException.class, () -> validator.validate(invalidNumber, validRegion));

        assertThat(exception.getMessage()).contains("Неверный символ", "в госномере", "Список доступных значений");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Aq23EE",
            "A1q3EE",
            "A12qEE",
            "A1*3EE",
    })
    void invalidNumberTest(String invalidNumber) {
        var validRegion = "77";
        var exception =
                assertThrows(TplIllegalArgumentException.class, () -> validator.validate(invalidNumber, validRegion));

        assertThat(exception.getMessage()).contains("Неверный символ", "в госномере", "Список доступных значений");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "A31EE",
            "A22231EE",
            "A232222222221EE",
    })
    void invalidNumberLengthTest(String invalidNumber) {
        var validRegion = "77";
        var exception =
                assertThrows(TplIllegalArgumentException.class, () -> validator.validate(invalidNumber, validRegion));

        assertThat(exception.getMessage()).contains("должен состоять из 6 символов");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1",
            "1234",
            "1234"
    })
    void invalidRegionSizeTest(String invalidRegion) {
        var validNumber = "X123EH";
        var exception =
                assertThrows(TplIllegalArgumentException.class, () -> validator.validate(validNumber, invalidRegion));

        assertThat(exception.getMessage()).contains("Количество символов в регионе");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "7A",
            "A7",
            "77A",
            "7A7",
            "A77",
    })
    void invalidRegionWithLettersTest(String invalidRegion) {
        var validNumber = "X123EH";
        var exception =
                assertThrows(TplIllegalArgumentException.class, () -> validator.validate(validNumber, invalidRegion));

        assertThat(exception.getMessage()).contains("Неверный символ", "в номере региона", "Список доступных значений");
    }

    private static Stream<String> blankStrings() {
        return Stream.of("", "   ", null);
    }
}
