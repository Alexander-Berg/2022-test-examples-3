package ru.yandex.market.global.checkout.util;

import java.util.Locale;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class LocaleUtilsTest {

    @Test
    public void testGetLanguage() {
        //тест покрывает то что реализация локали в jdk переводит язык he в iw
        Assertions.assertThat(LocaleUtil.getLanguageFromLocale("he-IL")).isEqualTo("he");
        Assertions.assertThat(LocaleUtil.getLanguageFromLocale("he")).isEqualTo("he");
        Assertions.assertThat(LocaleUtil.getLanguageFromLocale("iw-IL")).isEqualTo("iw");
    }

    @Test
    public void testGetLocale() {
        Assertions.assertThat(LocaleUtil.parseLocale("en-CA")).isEqualTo(Locale.CANADA);
        Assertions.assertThat(LocaleUtil.parseLocale("en")).isEqualTo(Locale.ENGLISH);
    }

}
