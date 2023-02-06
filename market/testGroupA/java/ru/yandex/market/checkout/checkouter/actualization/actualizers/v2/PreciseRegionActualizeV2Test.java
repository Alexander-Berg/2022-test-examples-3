package ru.yandex.market.checkout.checkouter.actualization.actualizers.v2;

import java.util.Collections;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.common.CollectionFeatureType;
import ru.yandex.market.checkout.checkouter.geo.GeoPoint;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;
import static ru.yandex.market.checkout.util.geocoder.GeocoderParameters.DEFAULT_GPS;

public class PreciseRegionActualizeV2Test extends AbstractWebTestBase {


    public static final Long PRECISE_REGION_ID = 116978L;

    @Autowired
    private WireMockServer geobaseMock;

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @BeforeEach
    public void before() {
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndGps();
    }

    @Test
    public void shouldPreciseRegionInActualDelivery() {
        mockGeobase(PRECISE_REGION_ID.toString());
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .buildParameters();

        checkouterFeatureWriter.writeValue(BooleanFeatureType.PRECISE_REGION_ENABLED, true);
        checkouterFeatureWriter.writeValue(CollectionFeatureType.REGIONS_LIST_SUPPORTING_PRECISION,
                Collections.emptySet());
        orderCreateHelper.multiCartActualize(parameters);
        geobaseMock.verify(1, WireMock.getRequestedFor(urlPathMatching("/v1/region_id_by_location")));
        assertRidsInReportRequests(PRECISE_REGION_ID);
    }

    @Test
    public void shouldPreciseRegionFromRequestInActualDelivery() {
        Long expectedPreciseRegionId = 123123L;
        mockGeobase(PRECISE_REGION_ID.toString());
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .buildParameters();
        checkouterFeatureWriter.writeValue(BooleanFeatureType.PRECISE_REGION_ENABLED, true);
        checkouterFeatureWriter.writeValue(CollectionFeatureType.REGIONS_LIST_SUPPORTING_PRECISION,
                Collections.emptySet());
        AddressImpl buyerAddress =
                (AddressImpl) parameters.getReportParameters().getOrder().getDelivery().getBuyerAddress();
        buyerAddress.setPreciseRegionId(expectedPreciseRegionId);
        orderCreateHelper.multiCartActualize(parameters);
        geobaseMock.verify(0, WireMock.getRequestedFor(urlPathMatching("/v1/region_id_by_location")));
        assertRidsInReportRequests(expectedPreciseRegionId);
    }

    @Test
    public void shouldRegionInActualDelivery() {
        mockGeobase(null);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .buildParameters();

        checkouterFeatureWriter.writeValue(BooleanFeatureType.PRECISE_REGION_ENABLED, true);
        checkouterFeatureWriter.writeValue(CollectionFeatureType.REGIONS_LIST_SUPPORTING_PRECISION,
                Collections.emptySet());
        orderCreateHelper.multiCartActualize(parameters);
        geobaseMock.verify(1, WireMock.getRequestedFor(urlPathMatching("/v1/region_id_by_location")));
        assertRidsInReportRequests(parameters.getOrder().getBuyer().getRegionId());
    }

    private void mockGeobase(String regionId) {
        GeoPoint defaultGeoPoint = GeoPoint.forGpsString(DEFAULT_GPS.replace(" ", ","));
        geobaseMock.stubFor(WireMock.get(urlPathEqualTo("/v1/region_id_by_location"))
                .withQueryParam("lat", WireMock.equalTo(Double.toString(defaultGeoPoint.getLatitude())))
                .withQueryParam("lon", WireMock.equalTo(Double.toString(defaultGeoPoint.getLongitude())))
                .willReturn(WireMock.okJson(regionId)));
    }

    private void assertRidsInReportRequests(Long regionId) {
        List<LoggedRequest> loggedRequests = getActualDeliveryRequests();
        loggedRequests.forEach(r -> {
            log.info(r.toString());
            assertTrue(r.getQueryParams().get("rids").values().equals(Collections.singletonList(regionId.toString())));
        });
    }

    private List<LoggedRequest> getActualDeliveryRequests() {
        return reportMock.findAll(
                getRequestedFor(anyUrl())
                        .withQueryParam("place", equalTo("actual_delivery"))
        );
    }
}
