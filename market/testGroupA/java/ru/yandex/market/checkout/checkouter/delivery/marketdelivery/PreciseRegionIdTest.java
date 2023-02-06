package ru.yandex.market.checkout.checkouter.delivery.marketdelivery;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.common.CollectionFeatureType;
import ru.yandex.market.checkout.checkouter.geo.GeoPoint;
import ru.yandex.market.checkout.checkouter.order.DeliveryEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;
import static ru.yandex.market.checkout.util.geocoder.GeocoderParameters.DEFAULT_GPS;

/**
 * @author mmetlov
 */
public class PreciseRegionIdTest extends AbstractWebTestBase {

    public static final Long PRECISE_REGION_ID = 116978L;

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private WireMockServer geobaseMock;

    @BeforeEach
    public void before() {
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndEmptyGps();
    }

    @AfterEach
    public void destroy() {
        geobaseMock.resetAll();
    }

    @Test
    public void shouldUsePreciseRegionIdWhenGeobaseAnsweredInTime() throws Exception {
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

        Order order = orderCreateHelper.createOrder(parameters);

        geobaseMock.verify(2, WireMock.getRequestedFor(urlPathMatching("/v1/region_id_by_location")));
        assertRidsInReportRequests(PRECISE_REGION_ID);

        assertEquals(PRECISE_REGION_ID, order.getDelivery().getBuyerAddress().getPreciseRegionId());

        Order orderFromDb = orderService.getOrder(order.getId());

        assertEquals(PRECISE_REGION_ID, orderFromDb.getDelivery().getBuyerAddress().getPreciseRegionId());

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        OrderEditOptionsRequest orderEditOptionsRequest = new OrderEditOptionsRequest();
        orderEditOptionsRequest.setDeliveryEditOptionsRequest(
                DeliveryEditOptionsRequest.newDeliveryChangePrerequest()
                        .shipmentDate(LocalDate.now().minusDays(1))
                        .build());

        reportMock.resetRequests();
        client.getOrderEditOptions(
                order.getId(), ClientRole.SYSTEM, null, Collections.singletonList(BLUE), orderEditOptionsRequest);
        assertRidsInReportRequests(PRECISE_REGION_ID);
    }

    private void mockGeobase(Integer delay) {
        GeoPoint defaultGeoPoint = GeoPoint.forGpsString(DEFAULT_GPS.replace(" ", ","));
        geobaseMock.stubFor(WireMock.get(urlPathEqualTo("/v1/region_id_by_location"))
                .withQueryParam("lat", WireMock.equalTo(Double.toString(defaultGeoPoint.getLatitude())))
                .withQueryParam("lon", WireMock.equalTo(Double.toString(defaultGeoPoint.getLongitude())))
                .willReturn(WireMock.okJson(PRECISE_REGION_ID.toString()).withFixedDelay(delay)));
    }

    @Test
    public void shouldUseRegionIdFromRequestWhenGeobaseUnavailable() {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .buildParameters();

        checkouterFeatureWriter.writeValue(BooleanFeatureType.PRECISE_REGION_ENABLED, true);
        checkouterFeatureWriter.writeValue(CollectionFeatureType.REGIONS_LIST_SUPPORTING_PRECISION,
                Collections.emptySet());

        Order order = orderCreateHelper.createOrder(parameters);

        assertRidsInReportRequests(DeliveryProvider.REGION_ID);
        assertNull(order.getDelivery().getBuyerAddress().getPreciseRegionId());
    }

    @Test
    public void shouldUseRegionIdFromRequestWhenGeobaseAnswersTooLong() {
        mockGeobase(5100);

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .buildParameters();

        checkouterFeatureWriter.writeValue(BooleanFeatureType.PRECISE_REGION_ENABLED, true);
        checkouterFeatureWriter.writeValue(CollectionFeatureType.REGIONS_LIST_SUPPORTING_PRECISION,
                Collections.emptySet());

        Order order = orderCreateHelper.createOrder(parameters);

        assertRidsInReportRequests(DeliveryProvider.REGION_ID);
        assertNull(order.getDelivery().getBuyerAddress().getPreciseRegionId());
    }

    @Test
    public void shouldntCallGeobaseWhenTurnedOff() {
        mockGeobase(null);

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .buildParameters();

        checkouterFeatureWriter.writeValue(BooleanFeatureType.PRECISE_REGION_ENABLED, false);
        checkouterFeatureWriter.writeValue(CollectionFeatureType.REGIONS_LIST_SUPPORTING_PRECISION,
                Collections.emptySet());

        Order order = orderCreateHelper.createOrder(parameters);

        geobaseMock.verify(0, WireMock.getRequestedFor(urlPathMatching("/v1/region_id_by_location")));
        assertRidsInReportRequests(DeliveryProvider.REGION_ID);
        assertNull(order.getDelivery().getBuyerAddress().getPreciseRegionId());
    }

    @Test
    public void shouldntCallGeobaseWhenAvailableForDifferentRegionId() {
        mockGeobase(null);

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .buildParameters();

        checkouterFeatureWriter.writeValue(BooleanFeatureType.PRECISE_REGION_ENABLED, true);
        checkouterFeatureWriter.writeValue(CollectionFeatureType.REGIONS_LIST_SUPPORTING_PRECISION,
                Collections.singleton(2L));

        Order order = orderCreateHelper.createOrder(parameters);

        geobaseMock.verify(0, WireMock.getRequestedFor(urlPathMatching("/v1/region_id_by_location")));
        assertRidsInReportRequests(DeliveryProvider.REGION_ID);
        assertNull(order.getDelivery().getBuyerAddress().getPreciseRegionId());
    }

    @Test
    public void shouldCallGeobaseWhenAvailableForRegionId() {
        mockGeobase(null);

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .buildParameters();

        checkouterFeatureWriter.writeValue(BooleanFeatureType.PRECISE_REGION_ENABLED, true);
        checkouterFeatureWriter.writeValue(CollectionFeatureType.REGIONS_LIST_SUPPORTING_PRECISION,
                Collections.singleton(DeliveryProvider.REGION_ID));

        Order order = orderCreateHelper.createOrder(parameters);

        geobaseMock.verify(2, WireMock.getRequestedFor(urlPathMatching("/v1/region_id_by_location")));
        assertRidsInReportRequests(PRECISE_REGION_ID);
        assertEquals(PRECISE_REGION_ID, order.getDelivery().getBuyerAddress().getPreciseRegionId());
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
