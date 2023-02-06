package ru.yandex.market.deepdive.domain.controller;

import java.sql.Date;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.controller.dto.PickupPointOrderDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;
import ru.yandex.market.deepdive.domain.pickup_point_order.PickupPointOrder;
import ru.yandex.market.deepdive.domain.pickup_point_order.PickupPointOrderMapper;
import ru.yandex.market.deepdive.domain.pickup_point_order.PickupPointOrderRepository;
import ru.yandex.market.deepdive.domain.pickup_point_order.PickupPointOrderService;

public class PickupPointOrderControllerTest {
    @Test
    public void OrderControllerTest() {
        PickupPointOrderRepository repository = Mockito.mock(PickupPointOrderRepository.class);
        PickupPointOrderMapper mapper = new PickupPointOrderMapper();
        List<PickupPointOrder> pickupPointOrders = new LinkedList<>();
        PickupPointOrder pickupPointOrder = new PickupPointOrder();

        pickupPointOrder.setPickupPointId(1L);
        pickupPointOrder.setId(1L);
        pickupPointOrder.setPaymentType("CARD");
        pickupPointOrder.setStatus("CREATED");
        pickupPointOrder.setTotalPrice(1000);
        pickupPointOrder.setDeliveryDate(new Date(2020, 10, 10));

        Mockito.when(repository.findAll()).thenReturn(pickupPointOrders);

        PickupPointOrderService service = new PickupPointOrderService(repository, mapper);
        PickupPointOrderController controller = new PickupPointOrderController(service);

        List<PickupPointOrderDto> gottenOrders = controller.getPickupPointOrders(1L, null, null);

        Assert.assertArrayEquals(pickupPointOrders.stream().map(mapper::map).toArray(), gottenOrders.toArray());
    }

    @Test
    public void OrderControllerFilterTest() {
        PickupPointOrderRepository repository = Mockito.mock(PickupPointOrderRepository.class);
        PickupPointOrderMapper mapper = new PickupPointOrderMapper();

        PickupPointOrder pickupPointOrder = new PickupPointOrder();
        pickupPointOrder.setPickupPointId(1L);
        pickupPointOrder.setId(1L);
        pickupPointOrder.setPaymentType("CARD");
        pickupPointOrder.setStatus("CREATED");
        pickupPointOrder.setTotalPrice(1000);
        pickupPointOrder.setDeliveryDate(new Date(2020, 10, 10));

        PickupPointOrder pickupPointOrderStatus = new PickupPointOrder();
        pickupPointOrder.setPickupPointId(1L);
        pickupPointOrder.setId(2L);
        pickupPointOrder.setPaymentType("CARD");
        pickupPointOrder.setStatus("LOST");
        pickupPointOrder.setTotalPrice(1000);
        pickupPointOrder.setDeliveryDate(new Date(2020, 10, 10));

        PickupPointOrder pickupPointOrderPaymentType = new PickupPointOrder();
        pickupPointOrder.setPickupPointId(1L);
        pickupPointOrder.setId(3L);
        pickupPointOrder.setPaymentType("CASH");
        pickupPointOrder.setStatus("CREATED");
        pickupPointOrder.setTotalPrice(1000);
        pickupPointOrder.setDeliveryDate(new Date(2020, 10, 10));

        PickupPointOrder pickupPointOrderStatusAndPaymentType = new PickupPointOrder();
        pickupPointOrder.setPickupPointId(1L);
        pickupPointOrder.setId(4L);
        pickupPointOrder.setPaymentType("CASH");
        pickupPointOrder.setStatus("LOST");
        pickupPointOrder.setTotalPrice(1000);
        pickupPointOrder.setDeliveryDate(new Date(2020, 10, 10));

        List<PickupPointOrder> status = new ArrayList<>();
        status.add(pickupPointOrderStatus);
        status.add(pickupPointOrderStatusAndPaymentType);

        List<PickupPointOrder> paymentType = new ArrayList<>();
        paymentType.add(pickupPointOrderPaymentType);
        paymentType.add(pickupPointOrderStatusAndPaymentType);

        List<PickupPointOrder> statusAndPaymentType = new ArrayList<>();
        statusAndPaymentType.add(pickupPointOrderStatusAndPaymentType);

        List<PickupPointOrder> allOrders = new ArrayList<>();
        allOrders.add(pickupPointOrderStatus);
        allOrders.add(pickupPointOrderStatus);
        allOrders.add(pickupPointOrderStatusAndPaymentType);
        allOrders.add(pickupPointOrder);


        Mockito.when(repository.findAllByPickupPointId(1L)).thenReturn(allOrders);
        Mockito.when(repository.findAllByPickupPointIdAndStatus(1L, "LOST")).thenReturn(status);
        Mockito.when(repository.findAllByPickupPointIdAndPaymentType(1L, "CASH")).thenReturn(paymentType);
        Mockito.when(repository.findAllByPickupPointIdAndStatusAndPaymentType(1L, "LOST", "CASH")).thenReturn(statusAndPaymentType);

        PickupPointOrderService service = new PickupPointOrderService(repository, mapper);
        PickupPointOrderController controller = new PickupPointOrderController(service);

        List<PickupPointOrderDto> gottenOrders = controller.getPickupPointOrders(1L, null, null);
        List<PickupPointOrderDto> gottenOrdersWithStatus = controller.getPickupPointOrders(1L, "LOST", null);
        List<PickupPointOrderDto> gottenOrdersWithPaymentType = controller.getPickupPointOrders(1L, null, "CASH");
        List<PickupPointOrderDto> gottenOrdersWithStatusPaymentType = controller.getPickupPointOrders(1L, "LOST",
                "CASH");


        Assert.assertArrayEquals(allOrders.stream().map(mapper::map).toArray(), gottenOrders.toArray());
        Assert.assertArrayEquals(status.stream().map(mapper::map).toArray(), gottenOrdersWithStatus.toArray());
        Assert.assertArrayEquals(paymentType.stream().map(mapper::map).toArray(),
                gottenOrdersWithPaymentType.toArray());
        Assert.assertArrayEquals(statusAndPaymentType.stream().map(mapper::map).toArray(),
                gottenOrdersWithStatusPaymentType.toArray());


    }
}
