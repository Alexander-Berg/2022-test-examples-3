package ru.yandex.market.api.internal.report.parsers.json;

import java.util.List;

import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.market.api.common.DeliveryService;
import ru.yandex.market.api.common.currency.CurrencyService;
import ru.yandex.market.api.common.url.MarketUrls;
import ru.yandex.market.api.common.url.params.UrlParamsFactoryImpl;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.geo.GeoRegionService;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.report.ReportRequestContext;
import ru.yandex.market.api.internal.report.data.ActualDelivery;
import ru.yandex.market.api.internal.report.parsers.json.filters.FilterFactory;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author apershukov
 */
@WithMocks
@WithContext
public class ActualDeliveryJsonParserTest extends BaseTest {

    @Mock
    private CurrencyService currencyService;
    @Mock
    private DeliveryService deliveryService;
    @Mock
    private GeoRegionService geoRegionService;
    @Mock
    private MarketUrls marketUrls;
    @Mock
    private UrlParamsFactoryImpl urlParamsFactoryImpl;
    @Mock
    private ClientHelper clientHelper;

    @Test
    public void testParse() {
        ActualDeliveryJsonParser parser = new ActualDeliveryJsonParser(
                new OfferV2JsonParser(
                        new ReportRequestContext(),
                        currencyService,
                        deliveryService,
                        geoRegionService,
                        new FilterFactory(),
                        marketUrls,
                        clientHelper)
        );
        ActualDelivery delivery = parser.parse(ResourceHelpers.getResource("actual-delivery.json"));
        assertNotNull(delivery);
        assertEquals(3000, delivery.getFreeThreshold().intValue());
        assertEquals(1600, delivery.getTotalPrice().intValue());
        assertEquals(1400, delivery.getFreeDeliveryRemainder().intValue());
        assertEquals(249, delivery.getDeliveryPrice().intValue());

        List<OfferV2> offers = delivery.getOffers();
        assertThat(offers, hasSize(1));

        assertEquals("06-JOu_UgomV-_hpt3UQ4g", offers.get(0).getId().getWareMd5());
        assertEquals("77823", offers.get(0).getPrice().getValue());
    }
}
