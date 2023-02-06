package ru.yandex.market.common.report.url.geo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GeoSearchRequestUrlBuilderTest {
    private GeoSearchRequestUrlBuilder builder;

    @Before
    public void setUp() throws Exception {
        builder = new GeoSearchRequestUrlBuilder("http://report.tst.vs.market.yandex.net:17051/yandsearch");
    }

    @Test
    public void shouldBuildUrl() {
        GeoSearchRequest request = new GeoSearchRequest(242102, "4wTSrqUBspf3hkJrw6Peww", 213L, 1);
        String url = builder.build(request);

        Assert.assertEquals(
                "http://report.tst.vs.market.yandex" +
                        ".net:17051/yandsearch?place=geo&fesh=242102&offerid=4wTSrqUBspf3hkJrw6Peww&rids=213&numdoc=1" +
                        "&use-virt-shop=0",
                url
        );

    }

}
