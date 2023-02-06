package ru.yandex.market.deepdive.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.OrderController;
import ru.yandex.market.deepdive.domain.controller.dto.OrderDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

public class OrderControllerTest {

    @Test
    public void testV1WithMockito() {
        PickupPointService pickupPointService = Mockito.mock(PickupPointService.class);

        OrderDto.OrderDtoBuilder orderDtoBuilder = OrderDto.builder();
        List<OrderDto> pickupPointOrdersList = List.of(
                orderDtoBuilder.id(1).build(),
                orderDtoBuilder.id(2).build()
        );

        long pvzOrderId = 1;
        Mockito.when(pickupPointService.getOrdersByPickupPointId(pvzOrderId)).thenReturn(pickupPointOrdersList);

        OrderController orderController = new OrderController(pickupPointService);
        orderController.getOrdersByPvzId(pvzOrderId);
        Mockito.verify(pickupPointService).getOrdersByPickupPointId(pvzOrderId);
    }
}
