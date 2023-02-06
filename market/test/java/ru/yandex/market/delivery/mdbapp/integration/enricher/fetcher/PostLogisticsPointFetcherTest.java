package ru.yandex.market.delivery.mdbapp.integration.enricher.fetcher;

import java.util.Optional;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import steps.logisticsPointSteps.LogisticPointSteps;
import steps.orderSteps.OrderSteps;
import steps.outletSteps.OutletSteps;
import steps.shopOutletSteps.ShopOutletSteps;

import ru.yandex.market.api.pager.Pager;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.core.backa.persist.addr.Coordinates;
import ru.yandex.market.core.outlet.GeoInfo;
import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.core.outlet.OutletType;
import ru.yandex.market.delivery.mdbapp.integration.converter.LmsWarehouseToInletConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.OutletToLogisticsPointConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.mbi.MbiAddressToAddressConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.mbi.MbiGeoInfoToGeoInfoConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.mbi.MbiPhoneNumberToPhoneNumberConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.mbi.MbiScheduleLinesToScheduleLinesConverter;
import ru.yandex.market.delivery.mdbapp.integration.enricher.EnrichmentFailException;
import ru.yandex.market.delivery.mdbapp.integration.payload.LogisticsPoint;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.outlets.Outlet;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.OutletInfoDTO;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.PagedOutletsDTO;

public class PostLogisticsPointFetcherTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private MbiApiClient mbiApiClient;

    @Mock
    private LMSClient lmsClient;

    @Mock
    private LmsWarehouseToInletConverter lmsConverter;

    private OutletToLogisticsPointConverter outletConverter;

    private SoftAssertions assertions;

    private PostLogisticsPointFetcher postLogisticsPointFetcher;

    private static final String OUTLET_INFO_NAME = "test";
    private static final String GEO_INFO_COORDS = "31.34634,76.3457";
    private static final Long GEO_INFO_REGION = 123L;

    @Before
    public void setUp() {
        assertions = new SoftAssertions();
        outletConverter = new OutletToLogisticsPointConverter(
            new MbiAddressToAddressConverter(),
            new MbiGeoInfoToGeoInfoConverter(),
            new MbiPhoneNumberToPhoneNumberConverter(),
            new MbiScheduleLinesToScheduleLinesConverter()
        );
        postLogisticsPointFetcher =
            new PostLogisticsPointFetcher(mbiApiClient, lmsClient, lmsConverter, outletConverter);
    }

    @After
    public void tearDown() {
        assertions.assertAll();
    }

    @Test
    public void fetchFromMbi() {
        Order order = OrderSteps.getFilledOrder();
        Outlet outlet = OutletSteps.getDefaultOutlet();
        ShopOutlet shopOutlet = ShopOutletSteps.getShopOutlet();
        order.getDelivery().setPostOutletId(shopOutlet.getId());
        order.getDelivery().setPostOutlet(shopOutlet);
        Mockito.when(mbiApiClient.getOutlet(Mockito.anyLong(), Mockito.anyBoolean()))
            .thenReturn(outlet);
        assertions.assertThat(postLogisticsPointFetcher.fetch(order)).isEqualTo(outletConverter.convert(outlet));
    }

    @Test
    public void fetchFromLms() {
        Order order = OrderSteps.getFilledOrder();
        LogisticsPointResponse outletFromLms = LogisticPointSteps.outletFromLms();
        LogisticsPoint logisticsPoint = LogisticPointSteps.getDefaultOutlet();
        ShopOutlet shopOutlet = ShopOutletSteps.getShopOutlet();
        order.getDelivery().setPostOutletId(shopOutlet.getId());

        Mockito.when(lmsClient.getLogisticsPoint(Mockito.anyLong())).thenReturn(Optional.of(outletFromLms));
        Mockito.when(lmsConverter.convert(outletFromLms)).thenReturn(logisticsPoint);

        assertions.assertThat(postLogisticsPointFetcher.fetch(order)).isEqualTo(logisticsPoint);
    }

    @Test
    public void fetchPostOutletIdNull() {
        Order order = OrderSteps.getFilledOrder();
        LogisticsPoint outlet = LogisticPointSteps.getDefaultOutlet();
        ShopOutlet shopOutlet = ShopOutletSteps.getShopOutlet();
//        order.getDelivery().setPostOutletId(shopOutlet.getId());
        order.getDelivery().setPostOutlet(shopOutlet);
        GeoInfo geoInfo = new GeoInfo(Coordinates.valueOf(GEO_INFO_COORDS), GEO_INFO_REGION);

        OutletInfo outletInfo1 = new OutletInfo(1, 2, OutletType.DEPOT, OUTLET_INFO_NAME, true, "123");
        outletInfo1.setGeoInfo(geoInfo);
        OutletInfo outletInfo2 = new OutletInfo(2, 3, OutletType.DEPOT, OUTLET_INFO_NAME, true, "123");
        outletInfo2.setGeoInfo(geoInfo);
        OutletInfoDTO outletInfoDTO1 = new OutletInfoDTO(outletInfo1);
        OutletInfoDTO outletInfoDTO2 = new OutletInfoDTO(outletInfo2);

        PagedOutletsDTO pagedOutletsDTO =
            new PagedOutletsDTO(new Pager(), ImmutableList.of(outletInfoDTO1, outletInfoDTO2));

        Mockito.when(mbiApiClient.getOutletsV2(
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any()
            )
        ).thenReturn(pagedOutletsDTO);
        LogisticsPoint outletReturned = postLogisticsPointFetcher.fetch(order);
        assertions.assertThat(outletReturned).isNotEqualTo(outlet);
        assertions.assertThat(outletReturned.getGeoInfo()).isNotNull();
        assertions.assertThat(outletReturned.getGeoInfo().getGpsCoords()).isEqualTo(GEO_INFO_COORDS);
        assertions.assertThat(outletReturned.getGeoInfo().getRegionId()).isEqualTo(GEO_INFO_REGION);
    }

    @Test(expected = EnrichmentFailException.class)
    public void fetchEmptyPostOutlet() {
        Order order = OrderSteps.getFilledOrder();
        Outlet outlet = OutletSteps.getDefaultOutlet();
        Mockito.when(mbiApiClient.getOutlet(Mockito.anyLong(), Mockito.anyBoolean()))
            .thenReturn(outlet);
        postLogisticsPointFetcher.fetch(order);
    }

    @Test(expected = EnrichmentFailException.class)
    public void fetchOutletNotFound() {
        Order order = OrderSteps.getFilledOrder();
        LogisticsPoint outlet = LogisticPointSteps.getDefaultOutlet();
        ShopOutlet shopOutlet = ShopOutletSteps.getShopOutlet();
        order.getDelivery().setPostOutletId(shopOutlet.getId());
        order.getDelivery().setPostOutlet(shopOutlet);
        Mockito.when(mbiApiClient.getOutlet(Mockito.anyLong(), Mockito.anyBoolean())).thenReturn(null);
        Mockito.when(lmsClient.getLogisticsPoint(Mockito.anyLong())).thenReturn(Optional.empty());
        postLogisticsPointFetcher.fetch(order);
    }
}
