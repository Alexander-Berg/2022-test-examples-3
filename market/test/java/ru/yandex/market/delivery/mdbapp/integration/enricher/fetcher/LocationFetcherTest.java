package ru.yandex.market.delivery.mdbapp.integration.enricher.fetcher;

import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import steps.LocationSteps;
import steps.logisticsPointSteps.LogisticPointSteps;
import steps.orderSteps.OrderSteps;
import steps.shopSteps.ShopSteps;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.components.geo.GeoInfo;
import ru.yandex.market.delivery.mdbapp.components.geo.Location;
import ru.yandex.market.delivery.mdbapp.integration.enricher.EnrichmentFailException;
import ru.yandex.market.delivery.mdbapp.integration.payload.LogisticsPoint;
import ru.yandex.market.mbi.api.client.entity.shops.Shop;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LocationFetcherTest {

    private static final int REGION_ID = 123;

    private static final int DEFAULT_REGION_ID = 1; //Мск и МО

    @Mock
    private GeoInfo geoInfo;

    @InjectMocks
    private LocationFetcher locationFetcher;

    private final SoftAssertions assertions = new SoftAssertions();

    @Before
    public void setUp() {
        when(geoInfo.getLocation(REGION_ID)).thenReturn(LocationSteps.getLocation(REGION_ID));
        when(geoInfo.getLocation(DEFAULT_REGION_ID)).thenReturn(LocationSteps.getLocation(DEFAULT_REGION_ID));
    }

    @After
    public void tearDown() {
        assertions.assertAll();
    }

    @Test
    public void fetchByOrder() {
        Order order = OrderSteps.getFilledOrder();
        order.getDelivery().setRegionId((long) REGION_ID);

        Location location = locationFetcher.fetch(order);

        verify(geoInfo).getLocation(REGION_ID);
        assertions.assertThat(location)
            .as("Location in fetchByOrder")
            .isEqualToComparingFieldByFieldRecursively(LocationSteps.getLocation(REGION_ID));
    }

    @Test
    public void fetchByShop() {
        Shop shop = ShopSteps.getDefaultShop(1, (long) REGION_ID);

        Location location = locationFetcher.fetch(shop);

        verify(geoInfo).getLocation(REGION_ID);
        assertions.assertThat(location)
            .as("Location in fetchByShop")
            .isEqualToComparingFieldByFieldRecursively(LocationSteps.getLocation(REGION_ID));
    }

    @Test
    public void fetchNullableByShop() {
        Shop shop = ShopSteps.getDefaultShop(1, null);

        Location location = locationFetcher.fetchNullable(shop);

        verifyNoMoreInteractions(geoInfo);

        assertions.assertThat(location)
            .as("Location in fetchNullableByShop")
            .isNull();
    }

    @Test
    public void fetchByInlet() {
        LogisticsPoint inlet = LogisticPointSteps.getDefaultOutlet((long) REGION_ID);

        Location location = locationFetcher.fetch(inlet);

        verify(geoInfo).getLocation(REGION_ID);
        assertions.assertThat(location)
            .as("Location in fetchByInlet")
            .isEqualToComparingFieldByFieldRecursively(LocationSteps.getLocation(REGION_ID));
    }

    @Test
    public void fetchByInletNull() {
        LogisticsPoint inlet = null;

        Location location = locationFetcher.fetch(inlet);

        verify(geoInfo).getLocation(DEFAULT_REGION_ID);
        assertions.assertThat(location)
            .as("Location in fetchByInletNull")
            .isEqualToComparingFieldByFieldRecursively(LocationSteps.getLocation(DEFAULT_REGION_ID));
    }

    @Test
    public void fetchByRegionId() {
        Location location = locationFetcher.fetch(REGION_ID);

        verify(geoInfo).getLocation(REGION_ID);
        assertions.assertThat(location)
            .as("Location in fetchByInlet")
            .isEqualToComparingFieldByFieldRecursively(LocationSteps.getLocation(REGION_ID));
    }

    @Test(expected = EnrichmentFailException.class)
    public void fetchByRegionIdNull() {
        Integer regionId = null;

        locationFetcher.fetch(regionId);
    }
}
