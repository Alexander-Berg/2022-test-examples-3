package ru.yandex.market.deepdive.domain.controller;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageImpl;

import ru.yandex.market.deepdive.domain.controller.dto.OrderToPvzDto;
import ru.yandex.market.deepdive.domain.order.OrderService;
import ru.yandex.market.deepdive.domain.order.OrderToPvz;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

public class OrderControllerTest {

    @Description("Test check if OrderController finds needed orders" +
            "with using filter.")
    @Test
    public void getPickupPointsWithoutExceptionsTest() {

        PickupPoint pickupPoint0 = getPickupPoint(0L, "0", 10L);
        PickupPoint pickupPoint1 = getPickupPoint(1L, "1", 20L);

        OrderToPvzDto order0 = getOrder(0L, 0L,
                "PREPAID", "STORAGE_PERIOD_EXPIRED");

        OrderToPvzDto order1 = getOrder(1L, 0L,
                "CASH", "STORAGE_PERIOD_EXPIRED");

        OrderToPvzDto order2 = getOrder(2L, 1L, "CASH", "CREATED");

        OrderToPvzDto order3 = getOrder(3L, 1L, "PREPAID", "CREATED");

        OrderToPvzDto order4 = getOrder(4L, 1L,
                "CASH", "STORAGE_PERIOD_EXPIRED");

        OrderService orderService = Mockito.mock(OrderService.class);
        PickupPointService pickupPointService = Mockito.mock(PickupPointService.class);

        Mockito.when(pickupPointService.getById(0L))
                .thenReturn(pickupPoint0);

        Mockito.when(pickupPointService.getById(1L))
                .thenReturn(pickupPoint1);

        Mockito.when(orderService.getOrdersByExample(Example.of(
                        OrderToPvz.builder()
                                .id(null)
                                .pickupPoint(pickupPoint0)
                                .status(null)
                                .deliveryDate(null)
                                .paymentType(null)
                                .totalPrice(null)
                                .build()), null))
                .thenReturn(new PageImpl<>(List.of(order0, order1)));

        Mockito.when(orderService.getOrdersByExample(Example.of(
                        OrderToPvz.builder()
                                .id(null)
                                .pickupPoint(pickupPoint1)
                                .status(null)
                                .deliveryDate(null)
                                .paymentType(null)
                                .totalPrice(null)
                                .build()), null))
                .thenReturn(new PageImpl<>(List.of(order2, order3, order4)));

        Mockito.when(orderService.getOrdersByExample(Example.of(
                OrderToPvz.builder()
                        .id(null)
                        .pickupPoint(pickupPoint0)
                        .status("STORAGE_PERIOD_EXPIRED")
                        .deliveryDate(null)
                        .paymentType(null)
                        .totalPrice(null)
                        .build()), null))
                .thenReturn(new PageImpl<>(List.of(order0, order1)));

        Mockito.when(orderService.getOrdersByExample(Example.of(
                        OrderToPvz.builder()
                                .id(null)
                                .pickupPoint(pickupPoint1)
                                .status(null)
                                .deliveryDate(null)
                                .paymentType("CASH")
                                .totalPrice(null)
                                .build()), null))
                .thenReturn(new PageImpl<>(List.of(order2, order4)));

        Mockito.when(orderService.getOrdersByExample(Example.of(
                        OrderToPvz.builder()
                                .id(null)
                                .pickupPoint(null)
                                .status("STORAGE_PERIOD_EXPIRED")
                                .deliveryDate(null)
                                .paymentType("CASH")
                                .totalPrice(null)
                                .build()), null))
                .thenReturn(new PageImpl<>(List.of(order1, order4)));

        Mockito.when(orderService.getOrdersByExample(Example.of(
                        OrderToPvz.builder()
                                .id(null)
                                .pickupPoint(null)
                                .status(null)
                                .deliveryDate(null)
                                .paymentType(null)
                                .totalPrice(null)
                                .build()), null))
                .thenReturn(new PageImpl<>(
                        List.of(order0, order1, order2, order3, order4)));

        OrderController controller = new OrderController(orderService, pickupPointService);

        List<OrderToPvzDto> result = controller
                .getOrdersWithFilter(0L, null, null, null).getContent();

        if (!result.containsAll(List.of(order0, order1)) || result.size() != 2) {
            Assert.fail("Incorrect result for controller.getOrdersWithFilter(0L, null, null);" +
                    "expected" + order0 + order1 + " but actual " + result);
        }

        result = controller.getOrdersWithFilter(1L, null, null, null).getContent();

        if (!result.containsAll(List.of(order2, order3, order4)) || result.size() != 3) {
            Assert.fail("Incorrect result for controller.getOrdersWithFilter(1L, null, null);");
        }

        result = controller.getOrdersWithFilter(0L, "STORAGE_PERIOD_EXPIRED", null, null).getContent();

        if (!result.containsAll(List.of(order0, order1)) || result.size() != 2) {
            Assert.fail("Incorrect result for controller." +
                    "getOrdersWithFilter(0L, \"STORAGE_PERIOD_EXPIRED\", null);");
        }

        result = controller.getOrdersWithFilter(1L, null, "CASH", null).getContent();

        if (!result.containsAll(List.of(order2, order4)) || result.size() != 2) {
            Assert.fail("Incorrect result for controller.getOrdersWithFilter(1L, null, \"CASH\");");
        }

        result = controller.getOrdersWithFilter(null, "STORAGE_PERIOD_EXPIRED", "CASH", null).getContent();

        if (!result.containsAll(List.of(order1, order4)) || result.size() != 2) {
            Assert.fail("Incorrect result for " +
                    "controller.getOrdersWithFilter(null, \"STORAGE_PERIOD_EXPIRED\", \"CASH\");");
        }

        result = controller.getOrdersWithFilter(null, null, null, null).getContent();

        if (!result.containsAll(List.of(order0, order1, order2, order3, order4)) || result.size() != 5) {
            Assert.fail("Incorrect result for controller.getOrdersWithFilter(null, null, null);");
        }
    }

    private PickupPoint getPickupPoint(Long id, String name, Long pvzMarketId) {
        return PickupPoint.builder()
                .id(id)
                .name(name)
                .pvzMarketId(pvzMarketId)
                .build();
    }

    private OrderToPvzDto getOrder(Long id, Long pickupPointId, String paymentType, String status) {
        return OrderToPvzDto.builder()
                .id(id)
                .pickupPointId(pickupPointId)
                .paymentType(paymentType)
                .status(status)
                .build();
    }
}
