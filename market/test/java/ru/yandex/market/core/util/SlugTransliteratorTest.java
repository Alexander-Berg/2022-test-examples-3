package ru.yandex.market.core.util;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SlugTransliteratorTest {

    private static final int MAX_LENGTH = 1000;

    private static Stream<Arguments> getCommonCasesArguments() {
        return Stream.of(
                Arguments.of("model", "Модель"),
                Arguments.of("hal9000", "hal9000"),
                Arguments.of("processor-cores-count-two", "  processor  cores count — two  "),
                Arguments.of("elka", "ёлка ъ"),
                Arguments.of("hal9000", "hal9000"),
                Arguments.of("processor-cores-count-two", "  processor  cores count — two  "),
                Arguments.of("sedobnaia-palma", "съедобная пальма"),
                Arguments.of("utiug-s-parogeneratorom-barelli-bsm-2000", "Утюг с парогенератором BARELLİ BSM 2000"),
                Arguments.of("noutbuk-hp-650-h5k65ea-pentium-2020m-2400-mhz-15-6-1366x768-2048mb-320gb-dvd-rw-wi-fi-bluetooth-linux",
                        "Ноутбук HP 650 (H5K65EA) (Pentium 2020M 2400 Mhz/15.6\\\"/1366x768/2048Mb/320Gb/DVD-RW/Wi-Fi/Bluetooth/Linux)"),
                Arguments.of("salon-issaaaaaaaeceeeeiiiiidnooooooeuuuuythyaacloesss-if",
                        "Салон IßÀÁÂÃÄÅÆÇÈÉÊËÌİÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞŸĀĄČŁŒŚŞŠ¡IƑ"),
                Arguments.of("smes-nan-nestle-pre-fm-85-s-rozhdeniia-70-g", "Смесь NAN (Nestlé) Pre FM 85 (с рождения) 70 г"),
                Arguments.of("ksenon", "Ксенон")
        );
    }

    private static Stream<Arguments> getYandexCasesArguments() {
        return Stream.of(
                Arguments.of("yandex-stantsiia", "Яндекс станция"),
                Arguments.of("u-yandexa", "У Яндекса"),
                Arguments.of("komu-yandexu", "Кому? Яндексу"),
                Arguments.of("yandex-yandex", "Яндекс Яндекс")
        );
    }

    private static Stream<String> getNullCasesArguments() {
        return Stream.of("", null);
    }

    @ParameterizedTest
    @MethodSource({"getCommonCasesArguments", "getYandexCasesArguments"})
    void commonCasesTest(String expected, String original) {
        assertEquals(expected, SlugTransliterator.generateSlug(original));
    }

    @ParameterizedTest
    @MethodSource("getNullCasesArguments")
    void nullCasesTest(String original) {
        assertNull(SlugTransliterator.generateSlug(original));
    }

    @Test
    void tooLongSlugIsTrimmedTest() {
        final String pattern = "Very long string";

        StringBuilder testStr = new StringBuilder();
        for (int i = 0; i <= MAX_LENGTH / pattern.length(); i++) {
            testStr.append(pattern);
        }

        String result = SlugTransliterator.generateSlug(testStr.toString());
        assertNotNull(result);
        assertEquals(MAX_LENGTH, result.length());

    }

    @Test
    void trailingDashIsTrimmedTest() {
        final String pattern = "qwe+rty+z+";

        StringBuilder testStr = new StringBuilder();
        for (int i = 0; i <= MAX_LENGTH / pattern.length(); i++) {
            testStr.append(pattern);
        }

        String result = SlugTransliterator.generateSlug(testStr.toString());
        assertNotNull(result);
        assertFalse(result.endsWith("-"));
    }
}
