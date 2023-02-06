package ru.yandex.market.mbo.cms.core.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.CatalogParsedUrl;

public class CatalogUrlParserTest {

    @Test
    public void testFilled() throws Exception {
        CatalogParsedUrl parsed = CatalogUrlParser.parse(
                "https://market.yandex.ru/catalog--kukhonnye-plity/54954/list" +
                        "?hid=237420&glfilter=5127129%3A12103683&glfilter=12824260%3A12824262" +
                        "&glfilter=5127130%3A12103688%2C12103687%2C12103699" +
                        "&local-offers-first=1"
        );

        Map<String, List<String>> expectedGlFilter = new HashMap<>();
        expectedGlFilter.put("5127129", Arrays.asList("12103683"));
        expectedGlFilter.put("12824260", Arrays.asList("12824262"));
        expectedGlFilter.put("5127130", Arrays.asList("12103688", "12103687", "12103699"));

        Assert.assertEquals("237420", parsed.getHid());
        Assert.assertEquals(expectedGlFilter, parsed.getGlfilter());
    }

    @Test
    public void testEmpty() throws Exception {
        CatalogParsedUrl parsed = CatalogUrlParser.parse(
                "https://market.yandex.ru/catalog--kukhonnye-plity/54954/list?&local-offers-first=1"
        );
        Map<String, List<String>> expectedGlFilter = new HashMap<>();
        Assert.assertNull("", parsed.getHid());
        Assert.assertEquals(expectedGlFilter, parsed.getGlfilter());
    }
}
