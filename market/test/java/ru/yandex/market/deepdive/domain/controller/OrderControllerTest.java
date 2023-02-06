package ru.yandex.market.deepdive.domain.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.dto.OrderDto;
import ru.yandex.market.deepdive.domain.order.Order;
import ru.yandex.market.deepdive.domain.order.OrderMapper;
import ru.yandex.market.deepdive.domain.order.OrderRepository;
import ru.yandex.market.deepdive.domain.order.OrderService;
import ru.yandex.market.deepdive.domain.order.OrderSpecification;
import ru.yandex.market.deepdive.domain.order.PaymentType;
import ru.yandex.market.deepdive.domain.order.Status;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderControllerTest {

    @Mock
    private OrderRepository orderRepository;

    private OrderMapper orderMapper = new OrderMapper();

    @Captor
    private ArgumentCaptor<OrderSpecification> argumentCaptor;

    private List<Order> orders;

    @Before
    public void setUp() {
        orders = List.of(
                Order.builder()
                        .id(1L)
                        .pvzMarketId("firstOne")
                        .status(Status.ARRIVED_TO_PICKUP_POINT)
                        .paymentType(PaymentType.PREPAID)
                        .build(),
                Order.builder()
                        .id(2L)
                        .pvzMarketId("firstOne")
                        .status(Status.ARRIVED_TO_PICKUP_POINT)
                        .paymentType(PaymentType.CARD)
                        .build(),
                Order.builder()
                        .id(3L)
                        .pvzMarketId("firstOne")
                        .status(Status.DELIVERED_TO_RECIPIENT)
                        .paymentType(PaymentType.CARD)
                        .build(),
                Order.builder()
                        .id(4L)
                        .pvzMarketId("theBestOne")
                        .status(Status.ARRIVED_TO_PICKUP_POINT)
                        .paymentType(PaymentType.PREPAID)
                        .build(),
                Order.builder()
                        .id(5L)
                        .pvzMarketId("theBestOne")
                        .status(Status.ARRIVED_TO_PICKUP_POINT)
                        .paymentType(PaymentType.CARD)
                        .build(),
                Order.builder()
                        .id(6L)
                        .pvzMarketId("theBestOne")
                        .status(Status.DELIVERED_TO_RECIPIENT)
                        .paymentType(PaymentType.CARD)
                        .build()
        );

        Mockito.when(orderRepository.findAll(argumentCaptor.capture())).thenReturn(
                orders.stream()
                        .filter(order -> {
                            OrderSpecification specification = argumentCaptor.getValue();
                            return order.getPvzMarketId().equals(specification.getPvzMarketId()) &&
                                    order.getStatus().equals(specification.getStatus()) &&
                                    order.getPaymentType().equals(specification.getPaymentType());
                        })
                        .collect(Collectors.toList())
        );
    }

    @Test
    public void getOrdersByPvzMarketId() {
        OrderController orderController = new OrderController(
                new OrderService(orderRepository, orderMapper)
        );
        String pvzMarketId = "firstOne";
        List<OrderDto> expectedPoints = orders.stream()
                .filter(order -> order.getPvzMarketId().equals(pvzMarketId))
                .map(orderMapper::map)
                .collect(Collectors.toList());
        List<OrderDto> actualPoints = orderController.getOrders(pvzMarketId, null, null);
        assertThat(actualPoints).isEqualTo(expectedPoints);
    }

    @Test
    public void getOrdersByStatus() {
        OrderController orderController = new OrderController(
                new OrderService(orderRepository, orderMapper)
        );
        String pvzMarketId = "firstOne";
        Status status = Status.ARRIVED_TO_PICKUP_POINT;
        List<OrderDto> expectedPoints = orders.stream()
                .filter(order -> order.getPvzMarketId().equals(pvzMarketId)
                        && order.getStatus().equals(status))
                .map(orderMapper::map)
                .collect(Collectors.toList());
        List<OrderDto> actualPoints = orderController.getOrders(pvzMarketId, status, null);
        assertThat(actualPoints).isEqualTo(expectedPoints);
    }

    @Test
    public void getOrdersByPaymentType() {
        OrderController orderController = new OrderController(
                new OrderService(orderRepository, orderMapper)
        );
        String pvzMarketId = "firstOne";
        PaymentType paymentType = PaymentType.CARD;
        List<OrderDto> expectedPoints = orders.stream()
                .filter(order -> order.getPvzMarketId().equals(pvzMarketId)
                        && order.getPaymentType().equals(paymentType))
                .map(orderMapper::map)
                .collect(Collectors.toList());
        List<OrderDto> actualPoints = orderController.getOrders(pvzMarketId, null, paymentType);
        assertThat(actualPoints).isEqualTo(expectedPoints);
    }

    @Test
    public void getOrdersForNonExistingPvzMarketId() {
        OrderController orderController = new OrderController(
                new OrderService(orderRepository, orderMapper)
        );
        String pvzMarketId = "phantomPvz";
        List<OrderDto> actualPoints = orderController.getOrders(pvzMarketId, null, null);
        assertThat(actualPoints.size()).isEqualTo(0);
    }

}
