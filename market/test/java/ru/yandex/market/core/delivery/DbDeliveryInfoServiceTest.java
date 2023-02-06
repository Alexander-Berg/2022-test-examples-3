package ru.yandex.market.core.delivery;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты для {@link DbDeliveryInfoService}.
 * <p>
 * Created by stani on 11.07.17.
 */
@DbUnitDataSet(before = "DeliveryDataTest.before.csv")
class DbDeliveryInfoServiceTest extends FunctionalTest {

    @Autowired
    private DeliveryInfoService deliveryInfoService;

    @Test
    void marketMarketDeliveryServiceIdsTest() {
        List<Long> actual = deliveryInfoService.getMarketDeliveryServiceIds();
        assertThat(actual, containsInAnyOrder(106L, 123L, 131L, 132L, 133L, 201L, 203L, 607L, 708L, 908L));
    }

    @Test
    void testListAllDeliveryServicesWithParams() {
        Collection<DeliveryServiceInfo> deliveryServiceInfos = deliveryInfoService.listDeliveryServicesBy(
                DeliveryInfoFilter.builder()
                        .withPickupAvailable(Boolean.TRUE)
                        .withDatasourceId(190104L)
                        .build());
        assertEquals(1, deliveryServiceInfos.size());
        assertEquals(getExpectedServices().get(106L), deliveryServiceInfos.iterator().next());
    }

    private Map<Long, DeliveryServiceInfo> getExpectedServices() {
        Map<Long, DeliveryServiceInfo> services = new HashMap<>();

        DeliveryServiceInfo service = new DeliveryServiceInfo();
        service.setId(106L);
        service.setName("Boxberry");
        service.setType(DeliveryServiceType.CARRIER);
        service.setMarketStatus(DeliveryServiceMarketStatus.ON);
        service.setDateSwitchHour(15);
        service.setPickupAvailable(true);
        service.setRating(1);
        service.setExternalId("bb");
        service.setShipmentType(DeliveryServiceShipmentType.IMPORT);
        service.setAddress("Льва Толстого, Москва, 111111");
        service.setSettlement("Москва");
        services.put(service.getId(), service);

        service = new DeliveryServiceInfo();
        service.setId(123L);
        service.setName("Post");
        service.setType(DeliveryServiceType.CARRIER);
        service.setMarketStatus(DeliveryServiceMarketStatus.PAUSE);
        service.setDateSwitchHour(null);
        services.put(service.getId(), service);

        service = new DeliveryServiceInfo();
        service.setId(131L);
        service.setName("Маршрут");
        service.setType(DeliveryServiceType.SORTING_CENTER);
        service.setMarketStatus(DeliveryServiceMarketStatus.ON);
        service.setDateSwitchHour(null);
        services.put(service.getId(), service);

        service = new DeliveryServiceInfo();
        service.setId(132L);
        service.setName("СПб Парнас");
        service.setType(DeliveryServiceType.SORTING_CENTER);
        service.setMarketStatus(DeliveryServiceMarketStatus.PAUSE);
        service.setDateSwitchHour(null);
        services.put(service.getId(), service);

        service = new DeliveryServiceInfo();
        service.setId(133L);
        service.setName("МОС Огородный");
        service.setType(DeliveryServiceType.SORTING_CENTER);
        service.setMarketStatus(DeliveryServiceMarketStatus.ON);
        service.setDateSwitchHour(null);
        services.put(service.getId(), service);

        service = new DeliveryServiceInfo();
        service.setId(201L);
        service.setName("Many optional");
        service.setType(DeliveryServiceType.CARRIER);
        service.setMarketStatus(DeliveryServiceMarketStatus.ON);
        service.setDateSwitchHour(19);
        service.setRating(2);
        services.put(service.getId(), service);

        service = new DeliveryServiceInfo();
        service.setId(203L);
        service.setName("No optional");
        service.setType(DeliveryServiceType.CARRIER);
        service.setMarketStatus(DeliveryServiceMarketStatus.ON);
        service.setDateSwitchHour(null);
        services.put(service.getId(), service);

        service = new DeliveryServiceInfo();
        service.setId(502L);
        service.setName("Turned off");
        service.setType(DeliveryServiceType.CARRIER);
        service.setMarketStatus(DeliveryServiceMarketStatus.OFF);
        service.setDateSwitchHour(null);
        services.put(service.getId(), service);

        service = new DeliveryServiceInfo();
        service.setId(607L);
        service.setName("Turned off");
        service.setType(DeliveryServiceType.CARRIER);
        service.setMarketStatus(DeliveryServiceMarketStatus.PAUSE);
        service.setDateSwitchHour(null);
        service.setRating(4);
        services.put(service.getId(), service);

        service = new DeliveryServiceInfo();
        service.setId(708L);
        service.setName("Marschroute FF");
        service.setType(DeliveryServiceType.FULFILLMENT);
        service.setMarketStatus(DeliveryServiceMarketStatus.PAUSE);
        service.setDateSwitchHour(null);
        services.put(service.getId(), service);

        service = new DeliveryServiceInfo();
        service.setId(809L);
        service.setName("Rostov FF");
        service.setType(DeliveryServiceType.FULFILLMENT);
        service.setMarketStatus(DeliveryServiceMarketStatus.OFF);
        service.setDateSwitchHour(null);
        services.put(service.getId(), service);

        service = new DeliveryServiceInfo();
        service.setId(908L);
        service.setName("Kazan FF");
        service.setType(DeliveryServiceType.CROSSDOCK);
        service.setMarketStatus(DeliveryServiceMarketStatus.PAUSE);
        service.setDateSwitchHour(null);
        service.setRegionId(56L);
        services.put(service.getId(), service);

        service = new DeliveryServiceInfo();
        service.setId(909L);
        service.setName("Vladimir FF");
        service.setType(DeliveryServiceType.DROPSHIP);
        service.setMarketStatus(DeliveryServiceMarketStatus.OFF);
        service.setDateSwitchHour(null);
        service.setExpress(true);
        service.setRegionId(213L);
        services.put(service.getId(), service);
        return services;
    }

    @Test
    void testAllDeliveryServicesShort() {
        assertThat(deliveryInfoService.listAllDeliveryServicesShort(), containsInAnyOrder(
                samePropertyValuesAs(new DeliveryServiceInfoShort(106, "Boxberry")),
                samePropertyValuesAs(new DeliveryServiceInfoShort(123, "Post")),
                samePropertyValuesAs(new DeliveryServiceInfoShort(131, "Маршрут")),
                samePropertyValuesAs(new DeliveryServiceInfoShort(132, "СПб Парнас")),
                samePropertyValuesAs(new DeliveryServiceInfoShort(133, "МОС Огородный")),
                samePropertyValuesAs(new DeliveryServiceInfoShort(201, "Many optional")),
                samePropertyValuesAs(new DeliveryServiceInfoShort(203, "No optional")),
                samePropertyValuesAs(new DeliveryServiceInfoShort(502, "Turned off")),
                samePropertyValuesAs(new DeliveryServiceInfoShort(607, "Turned off")),
                samePropertyValuesAs(new DeliveryServiceInfoShort(708, "Marschroute FF")),
                samePropertyValuesAs(new DeliveryServiceInfoShort(809, "Rostov FF")),
                samePropertyValuesAs(new DeliveryServiceInfoShort(908L, "Kazan FF")),
                samePropertyValuesAs(new DeliveryServiceInfoShort(909L, "Vladimir FF"))
        ));
    }

    @Test
    void getAvailableFulfillmentServices() {
        final List<DeliveryServiceInfo> services = deliveryInfoService.getAvailableFulfillmentServices(9901);
        assertThat(services, notNullValue());
        assertThat(services, hasSize(2));

        assertThat(services, containsInAnyOrder(
                samePropertyValuesAs(getExpectedServices().get(708L)),
                samePropertyValuesAs(getExpectedServices().get(809L))
        ));

    }

    @Test
    void getAvailableDeliveryServicesCollection() {
        final List<DeliveryServiceInfo> services = deliveryInfoService.getAvailableDeliveryServices(
                Collections.singleton(9901L), Collections.emptyList());
        assertThat(services, notNullValue());
        assertThat(services, hasSize(2));

        assertThat(services, containsInAnyOrder(
                samePropertyValuesAs(getExpectedServices().get(708L)),
                samePropertyValuesAs(getExpectedServices().get(809L))
        ));
    }

    @Test
    void getFulfillmentServices() {
        final List<DeliveryServiceInfo> services = deliveryInfoService.getFulfillmentServices();
        assertThat(services, hasSize(2));
        assertThat(services, containsInAnyOrder(
                samePropertyValuesAs(getExpectedServices().get(708L)),
                samePropertyValuesAs(getExpectedServices().get(809L))
        ));
    }

    @Test
    void getDeliveryServices() {
        final List<DeliveryServiceInfo> services = deliveryInfoService.getDeliveryServicesByTypes(
                Arrays.asList(DeliveryServiceType.FULFILLMENT, DeliveryServiceType.DROPSHIP,
                        DeliveryServiceType.CROSSDOCK));
        assertThat(services, hasSize(4));
        assertThat(services, containsInAnyOrder(
                samePropertyValuesAs(getExpectedServices().get(708L)),
                samePropertyValuesAs(getExpectedServices().get(809L)),
                samePropertyValuesAs(getExpectedServices().get(908L)),
                samePropertyValuesAs(getExpectedServices().get(909L))
        ));
    }

    @Test
    void getDeliveryServicesCollection() {
        final List<DeliveryServiceInfo> services = deliveryInfoService.getAvailableDeliveryServices(
                Collections.singleton(9902L),
                Collections.singleton(DeliveryServiceType.FULFILLMENT));
        assertThat(services, hasSize(1));
        assertEquals(services.get(0), getExpectedServices().get(708L));
    }

    @Test
    void hasExpressTest() {
        assertTrue(deliveryInfoService.hasExpress(9902));
        assertFalse(deliveryInfoService.hasExpress(9901));
    }

    @Test
    void smokeGetBusinessDeliveryServices() {
        assertThat(deliveryInfoService.getBusinessDeliveryServices(
                774L,
                Set.of(),
                false,
                1
        ), hasSize(1));
    }

    @Test
    void getPartnersWithExpressDeliveryServices() {
        var expressPartners = deliveryInfoService.getPartnersWithExpressDeliveryServices(Set.of(9902L, 9901L));
        assertThat(expressPartners, hasSize(1));
        assertThat(expressPartners, containsInAnyOrder(9902L));
    }
}
