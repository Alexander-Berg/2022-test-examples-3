package ru.yandex.market.deepdive.controller;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.deepdive.domain.controller.OrderController;
import ru.yandex.market.deepdive.domain.order.Order;
import ru.yandex.market.deepdive.domain.order.OrderMapper;
import ru.yandex.market.deepdive.domain.order.OrderRepository;
import ru.yandex.market.deepdive.domain.order.OrderService;


@RunWith(MockitoJUnitRunner.class)
public class OrderControllerTest {
    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    @Mock
    OrderRepository orderRepository;
    @Mock
    OrderMapper orderMapper;

    @Test
    public void findAll() throws ParseException {

        List<Order> list = Stream.of(
                Order.builder()
                        .id(189L)
                        .pickupPointId(85L)
                        .status("CREATED")
                        .deliveryDate(formatter.parse("2022-02-01"))
                        .paymentType("PREPAID")
                        .totalPrice(12345.00)
                        .build(),
                Order.builder()
                        .id(28L)
                        .pickupPointId(133L)
                        .status("TRANSMITTED_TO_RECIPIENT")
                        .deliveryDate(formatter.parse("2021-07-13"))
                        .paymentType("PREPAID")
                        .totalPrice(77.00)
                        .build()
        ).collect(Collectors.toList());

        Mockito.when(orderRepository.findAll()).thenReturn(list);
        OrderService orderService = new OrderService(orderRepository, orderMapper);
        OrderController orderController = new OrderController(orderService);

        Assert.assertEquals(orderController.getOrders().get(0).getStatus(), "CREATED");
        Assert.assertEquals(orderController.getOrders(), list);
    }

    @Test
    public void findAllByPickupPointId() throws ParseException {
        List<Order> list = Stream.of(
                Order.builder()
                        .id(189L)
                        .pickupPointId(85L)
                        .status("CREATED")
                        .deliveryDate(formatter.parse("2002-12-03"))
                        .paymentType("PREPAID")
                        .totalPrice(51641.00)
                        .build(),
                Order.builder()
                        .id(134L)
                        .pickupPointId(85L)
                        .status("TRANSMITTED_TO_RECIPIENT")
                        .deliveryDate(formatter.parse("2022-01-21"))
                        .paymentType("CARD")
                        .totalPrice(51114.00)
                        .build()
        ).collect(Collectors.toList());

        Mockito.when(orderRepository.findAllByPickupPointId(85L)).thenReturn(list);

        Mockito.when(orderRepository.findAllByPickupPointIdAndStatus(85L, "CREATED")).thenReturn(list.subList(0, 1));

        Mockito.when(orderRepository.findAllByPickupPointIdAndPaymentType(85L, "PREPAID")).thenReturn(list.subList(0,
                1));

        Mockito.when(orderRepository.findAllByPickupPointIdAndStatusAndPaymentType(85L, "CREATED", "PREPAID"))
                .thenReturn(list.subList(0, 1));

        OrderService orderService = new OrderService(orderRepository, orderMapper);
        OrderController orderController = new OrderController(orderService);

        Assert.assertEquals(orderController.getOrdersByPickupPointId(85L, null, null), list);
        Assert.assertEquals(orderController.getOrdersByPickupPointId(85L, "CREATED", null), list.subList(0, 1));
        Assert.assertEquals(orderController.getOrdersByPickupPointId(85L, null, "PREPAID"), list.subList(0, 1));
        Assert.assertEquals(orderController.getOrdersByPickupPointId(85L, "CREATED", "PREPAID"), list.subList(0, 1));
    }
}
