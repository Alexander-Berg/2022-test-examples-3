package ru.yandex.market.core.order;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by kudrale on 26.02.15.
 */
public class SearchTypeFilterTest {

    @Test
    public void shopTestAlpha() {
        Assert.assertFalse(SearchType.SHOP.getFilter("test").toString().isEmpty());
    }

    @Test
    public void marketTestAlpha() {
        Assert.assertTrue("", SearchType.MARKET.getFilter("test").toString().isEmpty());
    }

    @Test
    public void anyTestAlpha() {
        Assert.assertFalse(SearchType.ANY.getFilter("test").toString().isEmpty());
    }

    @Test
    public void shopTestNum() {
        Assert.assertFalse(SearchType.SHOP.getFilter("123").toString().isEmpty());
    }

    @Test
    public void marketTestNum() {
        Assert.assertFalse("", SearchType.MARKET.getFilter("123").toString().isEmpty());
    }

    @Test
    public void anyTestNum() {
        Assert.assertFalse(SearchType.ANY.getFilter("123").toString().isEmpty());
    }
}
