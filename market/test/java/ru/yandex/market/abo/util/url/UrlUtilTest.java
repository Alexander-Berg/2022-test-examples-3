package ru.yandex.market.abo.util.url;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author artemmz
 * @date 19/12/2019.
 */
class UrlUtilTest {
    @ParameterizedTest
    @CsvSource({
            "http://test.ru, http://test.ru",
            "https://архитекторы.рф/courses/management, https://xn--80akijuiemcz7e.xn--p1ai/courses/management",
            "http://yandex/something/something/something.ru, http://yandex/something/something/something.ru",
            "https://кроха-ха.рф/katalog-tovarov, https://xn----7sbb1bul0bc.xn--p1ai/katalog-tovarov"
    })
    void convertUrl(String validUrl, String url) {
        assertEquals(validUrl, UrlUtil.convertASCIIUrlToUnicode(url));
    }

    @Test
    void validateUrls() {
        assertFalse(UrlUtil.isURLFromDomain(null, "ya.ru"));
        assertFalse(UrlUtil.isURLFromDomain("", null));
        assertFalse(UrlUtil.isURLFromDomain("", "ya.ru"));
        assertFalse(UrlUtil.isURLFromDomain("http://ya.ru", ""));
        assertTrue(UrlUtil.isURLFromDomain("http://ya.ru", "ya.ru"));
        assertFalse(UrlUtil.isURLFromDomain("https://test.ru", "тест.рф"));
        assertTrue(UrlUtil.isURLFromDomain("https://кроха-ха.рф/katalog-tovarov", "кроха-ха.рф"));
        assertTrue(UrlUtil.isURLFromDomain("https://фильтр.su/shopping_cart/checkout/68806145?type=desktop", "xn--h1agphh4c.su"));
    }

}
