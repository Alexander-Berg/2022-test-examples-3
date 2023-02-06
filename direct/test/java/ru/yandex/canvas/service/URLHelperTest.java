package ru.yandex.canvas.service;

import java.net.URL;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.canvas.service.scraper.URLHelper;

@RunWith(SpringRunner.class)
public class URLHelperTest {

    @Test
    public void russianUrlTest() {
        URL uri = URLHelper.normalizeUrl("президент.рф");
        Assert.assertEquals("http://xn--d1abbgf6aiiy.xn--p1ai", uri.toString());
    }

    @Test
    public void httpRussianUrlTest() {
        URL uri = URLHelper.normalizeUrl("http://президент.рф");
        Assert.assertEquals("http://xn--d1abbgf6aiiy.xn--p1ai", uri.toString());
    }

    @Test
    public void httpsRussianUrlTest() {
        URL uri = URLHelper.normalizeUrl("https://президент.рф");
        Assert.assertEquals("https://xn--d1abbgf6aiiy.xn--p1ai", uri.toString());
    }

    @Test
    public void httpsUrlTest() {
        URL uri = URLHelper.normalizeUrl("https://somesite.com");
        Assert.assertEquals("https://somesite.com", uri.toString());
    }

    @Test
    public void spaceLeadHttpsUrlTest() {
        URL uri = URLHelper.normalizeUrl(" https://somesite.com");
        Assert.assertEquals("https://somesite.com", uri.toString());
    }

    @Test
    public void httpUrlTest() {
        URL uri = URLHelper.normalizeUrl("http://somesite.com");
        Assert.assertEquals("http://somesite.com", uri.toString());
    }

    @Test
    public void httpUrlPortTest() {
        URL uri = URLHelper.normalizeUrl("http://somesite.com:8080");
        Assert.assertEquals("http://somesite.com:8080", uri.toString());
    }

    @Test
    public void httpUrlPortQueryTest() {
        URL uri = URLHelper.normalizeUrl("http://somesite.com:8080/path/file?param=1");
        Assert.assertEquals("http://somesite.com:8080/path/file?param=1", uri.toString());
    }
}
