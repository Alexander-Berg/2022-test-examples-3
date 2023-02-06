package ru.yandex.market.api.internal.report.parsers.json;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.api.common.DeliveryService;
import ru.yandex.market.api.common.currency.Currency;
import ru.yandex.market.api.common.currency.CurrencyService;
import ru.yandex.market.api.domain.v2.DeliveryV2;
import ru.yandex.market.api.geo.GeoRegionService;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.report.ReportRequestContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;

@WithMocks
public class DeliveryV2JsonParserTest extends BaseTest {

    @Mock
    private ReportRequestContext reportRequestContext;

    @Mock
    private GeoRegionService geoRegionService;

    @Inject
    private CurrencyService currencyService;

    @Inject
    private DeliveryService deliveryService;

    @Before
    public void setUp() throws Exception {
        Mockito.when(geoRegionService.getInfoOrNull(anyInt())).thenReturn(null);
    }

    @Test
    public void testDefaultDelivery() {
        DeliveryV2 delivery = this.getParser().parse(ResourceHelpers.getResource("delivery.json"));

        assertEquals("102", delivery.getPrice().getValue());
    }

    @Test
    public void testDefaultDeliveryFree() {
        DeliveryV2 delivery = this.getParser().parse(ResourceHelpers.getResource("delivery-free.json"));

        assertEquals("0", delivery.getPrice().getValue());
        assertTrue(delivery.isFree());
    }

    @Test
    public void testDeliveryBriefWithCurrency() {
        Mockito.when(reportRequestContext.getCurrency())
            .thenReturn(Currency.RUR);

        DeliveryV2 delivery = this.getParser(reportRequestContext).parse(ResourceHelpers.getResource("delivery.json"));

        assertEquals("в Mосквy — 102 руб., возможен самовывоз", delivery.getBrief());
    }

    @Test
    public void testDeliveryBriefWithoutCurrency() {
        DeliveryV2 delivery = this.getParser().parse(ResourceHelpers.getResource("delivery.json"));

        assertEquals("в Mосквy — 102, возможен самовывоз", delivery.getBrief());

    }

    @Test
    public void testDeliveryIsEda() {
        DeliveryV2 delivery = this.getParser().parse(ResourceHelpers.getResource("delivery-eda.json"));

        assertTrue(delivery.isEda());

    }

    public DeliveryV2JsonParser getParser(ReportRequestContext reportRequestContext) {
        return new DeliveryV2JsonParser(reportRequestContext, currencyService, geoRegionService,
                this.deliveryService);
    }

    public DeliveryV2JsonParser getParser() {
        return getParser(reportRequestContext);
    }
}
