package ru.yandex.market.mbo.gwt.utils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author commince
 */
public class GwtUrlUtilsTest {

    @Test
    public void justDomainPassesValidation() {
        assertTrue(GwtUrlUtils.isValidUrl("yandex.ru"));
    }

    @Test
    public void schemeAndWwwPassesValidation() {
        assertTrue(GwtUrlUtils.isValidUrl("http://www.yandex.ru/gg/ff/m.jpg"));
    }

    @Test
    public void wwwWOSchemePassesValidation() {
        assertTrue(GwtUrlUtils.isValidUrl("www.yandex.ru/gg/ff/m.jpg"));
    }

    @Test
    public void schemeWOWwwPassesValidation() {
        assertTrue(GwtUrlUtils.isValidUrl("http://yandex.ru/gg/ff/m.jpg"));
    }

    @Test
    public void urlWithPathQueryAndFragmentPassesValidation() {
        assertTrue(GwtUrlUtils.isValidUrl("http://yandex.ru/gg/ff?q=qwerty&b=asd#myFragment"));
    }

    @Test
    public void invalidSchemeFailsValidation() {
        assertFalse(GwtUrlUtils.isValidUrl("httpz://yandex.ru/gg/ff/m.jpg"));
    }

    @Test
    public void wwwWOSchemaWSlashPassesValidation() {
        assertTrue(GwtUrlUtils.isValidUrl("www.yandex.ru/gg/ff/m.jpg/"));
    }

    @Test
    public void russianDomainPassesValidation() {
        assertTrue(GwtUrlUtils.isValidUrl("яндекс.рф/gg/ff/m.jpg/"));
    }

    @Test
    public void invalidZeroDomainFailsValidation() {
        assertFalse(GwtUrlUtils.isValidUrl("https://yandex.rurururu/gg/ff/m.jpg"));
    }

    @Test
    public void hostnameWithSpacesFailsValidation() {
        assertFalse(GwtUrlUtils.isValidUrl("https://yan dex.ru/gg/ff/m.jpg"));
    }

    @Test
    public void russianPathPassesValidation() {
        assertTrue(GwtUrlUtils.isValidUrl("https://market.yandex.ru/привет/56379/"));
    }

    @Test
    public void russianQueryPassesValidation() {
        assertTrue(GwtUrlUtils.isValidUrl("https://market.yandex.ru/qwe/56379?hello=медвед"));
    }

    @Test
    public void normalizationUrlWithCyrillic() {
        assertTrue(GwtUrlUtils.isValidUrl("https://www.гелеос.рус"));
    }

    @Test
    public void normalizationUrlWithCyrillicWithPath() {
        assertTrue(GwtUrlUtils.isValidUrl("https://www.гелеос.рус/dir1/dir2"));
    }

    @Test
    public void normalizationUrlWithCyrillicWithQuery() {
        assertTrue(GwtUrlUtils.isValidUrl("https://www.гелеос.рус?name=value"));
    }

    @Test
    public void normalizationUrlWithCyrillicWithPathAndQuery() {
        assertTrue(GwtUrlUtils.isValidUrl("https://www.гелеос.рус/dir1/dir2?name=value"));
    }

    @Test
    public void emptySchemeValidation() {
        assertTrue(GwtUrlUtils.isValidUrl("//market.yandex.ru/qwe/56379?hello=медвед"));
    }
}
