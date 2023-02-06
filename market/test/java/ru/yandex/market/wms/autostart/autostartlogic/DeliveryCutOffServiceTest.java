package ru.yandex.market.wms.autostart.autostartlogic;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.TreeMap;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.autostart.AutostartIntegrationTest;
import ru.yandex.market.wms.autostart.autostartlogic.service.DeliveryCutOffService;
import ru.yandex.market.wms.autostart.model.DeliveryOrderData;
import ru.yandex.market.wms.common.spring.dao.entity.DeliveryServiceCutoff;
import ru.yandex.market.wms.common.spring.dao.entity.Order;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeliveryCutOffServiceTest extends AutostartIntegrationTest {

    @Autowired
    private DeliveryCutOffService deliveryCutOffService;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Test
    public void threeOrdersWithDifferentCarriers() {
        Order order1 = mock(Order.class);
        when(order1.getShipmentDateTime()).thenReturn(
                OffsetDateTime.of(LocalDateTime.parse("2021-08-29 23:00:00.000", formatter), ZoneOffset.UTC));
        when(order1.getCarrierCode()).thenReturn("100");

        Order order2 = mock(Order.class);
        when(order2.getShipmentDateTime()).thenReturn(
                OffsetDateTime.of(LocalDateTime.parse("2021-08-29 20:00:00.000", formatter), ZoneOffset.UTC));
        when(order2.getCarrierCode()).thenReturn("200");

        Order order3 = mock(Order.class);
        when(order3.getShipmentDateTime()).thenReturn(
                OffsetDateTime.of(LocalDateTime.parse("2021-08-30 01:00:00.000", formatter), ZoneOffset.UTC));
        when(order3.getCarrierCode()).thenReturn("300");

        List<Order> orders = List.of(order1, order2, order3);

        TreeMap<LocalDateTime, List<DeliveryServiceCutoff>> expected = new TreeMap<>();

        expected.put(LocalDateTime.parse("2021-08-29 20:00:00.000", formatter),
                List.of(DeliveryServiceCutoff.builder()
                        .deliveryServiceCode("200")
                        .shippingCutoff(LocalDateTime.parse("2021-08-29 20:00:00.000", formatter))
                        .warehouseCutoff(LocalDateTime.parse("2021-08-29 16:00:00.000", formatter))
                        .shippingCreationCutOffInHours(null)
                        .warehouseCutoffCutOffInHours(null)
                        .build()));

        expected.put(LocalDateTime.parse("2021-08-29 23:00:00.000", formatter),
                List.of(DeliveryServiceCutoff.builder()
                        .deliveryServiceCode("100")
                        .shippingCutoff(LocalDateTime.parse("2021-08-29 23:00:00.000", formatter))
                        .warehouseCutoff(LocalDateTime.parse("2021-08-29 19:00:00.000", formatter))
                        .shippingCreationCutOffInHours(null)
                        .warehouseCutoffCutOffInHours(null)
                        .build()));

        expected.put(LocalDateTime.parse("2021-08-30 01:00:00.000", formatter),
                List.of(DeliveryServiceCutoff.builder()
                        .deliveryServiceCode("300")
                        .shippingCutoff(LocalDateTime.parse("2021-08-30 01:00:00.000", formatter))
                        .warehouseCutoff(LocalDateTime.parse("2021-08-29 21:00:00.000", formatter))
                        .shippingCreationCutOffInHours(null)
                        .warehouseCutoffCutOffInHours(null)
                        .build()));

        TreeMap<LocalDateTime, List<DeliveryServiceCutoff>> actual =
                deliveryCutOffService.createSchedulerForDeliveryServicesShipmentDateTime(orders);

        assertEquals(expected, actual);
    }

    @Test
    public void threeOrdersWithOneCarrier() {
        Order order1 = mock(Order.class);
        when(order1.getShipmentDateTime()).thenReturn(
                OffsetDateTime.of(LocalDateTime.parse("2021-08-29 23:00:00.000", formatter), ZoneOffset.UTC));
        when(order1.getCarrierCode()).thenReturn("100");

        Order order2 = mock(Order.class);
        when(order2.getShipmentDateTime()).thenReturn(
                OffsetDateTime.of(LocalDateTime.parse("2021-08-29 20:00:00.000", formatter), ZoneOffset.UTC));
        when(order2.getCarrierCode()).thenReturn("100");

        Order order3 = mock(Order.class);
        when(order3.getShipmentDateTime()).thenReturn(
                OffsetDateTime.of(LocalDateTime.parse("2021-08-30 01:00:00.000", formatter), ZoneOffset.UTC));
        when(order3.getCarrierCode()).thenReturn("100");

        List<Order> orders = List.of(order1, order2, order3);

        TreeMap<LocalDateTime, List<DeliveryServiceCutoff>> expected = new TreeMap<>();

        expected.put(LocalDateTime.parse("2021-08-29 20:00:00.000", formatter),
                List.of(DeliveryServiceCutoff.builder()
                        .deliveryServiceCode("100")
                        .shippingCutoff(LocalDateTime.parse("2021-08-29 20:00:00.000", formatter))
                        .warehouseCutoff(LocalDateTime.parse("2021-08-29 16:00:00.000", formatter))
                        .shippingCreationCutOffInHours(null)
                        .warehouseCutoffCutOffInHours(null)
                        .build()));

        expected.put(LocalDateTime.parse("2021-08-29 23:00:00.000", formatter),
                List.of(DeliveryServiceCutoff.builder()
                        .deliveryServiceCode("100")
                        .shippingCutoff(LocalDateTime.parse("2021-08-29 23:00:00.000", formatter))
                        .warehouseCutoff(LocalDateTime.parse("2021-08-29 19:00:00.000", formatter))
                        .shippingCreationCutOffInHours(null)
                        .warehouseCutoffCutOffInHours(null)
                        .build()));

        expected.put(LocalDateTime.parse("2021-08-30 01:00:00.000", formatter),
                List.of(DeliveryServiceCutoff.builder()
                        .deliveryServiceCode("100")
                        .shippingCutoff(LocalDateTime.parse("2021-08-30 01:00:00.000", formatter))
                        .warehouseCutoff(LocalDateTime.parse("2021-08-29 21:00:00.000", formatter))
                        .shippingCreationCutOffInHours(null)
                        .warehouseCutoffCutOffInHours(null)
                        .build()));

        TreeMap<LocalDateTime, List<DeliveryServiceCutoff>> actual =
                deliveryCutOffService.createSchedulerForDeliveryServicesShipmentDateTime(orders);

        assertEquals(expected, actual);
    }

    @Test
    public void differentCarrierButSameShippingCutoff() {
        Order order1 = mock(Order.class);
        when(order1.getShipmentDateTime()).thenReturn(
                OffsetDateTime.of(LocalDateTime.parse("2021-08-29 23:00:00.000", formatter), ZoneOffset.UTC));
        when(order1.getCarrierCode()).thenReturn("100");

        Order order2 = mock(Order.class);
        when(order2.getShipmentDateTime()).thenReturn(
                OffsetDateTime.of(LocalDateTime.parse("2021-08-29 23:00:00.000", formatter), ZoneOffset.UTC));
        when(order2.getCarrierCode()).thenReturn("200");

        Order order3 = mock(Order.class);
        when(order3.getShipmentDateTime()).thenReturn(
                OffsetDateTime.of(LocalDateTime.parse("2021-08-29 23:00:00.000", formatter), ZoneOffset.UTC));
        when(order3.getCarrierCode()).thenReturn("300");

        List<Order> orders = List.of(order1, order2, order3);

        TreeMap<LocalDateTime, List<DeliveryServiceCutoff>> expected = new TreeMap<>();

        expected.put(LocalDateTime.parse("2021-08-29 23:00:00.000", formatter),
                List.of(DeliveryServiceCutoff.builder()
                                .deliveryServiceCode("100")
                                .shippingCutoff(LocalDateTime.parse("2021-08-29 23:00:00.000", formatter))
                                .warehouseCutoff(LocalDateTime.parse("2021-08-29 19:00:00.000", formatter))
                                .shippingCreationCutOffInHours(null)
                                .warehouseCutoffCutOffInHours(null)
                                .build(),
                        DeliveryServiceCutoff.builder()
                                .deliveryServiceCode("200")
                                .shippingCutoff(LocalDateTime.parse("2021-08-29 23:00:00.000", formatter))
                                .warehouseCutoff(LocalDateTime.parse("2021-08-29 19:00:00.000", formatter))
                                .shippingCreationCutOffInHours(null)
                                .warehouseCutoffCutOffInHours(null)
                                .build(),
                        DeliveryServiceCutoff.builder()
                                .deliveryServiceCode("300")
                                .shippingCutoff(LocalDateTime.parse("2021-08-29 23:00:00.000", formatter))
                                .warehouseCutoff(LocalDateTime.parse("2021-08-29 19:00:00.000", formatter))
                                .shippingCreationCutOffInHours(null)
                                .warehouseCutoffCutOffInHours(null)
                                .build()
                ));

        TreeMap<LocalDateTime, List<DeliveryServiceCutoff>> actual =
                deliveryCutOffService.createSchedulerForDeliveryServicesShipmentDateTime(orders);

        assertEquals(expected, actual);
    }

    @Test
    public void threeOrdersSameCarrierAndSameShipmentDateTime() {
        Order order1 = mock(Order.class);
        when(order1.getShipmentDateTime()).thenReturn(
                OffsetDateTime.of(LocalDateTime.parse("2021-08-29 23:00:00.000", formatter), ZoneOffset.UTC));
        when(order1.getCarrierCode()).thenReturn("100");

        Order order2 = mock(Order.class);
        when(order2.getShipmentDateTime()).thenReturn(
                OffsetDateTime.of(LocalDateTime.parse("2021-08-29 23:00:00.000", formatter), ZoneOffset.UTC));
        when(order2.getCarrierCode()).thenReturn("100");

        Order order3 = mock(Order.class);
        when(order3.getShipmentDateTime()).thenReturn(
                OffsetDateTime.of(LocalDateTime.parse("2021-08-29 23:00:00.000", formatter), ZoneOffset.UTC));
        when(order3.getCarrierCode()).thenReturn("100");

        List<Order> orders = List.of(order1, order2, order3);

        TreeMap<LocalDateTime, List<DeliveryServiceCutoff>> expected = new TreeMap<>();

        expected.put(LocalDateTime.parse("2021-08-29 23:00:00.000", formatter),
                List.of(DeliveryServiceCutoff.builder()
                        .deliveryServiceCode("100")
                        .shippingCutoff(LocalDateTime.parse("2021-08-29 23:00:00.000", formatter))
                        .warehouseCutoff(LocalDateTime.parse("2021-08-29 19:00:00.000", formatter))
                        .shippingCreationCutOffInHours(null)
                        .warehouseCutoffCutOffInHours(null)
                        .build()
                ));

        TreeMap<LocalDateTime, List<DeliveryServiceCutoff>> actual =
                deliveryCutOffService.createSchedulerForDeliveryServicesShipmentDateTime(orders);

        assertEquals(expected, actual);
    }

    @Test
    @DatabaseSetup(
            "/autostartlogic/service/deliverycutoffservice/get-delivery-order-data/returns-order/nsqlconfig.xml")
    @DatabaseSetup("/autostartlogic/service/deliverycutoffservice/get-delivery-order-data/immutable.xml")
    public void getDeliveryOrderDataReturnsOrders() {
        DeliveryOrderData orderData = deliveryCutOffService.getDeliveryOrderData(1);
        List<Order> orders = orderData.getAllOrdersForStart();

        assertAll(
                () -> assertEquals(1, orders.stream().filter(f -> f.getOrderKey().equals("0000123")).count()),
                () -> assertEquals(1, orders.stream().filter(f -> f.getOrderKey().equals("0000155")).count()),
                () -> assertEquals(2, orders.size()),
                () -> assertTrue(
                        orderData.getDeliveryServiceCutoffs().containsKey(
                                LocalDateTime.parse("2020-04-01 14:00:00.000", formatter)
                        )
                )
        );
    }

    @Test
    @DatabaseSetup("/autostartlogic/service/deliverycutoffservice/get-delivery-order-data/no-order/nsqlconfig.xml")
    @DatabaseSetup("/autostartlogic/service/deliverycutoffservice/get-delivery-order-data/immutable.xml")
    public void getDeliveryOrderDataReturnsNoOrder() {
        DeliveryOrderData orderData = deliveryCutOffService.getDeliveryOrderData(1);
        assertAll(
                () -> assertTrue(orderData.getAllOrdersForStart().isEmpty()),
                () -> assertTrue(orderData.getDeliveryServiceCutoffs().isEmpty())
        );
    }
}
