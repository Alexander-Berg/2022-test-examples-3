package ru.yandex.market.sdk.userinfo;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.sdk.userinfo.util.UrlParser;

/**
 * @authror dimkarp93
 */
public class UrlParserTest {
    @Test
    public void parseHostWithScheme() {
        String host = UrlParser.extractHostWithScheme("http://blackbox.yandex.ru", "https");
        Assert.assertEquals(host, "http://blackbox.yandex.ru");
    }

    @Test
    public void parseHostWithSchemeAndPath() {
        String host = UrlParser.extractHostWithScheme("https://blackbox.yandex.ru/blackbox", "https");
        Assert.assertEquals(host, "https://blackbox.yandex.ru");
    }

    @Test
    public void parseHostWithSchemeAndPathAndPort() {
        String host = UrlParser.extractHostWithScheme("http://blackbox.yandex.ru:123/blackbox", "https");
        Assert.assertEquals(host, "http://blackbox.yandex.ru");
    }

    @Test
    public void parseHostWithSchemeAndPathAndPortAndQuery() {
        String host = UrlParser.extractHostWithScheme("https://blackbox.yandex.ru:123/blackbox?abc=123&q=r", "https");
        Assert.assertEquals(host, "https://blackbox.yandex.ru");
    }

    @Test
    public void parseHostWithoutSchemeDefaultHttp() {
        String host = UrlParser.extractHostWithScheme("blackbox.yandex.ru", "http");
        Assert.assertEquals(host, "http://blackbox.yandex.ru");
    }

    @Test
    public void parseHostWithoutSchemeDefaultHttps() {
        String host = UrlParser.extractHostWithScheme("blackbox.yandex.ru", "https");
        Assert.assertEquals(host, "https://blackbox.yandex.ru");
    }
}
