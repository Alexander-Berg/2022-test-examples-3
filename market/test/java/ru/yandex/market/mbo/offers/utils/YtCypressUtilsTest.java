package ru.yandex.market.mbo.offers.utils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class YtCypressUtilsTest {

    public static final String EXAMPLE_1 = "//home/market/users/blablabla/lol1";
    public static final String EXAMPLE_2 = "//home/market/users/some/dir/lol2";
    public static final String EXAMPLE_3 = "//home/market/users/some/dir/lol3/";
    public static final String EXAMPLE_4 = "//home/market/users/some/dir/lol1&";
    public static final String EXAMPLE_5 = "//home/market/users/some/dir/lol2&/";

    @Test
    public void testExtractLastDir() {
        Assert.assertEquals("lol1", YtCypressUtils.extractLastNode(EXAMPLE_1));
        Assert.assertEquals("lol2", YtCypressUtils.extractLastNode(EXAMPLE_2));
        Assert.assertEquals("lol3", YtCypressUtils.extractLastNode(EXAMPLE_3));
    }

    @Test
    public void testExtractLink() {
        Assert.assertEquals("lol1&", YtCypressUtils.extractLastNode(EXAMPLE_4));
        Assert.assertEquals("lol2&", YtCypressUtils.extractLastNode(EXAMPLE_5));
    }

    @Test
    public void testNull() {
        Assert.assertNull(YtCypressUtils.extractLastNode(null));
    }
}
