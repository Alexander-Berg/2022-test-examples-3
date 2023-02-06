package ru.yandex.market.common.report.parser.json;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.common.report.model.outlet.Outlet;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

public class GeoMarketReportJsonParserTest {
    private GeoMarketReportJsonParser geoMarketReportJsonParser;

    @Before
    public void setUp() throws Exception {
        geoMarketReportJsonParser = new GeoMarketReportJsonParser();
    }

    @Test
    public void shouldParse() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/files/geo.json")) {
            List<Outlet> result = geoMarketReportJsonParser.parse(stream);

            Assert.assertEquals(2, result.size());

            {
                Outlet first = result.get(0);
                Assert.assertEquals("69", first.getId());
                Assert.assertNull(first.getSelfDeliveryRule().getDayFrom());
                Assert.assertNull(first.getSelfDeliveryRule().getDayTo());
                Assert.assertTrue(first.getPaymentMethods().isEmpty());
                Assert.assertNull(first.getSelfDeliveryRule().getCost());
            }

            {
                Outlet second = result.get(1);
                Assert.assertEquals("96", second.getId());
                Assert.assertEquals(0, second.getSelfDeliveryRule().getDayFrom().intValue());
                Assert.assertEquals(2, second.getSelfDeliveryRule().getDayTo().intValue());
                Assert.assertThat(
                        second.getPaymentMethods(),
                        Matchers.containsInAnyOrder("YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY")
                );
                Assert.assertEquals(new BigDecimal(50), second.getSelfDeliveryRule().getCost());
            }
        }
    }
}
