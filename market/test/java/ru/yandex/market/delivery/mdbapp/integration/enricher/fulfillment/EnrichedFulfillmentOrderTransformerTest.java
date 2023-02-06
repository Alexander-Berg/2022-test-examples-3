package ru.yandex.market.delivery.mdbapp.integration.enricher.fulfillment;

import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import steps.LocationSteps;
import steps.PartnerInfoSteps;
import steps.logisticsPointSteps.LogisticPointSteps;
import steps.orderSteps.AddressSteps;
import steps.orderSteps.OrderSteps;
import steps.shopSteps.ShopSteps;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.components.geo.Location;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fetcher.InletFetcher;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fetcher.LocationFetcher;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fetcher.ReturnInletFetcher;
import ru.yandex.market.delivery.mdbapp.integration.payload.EnrichedFulfillmentOrder;
import ru.yandex.market.delivery.mdbapp.integration.payload.ExtendedOrder;
import ru.yandex.market.delivery.mdbapp.integration.payload.LogisticsPoint;
import ru.yandex.market.delivery.mdbapp.integration.payload.LogisticsPointPair;
import ru.yandex.market.delivery.mdbapp.integration.payload.ReturnInletData;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerInfoDTO;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class EnrichedFulfillmentOrderTransformerTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    @Mock
    private LocationFetcher locationFetcher;
    @Mock
    private InletFetcher inletFetcher;
    @Mock
    private ReturnInletFetcher returnInletFetcher;

    private EnrichedFulfillmentOrderTransformer fulfillmentOrderTransformer;

    private static final Long PARTNER_INFO_ID = 123L;

    private Location location = LocationSteps.getLocation();
    private LogisticsPoint outlet = LogisticPointSteps.getDefaultOutlet();
    private LogisticsPointPair logisticsPointPair = new LogisticsPointPair(outlet, null);
    private SoftAssertions assertions;

    @Before
    public void setUp() {
        when(locationFetcher.fetch(any(LogisticsPoint.class))).thenReturn(location);
        when(inletFetcher.fetch(any(Order.class))).thenReturn(logisticsPointPair);
        when(returnInletFetcher.doFetch(any(Long.class))).thenReturn(logisticsPointPair);
        fulfillmentOrderTransformer = new EnrichedFulfillmentOrderTransformer(
            locationFetcher,
            inletFetcher,
            returnInletFetcher
        );
        assertions = new SoftAssertions();
    }

    @After
    public void tearDown() {
        assertions.assertAll();
    }

    @Test
    public void transform() {
        EnrichedFulfillmentOrder enrichedFulfillmentOrder = getFfOrder();
        ExtendedOrder order = fulfillmentOrderTransformer.transform(enrichedFulfillmentOrder);
        assertions.assertThat(order.getOrder()).as("must be instance of Order")
            .isInstanceOf(Order.class);
        assertions.assertThat(order.getInlet()).as("must be instance of LogisticsPoint")
            .isInstanceOf(LogisticsPoint.class);
        assertions.assertThat(order.getOrderData()).as("must be instance of ExtendedOrder.OrderData")
            .isInstanceOf(ExtendedOrder.OrderData.class);
        assertions.assertThat(order.getOutlet()).as("must be instance of LogisticsPoint")
            .isInstanceOf(LogisticsPoint.class);
        assertions.assertThat(order.getPartnerInfo()).as("must be instance of PartnerInfo")
            .isInstanceOf(PartnerInfoDTO.class);
        assertions.assertThat(order.getReturnInletData()).as("must be instance of ReturnInletData")
            .isInstanceOf(ReturnInletData.class);
    }

    private EnrichedFulfillmentOrder getFfOrder() {
        Order orderAfter = OrderSteps.getFilledOrder();

        EnrichedFulfillmentOrder fforder = new EnrichedFulfillmentOrder(orderAfter);
        fforder.setOutlet(outlet);
        fforder.setAddress(AddressSteps.getAddress());
        fforder.setLocationTo(location);
        fforder.setShop(ShopSteps.getDefaultShop());
        fforder.setPartnerInfo(PartnerInfoSteps.getEmptyOrgInfoPartnerInfoDTO(PARTNER_INFO_ID));

        return fforder;
    }
}
