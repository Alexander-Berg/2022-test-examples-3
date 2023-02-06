package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import ru.yandex.market.api.common.DeliveryService;
import ru.yandex.market.api.common.currency.CurrencyService;
import ru.yandex.market.api.domain.OfferPrice;
import ru.yandex.market.api.domain.v2.DeliveryConditionsV2;
import ru.yandex.market.api.domain.v2.DeliveryOptionV2;
import ru.yandex.market.api.domain.v2.DeliveryServiceV2;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.report.ReportRequestContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Created by anton0xf on 22.03.17.
 */
@WithContext
@WithMocks
public class DeliveryServiceJsonParserTest extends UnitTestBase {

    private static final int SERVICE_ID = 99;
    private static final String SERVICE_NAME = "Delivery service name";

    @Mock
    private CurrencyService currencyService;
    @Mock
    private DeliveryService deliveryService;

    @Before
    public void setUp() {
        when(deliveryService.getServiceName(SERVICE_ID)).thenReturn(SERVICE_NAME);
    }

    @Test
    public void testParse() {
        ReportRequestContext context = new ReportRequestContext();
        DeliveryServiceJsonParser parser = new DeliveryServiceJsonParser(context, deliveryService, currencyService);
        DeliveryOptionV2 option = parser.parse(ResourceHelpers.getResource("delivery-option.json"));

        assertNotNull(option);
        assertTrue(option.isDefaultOption());

        assertNotNull(option.getService());
        DeliveryServiceV2 service = (DeliveryServiceV2) option.getService();
        assertEquals(SERVICE_ID, service.getId());
        assertEquals(SERVICE_NAME, service.getName());

        assertNotNull(option.getConditions());
        DeliveryConditionsV2 conditions = option.getConditions();

        assertNotNull(conditions.getPrice());
        OfferPrice price = conditions.getPrice();
        assertEquals("110", price.getValue());

        assertTrue(conditions.getDeliveryIncluded());
        assertEquals(Integer.valueOf(1), conditions.getDaysFrom());
        assertEquals(Integer.valueOf(2), conditions.getDaysTo());
        assertEquals(Integer.valueOf(23), conditions.getOrderBefore());
    }

}
