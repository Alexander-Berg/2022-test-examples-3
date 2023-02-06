package ru.yandex.market.mbo.utils;

import org.apache.commons.validator.ValidatorException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author commince
 */
public class UrlUtilsTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void justDomainPassesValidation() {
        assertTrue(UrlUtils.isValidUrl("yandex.ru"));
    }

    @Test
    public void schemeAndWwwPassesValidation() {
        assertTrue(UrlUtils.isValidUrl("http://www.yandex.ru/gg/ff/m.jpg"));
    }

    @Test
    public void wwwWOSchemePassesValidation() {
        assertTrue(UrlUtils.isValidUrl("www.yandex.ru/gg/ff/m.jpg"));
    }

    @Test
    public void schemeWOWwwPassesValidation() {
        assertTrue(UrlUtils.isValidUrl("http://yandex.ru/gg/ff/m.jpg"));
    }

    @Test
    public void urlWithPathQueryAndFragmentPassesValidation() {
        assertTrue(UrlUtils.isValidUrl("http://yandex.ru/gg/ff?q=qwerty&b=asd#myFragment"));
    }

    @Test
    public void invalidSchemeFailsValidation() {
        assertFalse(UrlUtils.isValidUrl("httpz://yandex.ru/gg/ff/m.jpg"));
    }

    @Test
    public void wwwWOSchemaWSlashPassesValidation() {
        assertTrue(UrlUtils.isValidUrl("www.yandex.ru/gg/ff/m.jpg/"));
    }

    @Test
    public void russianDomainPassesValidation() {
        assertTrue(UrlUtils.isValidUrl("яндекс.рф/gg/ff/m.jpg/"));
    }

    @Test
    public void invalidZeroDomainFailsValidation() {
        assertFalse(UrlUtils.isValidUrl("https://yandex.rurururu/gg/ff/m.jpg"));
    }

    @Test
    public void hostnameWithSpacesFailsValidation() {
        assertFalse(UrlUtils.isValidUrl("https://yan dex.ru/gg/ff/m.jpg"));
    }

    @Test
    public void russianPathPassesValidation() {
        assertTrue(UrlUtils.isValidUrl("https://market.yandex.ru/привет/56379/"));
    }

    @Test
    public void russianQueryPassesValidation() {
        assertTrue(UrlUtils.isValidUrl("https://market.yandex.ru/qwe/56379?hello=медвед"));
    }

    @Test
    public void emptySchemePassesValidation() {
        assertTrue(UrlUtils.isValidUrl("//market.yandex.ru/qwe/56379?hello=медвед"));
    }

    @Test
    public void normalizationTrimsSpaces() throws Exception {
        assertEquals("https://www.yandex.ru/gg/ff/m.jpg/",
            UrlUtils.validateAndNormalizeHttpURL("   https://www.yandex.ru/gg/ff/m.jpg/   "));
    }

    @Test
    public void normalizationAddsScheme() throws Exception {
        assertEquals("http://www.yandex.ru/gg/ff/m.jpg/",
            UrlUtils.validateAndNormalizeHttpURL("   www.yandex.ru/gg/ff/m.jpg/   "));
    }

    @Test
    public void normalizationReturnsNullForNull() throws Exception {
        assertNull(UrlUtils.validateAndNormalizeHttpURL(null));
    }

    @Test
    public void normalizationThrowsIfInvalid() throws Exception {
        expectedException.expect(ValidatorException.class);
        UrlUtils.validateAndNormalizeHttpURL("http://#( #^&/gg/ff/m.jpg/");
    }

    @Test
    public void normalizationUrlWithCyrillic() throws ValidatorException {
        assertEquals("https://www.гелеос.рус", UrlUtils.validateAndNormalizeHttpURL("https://www.гелеос.рус"));
    }

    @Test
    public void normalizationUrlWithCyrillicWithPath() throws ValidatorException {
        assertEquals("https://www.гелеос.рус/dir1/dir2",
                     UrlUtils.validateAndNormalizeHttpURL("https://www.гелеос.рус/dir1/dir2"));
    }

    @Test
    public void normalizationUrlWithCyrillicWithQuery() throws ValidatorException {
        assertEquals("https://www.гелеос.рус?name=value",
                     UrlUtils.validateAndNormalizeHttpURL("https://www.гелеос.рус?name=value"));
    }

    @Test
    public void normalizationUrlWithCyrillicWithPathAndQuery() throws ValidatorException {
        assertEquals("https://www.гелеос.рус/dir1/dir2?name=value",
                     UrlUtils.validateAndNormalizeHttpURL("https://www.гелеос.рус/dir1/dir2?name=value"));
    }

    @Test
    public void normalizationForEmptySchemeReturnsHttp() throws ValidatorException {
        assertEquals("http://market.yandex.ru/qwe/56379?hello=медвед",
            UrlUtils.validateAndNormalizeHttpURL("//market.yandex.ru/qwe/56379?hello=медвед"));
    }
}
