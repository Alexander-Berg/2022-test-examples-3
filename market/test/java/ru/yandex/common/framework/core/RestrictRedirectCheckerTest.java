package ru.yandex.common.framework.core;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author imelnikov
 */
public class RestrictRedirectCheckerTest {

    @Test
    public void testProtocolRestrict() {
        RestrictRedirectChecker redirectChecker = new RestrictRedirectChecker();
        assertTrue(redirectChecker.canRedirect("http://market.yandex.ru/some/url.xml"));
        assertTrue("Redirect without protocol", redirectChecker.canRedirect("//market.yandex.ru/some/url.xml"));
        assertTrue("Redirect relative path", redirectChecker.canRedirect("/some/url.xml"));

        assertFalse("Fail redirect to extrenal url", redirectChecker.canRedirect("http://lenta.ru"));
        assertFalse("Fail redirect to extrenal url without protocol", redirectChecker.canRedirect("//lenta.ru"));

        assertFalse(redirectChecker.canRedirect("http://google.com\n"));
    }

}
