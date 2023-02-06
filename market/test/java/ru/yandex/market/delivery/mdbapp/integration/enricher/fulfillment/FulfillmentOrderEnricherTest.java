package ru.yandex.market.delivery.mdbapp.integration.enricher.fulfillment;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import steps.PartnerInfoSteps;
import steps.orderSteps.OrderSteps;

import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.components.geo.GeoInfo;
import ru.yandex.market.delivery.mdbapp.components.geo.Location;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fetcher.PostLogisticsPointFetcher;
import ru.yandex.market.delivery.mdbapp.integration.payload.EnrichedFulfillmentOrder;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerInfoDTO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

public class FulfillmentOrderEnricherTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private GeoInfo geoInfo;

    @Mock
    private MbiApiClient mbiApiClient;
    @Mock
    private PostLogisticsPointFetcher postLogisticsPointFetcher;
    private OrderHistoryEvent orderHistoryEvent;
    private FulfillmentOrderEnricher fulfillmentOrderEnricher;

    private PartnerInfoDTO partnerInfoDTO;

    @Before
    public void before() {
        orderHistoryEvent = getOrderHistoryEvent();
        partnerInfoDTO = PartnerInfoSteps.getPartnerInfoDTO(1);
        when(mbiApiClient.getPartnerInfo(orderHistoryEvent.getOrderAfter().getShopId())).thenReturn(partnerInfoDTO);
        when(geoInfo.getLocation(213)).thenReturn(new Location());
        fulfillmentOrderEnricher = new FulfillmentOrderEnricher(
            geoInfo,
            mbiApiClient,
            postLogisticsPointFetcher
        );
    }

    @Test
    public void partnerInfoTest() {
        EnrichedFulfillmentOrder enrichedFulfillmentOrder = fulfillmentOrderEnricher.transform(orderHistoryEvent);

        assertNotNull("Empty partnerInfo after enriching order", enrichedFulfillmentOrder.getPartnerInfo());
        assertEquals(
            "Wrong partnerInfo id after enriching order",
            partnerInfoDTO.getId(), enrichedFulfillmentOrder.getPartnerInfo().getId()
        );
    }

    @Test
    public void shopTest() {
        EnrichedFulfillmentOrder enrichedFulfillmentOrder = fulfillmentOrderEnricher.transform(orderHistoryEvent);

        assertNotNull("Empty shop after enriching order", enrichedFulfillmentOrder.getShop());
        assertEquals(
            "Wrong shop id after enriching order",
            enrichedFulfillmentOrder.getPartnerInfo().getId(),
            enrichedFulfillmentOrder.getShop().getId()
        );
    }

    private OrderHistoryEvent getOrderHistoryEvent() {
        OrderHistoryEvent result = new OrderHistoryEvent();
        Order orderAfter = OrderSteps.getFilledOrder();
        result.setOrderAfter(orderAfter);
        return result;
    }
}
