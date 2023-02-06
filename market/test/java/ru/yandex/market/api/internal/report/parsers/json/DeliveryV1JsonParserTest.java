package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import ru.yandex.market.api.common.DeliveryService;
import ru.yandex.market.api.common.currency.CurrencyService;
import ru.yandex.market.api.geo.GeoRegionService;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.report.ReportRequestContext;
import ru.yandex.market.api.offer.Delivery;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;

@WithMocks
public class DeliveryV1JsonParserTest extends UnitTestBase {

    @Mock
    private ReportRequestContext reportRequestContext;

    @Mock
    private GeoRegionService geoRegionService;

    @Mock
    private CurrencyService currencyService;

    @Mock
    private DeliveryService deliveryService;

    private DeliveryV1JsonParser parser;

    @Before
    public void setUp() throws Exception {
        Mockito.when(geoRegionService.getInfoOrNull(anyInt())).thenReturn(null);

        this.parser = new DeliveryV1JsonParser(reportRequestContext, currencyService, geoRegionService,
                this.deliveryService);
    }

    @Test
    public void testDefaultDelivery(){
        Delivery delivery =  this.parser.parse(ResourceHelpers.getResource("delivery.json"));

        assertEquals("102", delivery.getPrice().getValue());
    }

    @Test
    public void testDefaultDeliveryFree(){
        Delivery delivery =  this.parser.parse(ResourceHelpers.getResource("delivery-free.json"));

        assertEquals("0", delivery.getPrice().getValue());
        assertTrue(delivery.isFree());
    }
}
