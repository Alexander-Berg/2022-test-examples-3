package ru.yandex.market.delivery.mdbapp.integration.enricher;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import steps.PartnerInfoSteps;
import steps.logisticsPointSteps.LogisticPointSteps;
import steps.orderSteps.OrderSteps;
import steps.shopSteps.ShopSteps;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.components.geo.Location;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fetcher.LocationFetcher;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fetcher.ShopFetcher;
import ru.yandex.market.delivery.mdbapp.integration.payload.ExtendedOrder;
import ru.yandex.market.delivery.mdbapp.integration.payload.ReturnInletData;
import ru.yandex.market.mbi.api.client.entity.shops.Shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(MockitoJUnitRunner.class)
@DisplayName("Обогащение dropship by seller заказа данными")
class DbsOrderExternalEnricherTest {

    private DbsOrderExternalEnricher dbsOrderExternalEnricher;

    @Mock
    private LocationFetcher locationFetcher;

    @Mock
    private ShopFetcher shopFetcher;

    private static final Order ORDER = OrderSteps.getFilledOrder();
    private static final Shop SHOP = ShopSteps.getDefaultShop();
    private static final Location LOCATION_TO = new Location().setCountry("to");
    private static final Location LOCATION_FROM = new Location().setCountry("from");

    @BeforeEach
    void setUp() {
        initMocks(this);
        dbsOrderExternalEnricher = new DbsOrderExternalEnricher(shopFetcher, locationFetcher);
        doReturn(SHOP)
            .when(shopFetcher).fetch(ORDER);
        doReturn(LOCATION_TO)
            .when(locationFetcher).fetch(ORDER);
        doReturn(LOCATION_FROM)
            .when(locationFetcher).fetch(SHOP);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(shopFetcher, locationFetcher);
    }

    @Test
    @DisplayName("Корректное обогащение dropship by seller заказа")
    void enrichOrder() {
        assertThat(dbsOrderExternalEnricher.enrich(extendedOrder()))
            .usingRecursiveComparison()
            .isEqualTo(expectedEnrichedOrder());

        verify(shopFetcher, times(2)).fetch(ORDER);
        verify(locationFetcher).fetch(SHOP);
        verify(locationFetcher).fetch(ORDER);
    }

    @Nonnull
    private ExtendedOrder extendedOrder() {
        return new ExtendedOrder()
            .setOrder(ORDER)
            .setOrderData(new ExtendedOrder.OrderData())
            .setInlet(LogisticPointSteps.getDefaultInlet())
            .setOutlet(LogisticPointSteps.getDefaultOutlet())
            .setPartnerInfo(PartnerInfoSteps.getPartnerInfoDTO(1L))
            .setReturnInletData(new ReturnInletData());
    }

    @Nonnull
    private ExtendedOrder expectedEnrichedOrder() {
        return extendedOrder()
            .setInlet(null)
            .setOutlet(null)
            .setReturnInletData(null)
            .setShop(ShopSteps.getDefaultShop())
            .setOrderData(new ExtendedOrder.OrderData()
                .setLocationTo(LOCATION_TO)
                .setLocationFrom(LOCATION_FROM));
    }

}
