package ru.yandex.market.delivery.mdbapp.integration.enricher;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import steps.LocationSteps;
import steps.ParcelSteps;
import steps.logisticsPointSteps.LogisticPointSteps;
import steps.orderSteps.OrderSteps;
import steps.shopSteps.ShopSteps;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.components.geo.Location;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fetcher.InletFetcher;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fetcher.LocationFetcher;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fetcher.LogisticsPointFetcher;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fetcher.ShopFetcher;
import ru.yandex.market.delivery.mdbapp.integration.payload.ExtendedOrder;
import ru.yandex.market.delivery.mdbapp.integration.payload.LogisticsPoint;
import ru.yandex.market.delivery.mdbapp.integration.payload.LogisticsPointPair;
import ru.yandex.market.mbi.api.client.entity.shops.Shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryType.DELIVERY;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryType.PICKUP;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryType.POST;

public class RedOrderEnricherTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private ShopFetcher shopFetcher;

    @Mock
    private LogisticsPointFetcher logisticsPointFetcher;

    @Mock
    private LocationFetcher locationFetcher;

    @Mock
    private InletFetcher inletFetcher;

    private OrderExternalEnricher redOrderEnricher;
    private ExtendedOrder extendedOrder = new ExtendedOrder();
    private Shop shop;
    private Location location;


    @Before
    public void before() {
        redOrderEnricher = new OrderExternalEnricher(
            shopFetcher,
            inletFetcher,
            logisticsPointFetcher,
            locationFetcher
        );

        Order order = OrderSteps.getFilledOrder();
        order.getItems().forEach(item -> {
            item.setId(1L);
        });

        extendedOrder.setOrder(order);
        extendedOrder.getOrder().getDelivery().setOutletId(123L);
        extendedOrder.getOrder().getDelivery().setShipment(ParcelSteps.getParcel());

        shop = ShopSteps.getDefaultShop(extendedOrder.getOrder().getShopId());
        location = LocationSteps.getLocation(extendedOrder.getOrder().getDelivery().getRegionId());
        when(shopFetcher.fetch(extendedOrder.getOrder())).thenReturn(shop);
        when(logisticsPointFetcher.fetch(extendedOrder.getOrder())).thenReturn(LogisticPointSteps.getDefaultOutlet());
        when(locationFetcher.fetch(any(Order.class))).thenReturn(location);
        when(locationFetcher.fetch(any(Shop.class))).thenReturn(location);
        when(locationFetcher.fetch(any(LogisticsPoint.class))).thenReturn(location);
        when(inletFetcher.
            fetch(any(Order.class))).thenReturn(new LogisticsPointPair(LogisticPointSteps.getDefaultOutlet(), null));
    }

    @Test
    public void enrichOrderTestShopFilledCorrectly() {
        redOrderEnricher.enrich(extendedOrder);
        assertThat(extendedOrder.getShop().getId())
            .as("Shop id after enriching order")
            .isEqualTo(extendedOrder.getOrder().getShopId());
    }

    @Test
    public void enrichOrderTestOutletForDeliveryNotSet() {
        extendedOrder.getOrder().getDelivery().setType(DeliveryType.DELIVERY);
        redOrderEnricher.enrich(extendedOrder);
        assertThat(extendedOrder.getOutlet()).as("Outlet from extended order").isNull();
    }

    @Test
    public void enrichOrderTestOutletForPostNotSet() {
        extendedOrder.getOrder().getDelivery().setType(POST);
        redOrderEnricher.enrich(extendedOrder);
        assertThat(extendedOrder.getOutlet()).as("Outlet from extended order").isNull();
    }

    @Test
    public void enrichOrderTestOutletForPickupSet() {
        extendedOrder.getOrder().getDelivery().setType(DeliveryType.PICKUP);
        redOrderEnricher.enrich(extendedOrder);
        assertThat(extendedOrder.getOutlet()).as("Outlet from extended order").isNotNull();
    }

    @Test
    public void enrichOrderTestInletIsSet() {
        redOrderEnricher.enrich(extendedOrder);
        assertThat(extendedOrder.getInlet()).as("Inlet after enriching order").isNotNull();
    }

    @Test(expected = EnrichmentFailException.class)
    public void enrichOrderTestFailToFetchInlet() {
        when(inletFetcher.fetch(any(Order.class))).thenThrow(EnrichmentFailException.class);
        redOrderEnricher.enrich(extendedOrder);
    }

    @Test
    public void enrichOrderTestLocationToEnriched() {
        redOrderEnricher.enrich(extendedOrder);

        assertThat(extendedOrder.getOrderData().getLocationTo().getId())
            .as("RegionId in locationTo after enriching order")
            .isEqualTo(extendedOrder.getOrder().getDelivery().getRegionId().intValue());
    }

    @Test
    public void enrichOrderTestPickupReturnInlet() {
        extendedOrder.getOrder().getDelivery().setType(PICKUP);
        redOrderEnricher.enrich(extendedOrder);

        assertThat(extendedOrder.getReturnInletData().getInlet())
            .as("Return inlet")
            .isEqualTo(extendedOrder.getInlet());

        assertThat(extendedOrder.getReturnInletData().getLocation())
            .as("Return inlet location")
            .isEqualTo(location);
    }

    @Test
    public void enrichOrderTestPostReturnInlet() {
        extendedOrder.getOrder().getDelivery().setType(POST);
        redOrderEnricher.enrich(extendedOrder);

        assertThat(extendedOrder.getReturnInletData().getInlet())
            .as("Return inlet")
            .isEqualTo(extendedOrder.getInlet());

        assertThat(extendedOrder.getReturnInletData().getLocation())
            .as("Return inlet location")
            .isEqualTo(location);
    }

    @Test
    public void enrichOrderTestDeliveryReturnInlet() {
        extendedOrder.getOrder().getDelivery().setType(DELIVERY);
        redOrderEnricher.enrich(extendedOrder);

        assertThat(extendedOrder.getReturnInletData().getInlet())
            .as("Return inlet")
            .isEqualTo(extendedOrder.getInlet());

        assertThat(extendedOrder.getReturnInletData().getLocation())
            .as("Return inlet location")
            .isEqualTo(location);
    }
}
