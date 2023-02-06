package ru.yandex.common.util;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestCase;

import ru.yandex.common.util.json.PlainJsonParametersSource;

import static org.junit.Assert.assertNotEquals;

/**
 * Date: Dec 24, 2009
 *
 * @author Alexander Astakhov (leftie@yandex-team.ru)
 */
public class URLUtilsTest extends TestCase {

    public void testCanonizeURL() {
        try {
            assertEquals("http://www.ya.ru/", URLUtils.canonizeURL("hTtp://wWw.Ya.rU"));
            assertEquals("http://www.ya.ru/", URLUtils.canonizeURL("http://www.ya.ru:80"));
            assertEquals("https://www.ya.ru/", URLUtils.canonizeURL("https://www.ya.ru:443"));
            assertEquals("https://www.ya.ru:3333/", URLUtils.canonizeURL("https://www.ya.ru:3333"));
            assertEquals("http://xn--d1acpjx3f.xn--p1ai/", URLUtils.canonizeURL("http://яндекс.рф/"));
            assertEquals("http://xn--d1acpjx3f.xn--p1ai/", URLUtils.canonizeURL("http://Яндекс.Рф/"));
            assertEquals("http://xn--d1acpjx3f.xn--p1ai/%D1%82%D0%B5%D1%81%D1%82/", URLUtils.canonizeURL("http://яндекс.рф/тест/"));
            assertEquals("http://xn--d1acpjx3f.xn--p1ai/%D1%82%D0%B5%D1%81%D1%82", URLUtils.canonizeURL("http://xn--d1acpjx3f.xn--p1ai/%D1%82%D0%B5%D1%81%D1%82"));
            assertEquals("http://www.ya.ru/", URLUtils.canonizeURL("http://www.ya.ru/?"));
            assertEquals("http://www.ya.ru/", URLUtils.canonizeURL("http://www.ya.ru#removed"));
            assertEquals("http://www.ya.ru/?a=b", URLUtils.canonizeURL("http://www.ya.ru?a=b#removed"));
            assertEquals("http://www.ya.ru/?_escaped_fragment_=not_removed", URLUtils.canonizeURL("http://www.ya.ru#!not_removed"));

            // Slash will be added, if path is empty
            assertEquals("http://ya.ru/", URLUtils.canonizeURL("http://ya.ru"));
            assertEquals("http://www.ya.ru/", URLUtils.canonizeURL("http://www.ya.ru/"));
            assertEquals("http://example.com/?k1=v1&k2=v2", URLUtils.canonizeURL("http://example.com?k1=v1&k2=v2"));

            // Scheme wouldn't be added
            assertNotEquals("http://ya.ru/", URLUtils.canonizeURL("ya.ru"));
            assertEquals("http://ya.ru/", URLUtils.canonizeURL("http://ya.ru"));
            assertEquals("ftp://ya.ru/", URLUtils.canonizeURL("ftp://ya.ru"));
            assertEquals("https://ya.ru/", URLUtils.canonizeURL("https://ya.ru"));

            // Host will be lowercased
            assertEquals("http://ya.ru/", URLUtils.canonizeURL("http://ya.ru"));
            assertEquals("http://ya.ru/", URLUtils.canonizeURL("http://YA.ru"));
            assertEquals("http://yandex.com/", URLUtils.canonizeURL("http://YaNdEx.CoM"));
            assertEquals("http://ya.ru/HELLO", URLUtils.canonizeURL("http://Ya.Ru/HELLO"));

            // Path will be percent-encoded
            assertEquals("http://ya.ru/asdf", URLUtils.canonizeURL("http://ya.ru/asdf#ddd"));
            assertEquals("http://ya.ru/", URLUtils.canonizeURL("http://ya.ru#aa"));
            assertEquals("http://ya.ru/", URLUtils.canonizeURL("http://ya.ru/#aa"));
            assertEquals("http://ya.ru/aa", URLUtils.canonizeURL("http://ya.ru/aa#"));

            // Look at specification
            assertEquals("http://www.ru/aaa?_escaped_fragment_=ooo", URLUtils.canonizeURL("http://www.ru/aaa#!ooo"));
            assertEquals("http://www.example.com/?myquery&_escaped_fragment_=k1=v1&k2=v2", URLUtils.canonizeURL("http://www.example.com?myquery#!k1=v1&k2=v2"));
            assertEquals("http://kinochi.org/?_escaped_fragment_=/view/695", URLUtils.canonizeURL("http://kinochi.org/#!/view/695"));
            assertEquals("https://groups.google.com/forum/?_escaped_fragment_=/fido7.ru.anime", URLUtils.canonizeURL("https://groups.google.com/forum/#!/fido7.ru.anime"));

            // IPv6 works
            assertEquals("http://[2a02:6b8::408:426c:8fff:fe26:5a1e]:8080/", URLUtils.canonizeURL("http://[2a02:6b8::408:426c:8fff:fe26:5a1e]:8080"));
            assertEquals("http://[1080:0:0:0:8:800:200c:417a]/index.html", URLUtils.canonizeURL("http://[1080:0:0:0:8:800:200C:417A]/index.html"));

            // Test HTTP(S) ports
            assertEquals("http://ya.ru/", URLUtils.canonizeURL("http://ya.ru:80/"));
            assertEquals("https://ya.ru/", URLUtils.canonizeURL("https://ya.ru:443/"));

            // Without changes
            assertEquals("http://ya.ru/a&b&c", URLUtils.canonizeURL("http://ya.ru/a&b&c"));
            assertEquals("http://ya.ru/a?b&c", URLUtils.canonizeURL("http://ya.ru/a?b&c"));
            // Port, percent-encoding for spaces, punycode
            assertEquals("http://xn--d1abbgf6aiiy.xn--p1ai:1010/hello%20hello", URLUtils.canonizeURL("http://президент.рф:1010/hello hello"));
            // Lowercase, punycode, percent-encoding
            assertEquals("http://xn--d1abbgf6aiiy.xn--p1ai/%D0%9F%D1%80%D0%B8%D0%B2%D0%B5%D1%82%20bonjour%20hello", URLUtils.canonizeURL("http://ПРезидент.рф/Привет bonjour hello"));
            // Empty path
            assertEquals( "http://www.ru:1010/", URLUtils.canonizeURL("http://www.ru:1010"));
            // 'index.html' wouldn't be deleted
            assertEquals("http://www.ru/index.html", URLUtils.canonizeURL("http://www.ru/index.html"));
            // Punycode, percent-encoding
            assertEquals("http://xn--41a.xn--p1ai/%D0%A2%20z", URLUtils.canonizeURL("http://я.рф/%D0%A2 z"));
            // Query args wouldn't be sorted
            assertEquals("http://www.ru/aaa?bb=b&aa=a", URLUtils.canonizeURL("http://www.ru/aaa?bb=b&aa=a"));
            // Trail slash in path
            assertEquals("http://ya.ru/path", URLUtils.canonizeURL("http://ya.ru/path"));
            assertEquals("http://ya.ru/path/", URLUtils.canonizeURL("http://ya.ru/path/"));
            // Path wouldn't be lowercased
            assertEquals("http://ya.ru/BlaBla?BlaBla=Ooo", URLUtils.canonizeURL("http://ya.ru/BlaBla?BlaBla=Ooo"));
            // Duplicate args wouldn't be deleted
            assertEquals("http://ya.ru/oo?aa=a&aa=a&aa=b", URLUtils.canonizeURL("http://ya.ru/oo?aa=a&aa=a&aa=b"));
            // 'www' wouldn't be deleted
            assertEquals("http://www.ya.ru/", URLUtils.canonizeURL("http://www.ya.ru"));
            // '#' will be deleted
            assertEquals("http://ya.ru/a?b=b", URLUtils.canonizeURL("http://ya.ru/a?b=b#c"));
            // Slash in host, '#!', '#'
            assertEquals("http://ya.ru/?aa&bb&_escaped_fragment_=cc%23dd", URLUtils.canonizeURL("http://ya.ru?aa&bb#!cc#dd"));

            //tests copypasted from https://a.yandex-team.ru/arc/trunk/arcadia/kernel/urlnorm/normalize_ut.cpp?rev=5030297
        } catch (URISyntaxException ignored) {
        }
    }

    public void testGetPath() {
        assertEquals("/blabla/", URLUtils.getPath("http://www.ya.ru/blabla/"));
        assertEquals("/blabla", URLUtils.getPath("http://www.ya.ru/blabla"));
        assertEquals("/", URLUtils.getPath("http://www.ya.ru/"));
        assertEquals("", URLUtils.getPath("http://www.ya.ru"));
    }

    public void testGetL2Domain() {
        assertEquals("infocar.ua", URLUtils.getL2Domain("ford-focus.infocar.ua/otzyv"));
        assertEquals("infocar.ua", URLUtils.getL2Domain("infocar.ua/otzyv"));
        assertEquals("infocar.ua", URLUtils.getL2Domain("infocar.ua"));
    }

    public void testAddParameters() {
        final Map<String, String> params = new LinkedHashMap<>();
        params.put("param1", "value1");
        params.put("param2", "value2");
        params.put("param3", "value3");

        assertEquals(
                "http://www.ya.ru/blabla?param1=value1&param2=value2&param3=value3",
                URLUtils.addParameters("http://www.ya.ru/blabla", params)
        );

        assertEquals(
                "http://www.ya.ru/blabla?param=value&param1=value1&param2=value2&param3=value3",
                URLUtils.addParameters("http://www.ya.ru/blabla?param=value", params)
        );

        assertEquals(
                "http://www.ya.ru/blabla?param=value&param4=value4&param1=value1&param2=value2&param3=value3",
                URLUtils.addParameters("http://www.ya.ru/blabla?param=value&param4=value4", params)
        );

        assertEquals(
                "http://www.ya.ru/blabla?param=value&param1=value1&param2=value2&param3=value3#123",
                URLUtils.addParameters("http://www.ya.ru/blabla?param=value#123", params)
        );
    }

    public void testAddQuery() {
        final String query = "param1=value1&param2=value2&param3=value3";

        assertEquals(
                "http://www.ya.ru/blabla?param1=value1&param2=value2&param3=value3",
                URLUtils.addQuery("http://www.ya.ru/blabla", query)
        );

        assertEquals(
                "http://www.ya.ru/blabla?param=value&param1=value1&param2=value2&param3=value3",
                URLUtils.addQuery("http://www.ya.ru/blabla?param=value", query)
        );

        assertEquals(
                "http://www.ya.ru/blabla?param=value&param4=value4&param1=value1&param2=value2&param3=value3",
                URLUtils.addQuery("http://www.ya.ru/blabla?param=value&param4=value4", query)
        );

        assertEquals(
                "http://www.ya.ru/blabla?param=value&param1=value1&param2=value2&param3=value3#123",
                URLUtils.addQuery("http://www.ya.ru/blabla?param=value#123", query)
        );
    }

    public void testGetDomain() {
        assertEquals("ya.ru", URLUtils.getDomain("http://www.ya.ru/blabla"));
        assertEquals("ya.ru", URLUtils.getDomain("http://ya.ru/blabla"));
    }

    public void testGetDomainWithWWW() {
        assertEquals("www.ya.ru", URLUtils.stripPath(URLUtils.stripProtocol("http://www.ya.ru/blabla")));
    }

    public void testGetDomainWithoutEndingSlash() {
        assertEquals("oktogo.ru", URLUtils.getDomain("http://oktogo.ru?utm_source=yandex&utm_medium=maps&utm_content=footer"));
    }

    public void testStripAnyProtocol() {
        assertEquals("ya.ru", URLUtils.stripAnyProtocol("ssh://ya.ru"));
        assertEquals("ya.ru", URLUtils.stripAnyProtocol("p2p://ya.ru"));
    }

    public void testStripTrailingSlash() {
        assertEquals("ya.ru/some/path", URLUtils.stripTrailingSlash("ya.ru/some/path/"));
        assertEquals("ya.ru/some/path", URLUtils.stripTrailingSlash("ya.ru/some/path"));
    }

    public void testRemoveParamsAndfilename() {
        assertEquals("ya.ru/some/path/", URLUtils.removeParamsAndFilename("ya.ru/some/path/filename?param=value#anchor"));
        assertEquals("ya.ru/some/", URLUtils.removeParamsAndFilename("ya.ru/some/path"));
    }

    public void testBuildQueryString() {
        assertEquals("foo=bar&foo=baz&zoo=zaa", URLUtils.buildQueryString(new PlainJsonParametersSource("" +
                "{'foo':['bar', 'baz'], 'zoo':'zaa'}")));
    }

    public void testIsValidHttpURL() {
        assertTrue(URLUtils.isValidHttpURL("http://yandex.ru"));
        assertTrue(URLUtils.isValidHttpURL("https://yandex.ru"));
        assertTrue(URLUtils.isValidHttpURL("https://yandex.ru/"));
        assertTrue(URLUtils.isValidHttpURL("https://yandex.ru/"));
        assertTrue(URLUtils.isValidHttpURL("https://yandex.ru/"));
        assertTrue(URLUtils.isValidHttpURL("http://bellmarket.website"));
        assertTrue(URLUtils.isValidHttpURL("http://bellezza.boutique"));

        assertTrue(URLUtils.isValidHttpURL("http://яндекс.рф"));
        assertTrue(URLUtils.isValidHttpURL("http://идн-тест.яндекс.рф"));
        assertTrue(URLUtils.isValidHttpURL("http://svn.яндекс.рф"));
        assertTrue(URLUtils.isValidHttpURL("https://яндекс.рф"));
        assertTrue(URLUtils.isValidHttpURL("http://中国互联网络信息中心.中国/"));
        assertTrue(URLUtils.isValidHttpURL("http://xn--d1acpjx3f.xn--p1ai"));

        assertTrue(URLUtils.isValidHttpURL("http://localhost"));
        assertTrue(URLUtils.isValidHttpURL("http://77.88.55.77"));

        assertTrue(URLUtils.isValidHttpURL("http://yandex.ru:8080"));
        assertTrue(URLUtils.isValidHttpURL("https://yandex.ru:443/index.html"));

        assertTrue(URLUtils.isValidHttpURL("http://идн-тест.яндекс.рф/mptest/фиды/smallsvyaznoy05.xml"));
        assertTrue(URLUtils.isValidHttpURL("http://идн-тест.яндекс.рф/mptest/feeds/smallsvyaznoy05.xml"));

        assertFalse(URLUtils.isValidHttpURL("http://yandex.a-1"));
        assertFalse(URLUtils.isValidHttpURL("http://yandex~.ru"));
        assertFalse(URLUtils.isValidHttpURL("хттпс://яндекс.рф"));
        assertFalse(URLUtils.isValidHttpURL("yandex.ru"));
        assertFalse(URLUtils.isValidHttpURL("ftp://yandex.ru"));
        assertFalse(URLUtils.isValidHttpURL("http://yandex.ru/\r\n"));
    }

    public void testIsValidDomain() {
        assertTrue(URLUtils.isValidDomain("yandex.ru"));
        assertTrue(URLUtils.isValidDomain("яндекс.рф"));
        assertTrue(URLUtils.isValidDomain("идн-тест.яндекс.рф"));

        assertTrue(URLUtils.isValidDomain("bellmarket.website"));
        assertTrue(URLUtils.isValidDomain("bellezza.boutique"));

        assertTrue(URLUtils.isValidDomain("yandex.newtld"));
        assertTrue(URLUtils.isValidDomain("яндекс.новыйдомен"));

        assertTrue(URLUtils.isValidDomain("1st-domain.com"));

        assertFalse(URLUtils.isValidDomain("abc"));
        assertFalse(URLUtils.isValidDomain("123"));
        assertFalse(URLUtils.isValidDomain("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.com")); //63+ символа
        assertFalse(URLUtils.isValidDomain("a.coooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooom")); //63+ символа
    }

    public void testConvertToASCIIDomain() {
        Arrays.asList("http", "https").forEach(protocol ->
                Arrays.asList("", "/", "/test/123", ":12345/test/123", "/test/123?a=b&e=%3D%D1",
                        "/test/123?a=b&e=абв").forEach(postfix -> {
                            String unicodeDomainUrl = protocol + "://yandex.ru" + postfix;
                            assertEquals(unicodeDomainUrl, URLUtils.convertToASCIIDomain(unicodeDomainUrl));

                            String cyrillicDomainUrl = protocol + "://яндекс.рф" + postfix;
                            // сконвертировано при помощи - https://www.punycoder.com
                            String punycodeDomainUrl = protocol + "://xn--d1acpjx3f.xn--p1ai" + postfix;
                            assertEquals(punycodeDomainUrl, URLUtils.convertToASCIIDomain(cyrillicDomainUrl));
                        }
                )
        );
    }

}
