package ru.yandex.market.robot.shared.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Dmitriy Kotelnikov <a href="mailto:kotelnikov@yandex-team.ru"></a>
 * @date 30.01.12
 */
public class UrlCheckerTest extends Assert {
    @Test
    public void testGetHostForUrl() throws Exception {
        assertEquals("ya.ru", UrlChecker.getHostForUrl("http://ya.ru"));
        assertEquals("test.ya-ma.ru", UrlChecker.getHostForUrl("http://test.ya-ma.ru"));
        assertEquals("test.ya.ru", UrlChecker.getHostForUrl("http://www.test.ya.ru/hello/my.ddddd/"));
        assertEquals("test.ya.ru", UrlChecker.getHostForUrl("http://test.ya.ru/"));
        assertEquals("test.ya.ru", UrlChecker.getHostForUrl("https://test.ya.ru"));
        assertEquals("1.2.3.4.5.test.ya.ru", UrlChecker.getHostForUrl("https://1.2.3.4.5.test.ya.ru"));
        assertEquals("домен.рф", UrlChecker.getHostForUrl("http://домен.рф"));
    }
}
