package ru.yandex.market.api.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SlugTest {
    @Test
    public void testTransliteration() {
        assertEquals("model", Slug.generateSlug("Модель"));
        assertEquals("elka", Slug.generateSlug("ёлка ъ"));
        assertEquals("hal9000", Slug.generateSlug("hal9000"));
        assertEquals("processor-cores-count-two", Slug.generateSlug("  processor  cores count — two  "));
        assertEquals("sedobnaia-palma", Slug.generateSlug("съедобная пальма"));
        assertEquals("utiug-s-parogeneratorom-barelli-bsm-2000", Slug.generateSlug("Утюг с парогенератором BARELLİ BSM 2000"));
        assertEquals("noutbuk-hp-650-h5k65ea-pentium-2020m-2400-mhz-15-6-1366x768-2048mb-320gb-dvd-rw-wi-fi-bluetooth-linux",
                Slug.generateSlug("Ноутбук HP 650 (H5K65EA) (Pentium 2020M 2400 Mhz/15.6\\\"/1366x768/2048Mb/320Gb/DVD-RW/Wi-Fi/Bluetooth/Linux)"));
        assertEquals("salon-issaaaaaaaeceeeeiiiiidnooooooeuuuuythyaacloesss-if", Slug.generateSlug("Салон IßÀÁÂÃÄÅÆÇÈÉÊËÌİÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞŸĀĄČŁŒŚŞŠ¡IƑ"));
        assertEquals("smes-nan-nestle-pre-fm-85-s-rozhdeniia-70-g", Slug.generateSlug("Смесь NAN (Nestlé) Pre FM 85 (с рождения) 70 г"));
        assertEquals("ksenon", Slug.generateSlug("Ксенон"));
        assertNull(Slug.generateSlug(null));
        assertNull(Slug.generateSlug(""));
    }

    @Test
    public void YandexTransliterationTest() {
        assertEquals("yandex-stantsiia", Slug.generateSlug("Яндекс станция"));
        assertEquals("u-yandexa", Slug.generateSlug("У Яндекса"));
        assertEquals("komu-yandexu", Slug.generateSlug("Кому? Яндексу"));
        assertEquals("yandex-yandex", Slug.generateSlug("Яндекс Яндекс"));
    }

    @Test
    public void slugLengthTest() {
        // Длина slug не должна превышать 1000 символов
        StringBuilder testStr1 = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            testStr1.append("Very long string");
        }
        assertEquals(1000, Slug.generateSlug(testStr1.toString()).length());

        // "-" на конце должен обрезаться
        StringBuilder testStr2 = new StringBuilder();
        for (int i = 0; i < 110; i++) {
            testStr2.append("qwe+rty+z+");
        }
        assertEquals(999, Slug.generateSlug(testStr2.toString()).length());
    }
}
