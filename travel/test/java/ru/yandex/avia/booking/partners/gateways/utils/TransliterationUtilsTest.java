package ru.yandex.avia.booking.partners.gateways.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.avia.booking.partners.gateways.utils.TransliterationUtils.transliterateToLatinSafeIcao9303;

public class TransliterationUtilsTest {
    @Test
    void testTransliterateToLatinSafeIcao9303() {
        // shouldn't be modified
        assertThat(transliterateToLatinSafeIcao9303("123 ASD \t\n %*&^$&@"))
                .isEqualTo("123 ASD \t\n %*&^$&@");

        assertThat(transliterateToLatinSafeIcao9303("IVX Я ТЕКСТ НА РУССКОМ END"))
                .isEqualTo("IVX IA TEKST NA RUSSKOM END");
        assertThat(transliterateToLatinSafeIcao9303("[АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЫЬЪЭЮЯ]"))
                .isEqualTo("[ABVGDEEZHZIIKLMNOPRSTUFKHTSCHSHSHCHYIEEIUIA]");
        assertThat(transliterateToLatinSafeIcao9303("[абвгдеёжзийклмнопрстуфхцчшщыьъэюя]"))
                .isEqualTo("[abvgdeezhziiklmnoprstufkhtschshshchyieeiuia]");
    }

    @Test
    void testTransliterateToLatinSafeIcao9303Failure() {
        assertThatThrownBy(() -> transliterateToLatinSafeIcao9303("ä, é, ò"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not mapped non-latin characters detected");
    }
}
