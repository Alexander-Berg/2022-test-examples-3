package ru.yandex.market.api.common.url;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;

/**
 * @author dimkarp93
 */
public class MarketUrlTest extends UnitTestBase {

    private static final String[] URI = new String[] {
        "https://market.yandex.ru/catalog",
        "http://market.yandex.ru",
        "https://market.yandex.ru/",
        "market.yandex.ru",
        "market.yandex.ru/product/123",
        "market.yandex.ru/product--some-slug/123",
        "market.yandex.ru/catalog--some-slug/4?hid=5",
        "yandexmarket://catalog/root?hid=51&glfilter=12:34&glfilter=13:45",
        "/articles/kak-vybrat-velosiped?suggest_text=kak&suggest=1&suggest_type=article"
    };

    private static final String[][] DATA = new String[][] {
        new String[]{"https", "market.yandex.ru", "/catalog/", null},
        new String[]{"http", "market.yandex.ru", "", null},
        new String[]{"https", "market.yandex.ru", "", null},
        new String[]{"", "market.yandex.ru", "", null},
        new String[]{"", "market.yandex.ru", "/product/123/", null},
        new String[]{"", "market.yandex.ru", "/product--some-slug/123/", null},
        new String[]{"", "market.yandex.ru", "/catalog--some-slug/4/", "hid=5"},
        new String[]{"yandexmarket", "", "/catalog/root/", "hid=51&glfilter=12:34&glfilter=13:45"},
        new String[]{"", "market.yandex.ru", "/articles/kak-vybrat-velosiped/", "suggest_text=kak&suggest=1&suggest_type=article"}
    };

    @Test
    public void testParse() {
        Assert.assertEquals(URI.length, DATA.length);
        for (int i = 0; i < URI.length; ++i) {
            test(URI[i], DATA[i]);
        }
    }

    private void test(String uriQuery, String[] data) {
        MarketUrl uri = MarketUrl.of(uriQuery);

        Assert.assertEquals(data[0], uri.getScheme());
        Assert.assertEquals(data[1], uri.getDomain());
        Assert.assertEquals(data[2], uri.getPath());
        Assert.assertEquals(data[3], uri.getQuery());

    }

}
