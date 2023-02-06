package ru.yandex.market.core.outlet;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.phone.PhoneType;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Victor Nazarov &lt;sviperll@yandex-team.ru&gt;
 */
public class PhoneNumberTest {

    @DisplayName("Успешный парсинг номера телефона без расширения.")
    @Test
    public void setBasedOnString_withoutExtension_correct() {
        PhoneNumber.Builder builder = PhoneNumber.builder();
        builder.setPhoneType(PhoneType.PHONE);
        builder.setBasedOnString("773(495)225-9123");
        PhoneNumber number = builder.build();
        assertPhoneNumber(number, true);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @DisplayName("Успешный парсинг номера телефона с расширением.")
    @Test
    public void setBasedOnString_withExtension_correct() {
        PhoneNumber.Builder builder = PhoneNumber.builder();
        builder.setPhoneType(PhoneType.PHONE);
        builder.setBasedOnString("+773(495)225-9123#1234\n");
        PhoneNumber number = builder.build();
        assertPhoneNumber(number, false);
        Assertions.assertThat(number.extension().get())
                .isEqualTo("1234");
    }

    private void assertPhoneNumber(@Nonnull PhoneNumber number, boolean isEmptyExtension) {
        Assertions.assertThat(number.getCountry())
                .isEqualTo("773");
        Assertions.assertThat(number.getCity())
                .isEqualTo("495");
        Assertions.assertThat(number.getNumber())
                .isEqualTo("2259123");
        Assertions.assertThat(number.extension().isEmpty())
                .isEqualTo(isEmptyExtension);
    }

    @DisplayName("Некорректный номер телефона. Парсинг завершился с ошибкой.")
    @Test
    public void setBasedOnString_wrongNumber_exception() {
        PhoneNumber.Builder builder = PhoneNumber.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.setBasedOnString("8 902 5135245, 8(800)3265000\n"));
    }
}
