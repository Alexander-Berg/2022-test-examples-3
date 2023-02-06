package ru.yandex.market.delivery.mdbapp.integration.converter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import steps.ParcelSteps;
import steps.orderSteps.OrderEventSteps;
import steps.orderSteps.OrderSteps;
import steps.orderSteps.itemSteps.ItemsSteps;

import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.delivery.mdbapp.components.service.lms.LmsLogisticsPointClient;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.OrderToShip;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.OrderToShipValue;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.CapacityCountingType;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.OrderToShipStatus;
import ru.yandex.market.delivery.mdbclient.model.dto.CapacityServiceType;
import ru.yandex.market.delivery.mdbclient.model.dto.DeliveryType;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrderToShipConverterTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Mock
    private LmsLogisticsPointClient lmsLogisticsPointClient;

    private OrderToShipConverter orderToShipConverter;

    static final Long MK_NOVGOROD = 1005546L;
    static final Long MK_EAST = 1005492L;

    @Before
    public void before() {
        orderToShipConverter = new OrderToShipConverter(lmsLogisticsPointClient);
    }

    @Test
    public void testAllParcelsConverted() {
        when(lmsLogisticsPointClient.getActiveWarehousesByPartnerId(any()))
            .thenReturn(preparePartnerResponse(getAddress(100)));

        OrderHistoryEvent event = prepareEvent();
        List<Parcel> parcels = Arrays.asList(
            getParcel(1L, 1L),
            getParcel(2L, 1L)
        );
        event.getOrderAfter().getDelivery().getParcels().addAll(parcels);
        List<OrderToShip> orderToShips = orderToShipConverter.convertEventToOrdersToShip(event);
        softly.assertThat(orderToShips).containsExactlyInAnyOrder(
            prepareOrderToShip(987L, "1", LocalDate.of(2019, 1, 2), CapacityServiceType.DELIVERY, 100L, 213L),
            prepareOrderToShip(100500L, "1", LocalDate.of(2019, 1, 2), CapacityServiceType.SHIPMENT, 100L, 213L),
            prepareOrderToShip(987L, "2", LocalDate.of(2019, 1, 2), CapacityServiceType.DELIVERY, 100L, 213L),
            prepareOrderToShip(100500L, "2", LocalDate.of(2019, 1, 2), CapacityServiceType.SHIPMENT, 100L, 213L)
        );
        assertOrderToShipValues(orderToShips);
        verify(lmsLogisticsPointClient, times(1)).getActiveWarehousesByPartnerId(any());
    }

    @Test
    public void testFfParcelConverted() {
        when(lmsLogisticsPointClient.getActiveWarehousesByPartnerId(any()))
            .thenReturn(preparePartnerResponse(getAddress(100)));

        OrderHistoryEvent event = prepareEvent();
        Parcel parcel = getParcel(123L, 1L);
        event.getOrderAfter().getDelivery().setParcels(Collections.singletonList(parcel));
        List<OrderToShip> orderToShips = orderToShipConverter.convertEventToOrdersToShip(event);
        softly.assertThat(orderToShips).containsExactlyInAnyOrder(
            prepareOrderToShip(100500L, "123", LocalDate.of(2019, 1, 2), CapacityServiceType.SHIPMENT, 100L, 213L),
            prepareOrderToShip(987L, "123", LocalDate.of(2019, 1, 2), CapacityServiceType.DELIVERY, 100L, 213L)
        );
        assertOrderToShipValues(orderToShips);
        verify(lmsLogisticsPointClient, times(1)).getActiveWarehousesByPartnerId(any());
    }

    @Test
    public void testDsParcelConverted() {
        when(lmsLogisticsPointClient.getActiveWarehousesByPartnerId(any()))
            .thenReturn(preparePartnerResponse(getAddress(100)));

        OrderHistoryEvent event = prepareEvent();
        Parcel parcel = getParcel(987L, 1L);
        event.getOrderAfter().getDelivery().getParcels().add(parcel);
        List<OrderToShip> orderToShips = orderToShipConverter.convertEventToOrdersToShip(event);
        softly.assertThat(orderToShips).containsExactlyInAnyOrder(
            prepareOrderToShip(987L, "987", LocalDate.of(2019, 1, 2), CapacityServiceType.DELIVERY, 100L, 213L),
            prepareOrderToShip(100500L, "987", LocalDate.of(2019, 1, 2), CapacityServiceType.SHIPMENT, 100L, 213L)
        );
        assertOrderToShipValues(orderToShips);
        verify(lmsLogisticsPointClient, times(1)).getActiveWarehousesByPartnerId(any());
    }

    @Test
    public void testNoParcels() {
        OrderHistoryEvent event = prepareEvent();
        List<OrderToShip> orderToShips = orderToShipConverter.convertEventToOrdersToShip(event);
        softly.assertThat(orderToShips).isEmpty();
        verify(lmsLogisticsPointClient, never()).getActiveWarehousesByPartnerId(any());
    }

    @Test
    public void testCrossdockParcelsConverted() {

        when(lmsLogisticsPointClient.getActiveWarehousesByPartnerId(100501L))
            .thenReturn(preparePartnerResponse(getAddress(101)));
        when(lmsLogisticsPointClient.getActiveWarehousesByPartnerId(1001L))
            .thenReturn(preparePartnerResponse(getAddress(214)));

        LocalDate shipmentDate = LocalDate.of(2020, 3, 4);
        LocalDate supplierShipmentDate = LocalDate.of(2020, 3, 2);
        LocalDate inboundDate = LocalDate.of(2020, 3, 3);
        LocalDate anyJunkDate = LocalDate.of(2059, 12, 1);

        OrderHistoryEvent event = prepareEvent();
        List<Parcel> parcels = Stream.of(1L, 2L).map(id -> {
            Parcel parcel = getParcel(id, id);
            long offset = id - 1;
            parcel.setShipmentDate(shipmentDate.plusDays(offset));
            parcel.setShipmentDateTimeBySupplier(anyJunkDate.atStartOfDay());

            ParcelItem parcelItem = parcel.getParcelItems().get(0);
            parcelItem.setShipmentDateTimeBySupplier(anyJunkDate.plusDays(offset).atStartOfDay());
            parcelItem.setReceptionDateTimeByWarehouse(inboundDate.atStartOfDay());
            parcelItem.setSupplierStartDateTime(toInstant(supplierShipmentDate.plusDays(offset)));
            parcelItem.setSupplierShipmentDateTime(toInstant(anyJunkDate));
            return parcel;
        }).collect(Collectors.toList());

        List<OrderItem> items = Stream.of(1L, 2L).map(id -> {
            OrderItem orderItem = ItemsSteps.getOrderItem();
            orderItem.setId(id);
            orderItem.setOfferId(String.valueOf(id));
            orderItem.setAtSupplierWarehouse(true);
            orderItem.setWarehouseId(Math.toIntExact(100501));
            orderItem.setFulfilmentWarehouseId(1001L);
            return orderItem;
        }).collect(Collectors.toList());

        event.getOrderAfter().getDelivery().setParcels(parcels);
        event.getOrderAfter().setItems(items);
        // Это кроссдок
        event.getOrderAfter().setFulfilment(true);
        event.setType(HistoryEventType.NEW_ORDER);

        List<OrderToShip> orderToShips = orderToShipConverter.convertEventToOrdersToShip(event);
        softly.assertThat(orderToShips).containsExactlyInAnyOrder(
            prepareOrderToShip(987L, "1", shipmentDate, CapacityServiceType.DELIVERY, 214L, 213L),
            prepareOrderToShip(1001L, "1", shipmentDate, CapacityServiceType.SHIPMENT, 214L, 213L),
            prepareOrderToShip(1001L, "1", inboundDate, CapacityServiceType.INBOUND, 101L, 214L),
            prepareOrderToShip(100501L, "1", supplierShipmentDate, CapacityServiceType.SHIPMENT, 101L, 214L),
            prepareOrderToShip(987L, "2", shipmentDate.plusDays(1), CapacityServiceType.DELIVERY, 214L, 213L),
            prepareOrderToShip(1001L, "2", shipmentDate.plusDays(1), CapacityServiceType.SHIPMENT, 214L, 213L),
            prepareOrderToShip(1001L, "2", inboundDate, CapacityServiceType.INBOUND, 101L, 214L),
            prepareOrderToShip(
                100501L,
                "2",
                supplierShipmentDate.plusDays(1),
                CapacityServiceType.SHIPMENT,
                101L,
                214L
            )
        );
        assertOrderToShipValues(orderToShips);
    }

    @Test
    public void testDropshipParcelsConverted() {

        when(lmsLogisticsPointClient.getActiveWarehousesByPartnerId(100501L))
            .thenReturn(preparePartnerResponse(getAddress(101)));
        when(lmsLogisticsPointClient.getActiveWarehousesByPartnerId(1001L))
            .thenReturn(preparePartnerResponse(getAddress(214)));

        LocalDate shipmentDate = LocalDate.of(2020, 3, 4);
        LocalDate supplierShipmentDate = LocalDate.of(2020, 3, 2);
        LocalDate inboundDate = LocalDate.of(2020, 3, 3);
        LocalDate anyJunkDate = LocalDate.of(2059, 12, 1);

        OrderHistoryEvent event = prepareEvent();
        List<Parcel> parcels = Stream.of(1L, 2L).map(id -> {
            Parcel parcel = getParcel(id, id);
            long offset = id - 1;
            parcel.setShipmentDate(shipmentDate.plusDays(offset));
            parcel.setShipmentDateTimeBySupplier(anyJunkDate.atStartOfDay());

            ParcelItem parcelItem = parcel.getParcelItems().get(0);
            parcelItem.setShipmentDateTimeBySupplier(supplierShipmentDate.plusDays(offset).atStartOfDay());
            parcelItem.setReceptionDateTimeByWarehouse(inboundDate.atStartOfDay());
            parcelItem.setSupplierStartDateTime(toInstant(anyJunkDate));
            parcelItem.setSupplierShipmentDateTime(toInstant(anyJunkDate));
            return parcel;
        }).collect(Collectors.toList());

        List<OrderItem> items = Stream.of(1L, 2L).map(id -> {
            OrderItem orderItem = ItemsSteps.getOrderItem();
            orderItem.setId(id);
            orderItem.setOfferId(String.valueOf(id));
            orderItem.setAtSupplierWarehouse(true);
            orderItem.setWarehouseId(Math.toIntExact(100501));
            orderItem.setFulfilmentWarehouseId(1001L);
            return orderItem;
        }).collect(Collectors.toList());

        event.getOrderAfter().getDelivery().setParcels(parcels);
        event.getOrderAfter().setItems(items);
        // Это дропшип
        event.getOrderAfter().setFulfilment(false);
        event.setType(HistoryEventType.NEW_ORDER);

        List<OrderToShip> orderToShips = orderToShipConverter.convertEventToOrdersToShip(event);
        softly.assertThat(orderToShips).containsExactlyInAnyOrder(
            prepareOrderToShip(987L, "1", shipmentDate, CapacityServiceType.DELIVERY, 214L, 213L),
            prepareOrderToShip(1001L, "1", shipmentDate, CapacityServiceType.SHIPMENT, 214L, 213L),
            prepareOrderToShip(1001L, "1", inboundDate, CapacityServiceType.INBOUND, 101L, 214L),
            prepareOrderToShip(100501L, "1", supplierShipmentDate, CapacityServiceType.SHIPMENT, 101L, 214L),
            prepareOrderToShip(987L, "2", shipmentDate.plusDays(1), CapacityServiceType.DELIVERY, 214L, 213L),
            prepareOrderToShip(1001L, "2", shipmentDate.plusDays(1), CapacityServiceType.SHIPMENT, 214L, 213L),
            prepareOrderToShip(1001L, "2", inboundDate, CapacityServiceType.INBOUND, 101L, 214L),
            prepareOrderToShip(
                100501L,
                "2",
                supplierShipmentDate.plusDays(1),
                CapacityServiceType.SHIPMENT,
                101L,
                214L
            )
        );
        assertOrderToShipValues(orderToShips);
    }

    @Test
    public void testManyCrossdockSuppliers() {
        when(lmsLogisticsPointClient.getActiveWarehousesByPartnerId(100501L))
            .thenReturn(preparePartnerResponse(getAddress(101)));
        when(lmsLogisticsPointClient.getActiveWarehousesByPartnerId(100502L))
            .thenReturn(preparePartnerResponse(getAddress(102)));
        when(lmsLogisticsPointClient.getActiveWarehousesByPartnerId(100503L))
            .thenReturn(preparePartnerResponse(getAddress(103)));
        when(lmsLogisticsPointClient.getActiveWarehousesByPartnerId(1001L))
            .thenReturn(preparePartnerResponse(getAddress(214)));

        OrderHistoryEvent event = prepareEvent();

        OrderItem orderItem1 = prepareOrderItem(1L, 100501, 1001);
        OrderItem orderItem2 = prepareOrderItem(2L, 100502, 1001);
        OrderItem orderItem3 = prepareOrderItem(3L, 100503, 1001);
        OrderItem orderItem4 = prepareOrderItem(4L, 1001, 1001);
        Parcel parcel = getParcel(1L, 1L);

        LocalDate shipmentDate = LocalDate.of(2020, 3, 12);
        LocalDate supplierShipmentDate = LocalDate.of(2020, 3, 1);
        LocalDate inboundDate = LocalDate.of(2020, 3, 8);
        LocalDate anyJunkDate = LocalDate.of(2059, 12, 1);

        parcel.setShipmentDate(shipmentDate);

        parcel.setParcelItems(Stream.of(orderItem1, orderItem2, orderItem3, orderItem4)
            .map(ParcelItem::new)
            .peek(parcelItem -> {
                Long offset = parcelItem.getItemId();
                parcelItem.setShipmentDateTimeBySupplier(supplierShipmentDate.plusDays(offset).atStartOfDay());
                parcelItem.setReceptionDateTimeByWarehouse(inboundDate.plusDays(offset).atStartOfDay());
                parcelItem.setSupplierShipmentDateTime(toInstant(anyJunkDate));
                parcelItem.setSupplierStartDateTime(toInstant(supplierShipmentDate.plusDays(offset)));
            })
            .collect(Collectors.toList()));

        event.getOrderAfter().setItems(Stream.of(orderItem1, orderItem2, orderItem3, orderItem4)
            .collect(Collectors.toList()));
        event.getOrderAfter().getDelivery().setParcels(Collections.singletonList(parcel));
        event.getOrderAfter().setFulfilment(true);
        event.setType(HistoryEventType.NEW_ORDER);

        List<OrderToShip> orderToShips = orderToShipConverter.convertEventToOrdersToShip(event);
        softly.assertThat(orderToShips).containsExactlyInAnyOrder(
            prepareOrderToShip(987L, "1", shipmentDate, CapacityServiceType.DELIVERY, 214L, 213L),
            prepareOrderToShip(1001L, "1", shipmentDate, CapacityServiceType.SHIPMENT, 214L, 213L),
            prepareOrderToShip(1001L, "1", inboundDate.plusDays(1), CapacityServiceType.INBOUND, 101L, 214L),
            prepareOrderToShip(1001L, "1", inboundDate.plusDays(2), CapacityServiceType.INBOUND, 102, 214L),
            prepareOrderToShip(1001L, "1", inboundDate.plusDays(3), CapacityServiceType.INBOUND, 103L, 214L),
            prepareOrderToShip(100501L, "1",
                supplierShipmentDate.plusDays(1), CapacityServiceType.SHIPMENT, 101L, 214L
            ),
            prepareOrderToShip(100502L, "1",
                supplierShipmentDate.plusDays(2), CapacityServiceType.SHIPMENT, 102, 214L
            ),
            prepareOrderToShip(100503L, "1",
                supplierShipmentDate.plusDays(3), CapacityServiceType.SHIPMENT, 103L, 214L
            )
        );

        Map<Long, Long> expectedCountMap = new HashMap<>();
        expectedCountMap.put(987L, 8L);
        expectedCountMap.put(1001L, 14L);
        expectedCountMap.put(100501L, 2L);
        expectedCountMap.put(100502L, 2L);
        expectedCountMap.put(100503L, 2L);

        Map<Long, Long> countMap = orderToShips.stream()
            .map(OrderToShip::getOrderToShipValues)
            .flatMap(Collection::stream)
            .filter(orderToShipValue -> CapacityCountingType.ITEM.equals(orderToShipValue.getCountingType()))
            .collect(Collectors.toMap(orderToShipValue ->
                    orderToShipValue.getOrderToShip().getId().getPartnerId(),
                OrderToShipValue::getValue, Long::sum
            ));

        softly.assertThat(expectedCountMap).isEqualTo(countMap);

        verify(lmsLogisticsPointClient, times(1)).getActiveWarehousesByPartnerId(1001L);
        verify(lmsLogisticsPointClient, times(1)).getActiveWarehousesByPartnerId(100501L);
        verify(lmsLogisticsPointClient, times(1)).getActiveWarehousesByPartnerId(100502L);
        verify(lmsLogisticsPointClient, times(1)).getActiveWarehousesByPartnerId(100503L);
    }

    private OrderHistoryEvent prepareEvent() {
        OrderHistoryEvent orderHistoryEvent = OrderEventSteps.getOrderHistoryEvent();
        orderHistoryEvent.setOrderBefore(OrderSteps.getFilledOrder());
        orderHistoryEvent.getOrderAfter().setRgb(Color.BLUE);
        orderHistoryEvent.getOrderAfter().getDelivery().getParcels().clear();
        orderHistoryEvent.getOrderAfter().getItems().forEach(orderItem -> {
            orderItem.setFulfilmentWarehouseId(100500L);
            orderItem.setWarehouseId(100500);
        });

        return orderHistoryEvent;
    }

    private List<LogisticsPointResponse> preparePartnerResponse(Address address) {
        return Collections.singletonList(
            LogisticsPointResponse.newBuilder()
                .address(address)
                .active(true)
                .build()
        );
    }

    private Instant toInstant(LocalDate localDate) {
        return localDate.atStartOfDay(DateTimeUtils.MOSCOW_ZONE).toInstant();
    }

    private Address getAddress(Integer locationId) {
        return Address.newBuilder()
            .locationId(locationId)
            .settlement("Москва")
            .postCode("555666")
            .latitude(new BigDecimal("100"))
            .longitude(new BigDecimal("200"))
            .street("Октябрьская")
            .house("5")
            .housing("3")
            .building("2")
            .apartment("1")
            .comment("comment")
            .region("region")
            .addressString("Строка адреса")
            .shortAddressString("Строка адреса")
            .build();
    }

    private Parcel getParcel(Long parcelId, Long itemId) {
        Parcel parcel = ParcelSteps.getParcel();
        parcel.setId(parcelId);
        parcel.setShipmentDateTimeBySupplier(LocalDate.of(2019, 1, 1).atStartOfDay());
        parcel.setShipmentDate(LocalDate.of(2019, 1, 2));
        OrderItem orderItem = ItemsSteps.getOrderItem();
        orderItem.setId(itemId);
        ParcelItem parcelItem = new ParcelItem(orderItem);
        parcelItem.setSupplierStartDateTime(toInstant(LocalDate.of(2018, 12, 30)));
        parcelItem.setShipmentDateTimeBySupplier(LocalDate.of(2018, 12, 31).atStartOfDay());
        parcel.setParcelItems(Collections.singletonList(parcelItem));
        return parcel;
    }

    private OrderItem prepareOrderItem(long itemId, int ffId, int whId) {

        OrderItem orderItem = ItemsSteps.getOrderItem();
        orderItem.setWarehouseId(ffId);
        orderItem.setFulfilmentWarehouseId((long) whId);
        orderItem.setAtSupplierWarehouse(ffId != whId);
        orderItem.setOfferId(String.valueOf(itemId));
        orderItem.setId(itemId);
        return orderItem;
    }

    private OrderToShip prepareOrderToShip(
        Long partnerId,
        String parcelId,
        LocalDate shipmentDay,
        CapacityServiceType serviceType,
        long locationFromId,
        long locationToId
    ) {
        return prepareOrderToShip(
            partnerId,
            parcelId,
            shipmentDay,
            serviceType,
            locationFromId,
            locationToId,
            DeliveryType.DELIVERY
        );
    }

    private OrderToShip prepareOrderToShip(
        Long partnerId,
        String parcelId,
        LocalDate shipmentDay,
        CapacityServiceType serviceType,
        long locationFromId,
        long locationToId,
        DeliveryType deliveryType
    ) {
        OrderToShip orderToShip = new OrderToShip()
            .setId(OrderToShipConverter.toOrderToShipId(
                parcelId, 1L, partnerId, serviceType, OrderToShipStatus.CREATED))
            .setLocationFromId(locationFromId)
            .setLocationToId(locationToId)
            .setDeliveryType(deliveryType)
            .setShipmentDay(shipmentDay)
            .setProcessed(false);

        Stream.of(
            new OrderToShipValue().setCountingType(CapacityCountingType.ORDER).setValue(1L),
            new OrderToShipValue().setCountingType(CapacityCountingType.ITEM).setValue(2L)
        ).forEach(orderToShip::addOrderToShipValue);

        return orderToShip;
    }

    private void assertOrderToShipValues(List<OrderToShip> orderToShips) {
        assertOrderToShipValuesListAreNotEmpty(orderToShips, CapacityCountingType.ORDER);
        assertOrderToShipValuesListAreNotEmpty(orderToShips, CapacityCountingType.ITEM);

        assertOrderToShipValuesForGivenCountingType(orderToShips, CapacityCountingType.ORDER, 1L);
        assertOrderToShipValuesForGivenCountingType(orderToShips, CapacityCountingType.ITEM, 2L);
    }

    private void assertOrderToShipValuesListAreNotEmpty(
        List<OrderToShip> orderToShips,
        CapacityCountingType countingType
    ) {
        softly.assertThat(orderToShips)
            .flatExtracting(orderToShip -> orderToShip.getOrderToShipValues().stream()
                .filter(orderToShipValue -> orderToShipValue.getCountingType() == countingType)
                .map(OrderToShipValue::getValue)
                .collect(Collectors.toList()))
            .isNotEmpty();
    }

    private void assertOrderToShipValuesForGivenCountingType(
        List<OrderToShip> orderToShips,
        CapacityCountingType countingType,
        Long... expectedValues
    ) {
        softly.assertThat(orderToShips)
            .flatExtracting(orderToShip -> orderToShip.getOrderToShipValues().stream()
                .filter(orderToShipValue -> orderToShipValue.getCountingType() == countingType)
                .map(OrderToShipValue::getValue)
                .collect(Collectors.toList()))
            .containsOnly(expectedValues);
    }
}
