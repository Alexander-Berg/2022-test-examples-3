package ru.yandex.market.deepdive.domain;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.deepdive.AbstractTest;
import ru.yandex.market.deepdive.domain.client.PvzClient;
import ru.yandex.market.deepdive.domain.client.dto.PageableResponse;
import ru.yandex.market.deepdive.domain.client.dto.PvzIntOrderDto;
import ru.yandex.market.deepdive.domain.client.dto.PvzIntPickupPointDto;
import ru.yandex.market.deepdive.domain.controller.dto.OrderDto;
import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.order.OrderFilter;
import ru.yandex.market.deepdive.domain.order.OrderService;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;
import ru.yandex.market.deepdive.executor.UpdateDataExecutor;

public class ExecutorTest extends AbstractTest {
    @Autowired
    UpdateDataExecutor updateDataExecutor;

    @Autowired
    private PickupPointService pickupPointService;

    @Autowired
    private OrderService orderService;

    @Value("${pvz-int.partner.id:1001047541}")
    private Long partnerId;

    @MockBean
    private PvzClient client;

    @Test
    @DisplayName("Update pickup-points")
    public void updatePickupPoints() {
        PageableResponse<PvzIntPickupPointDto> response = new PageableResponse<>();
        response.setContent(Collections.emptyList());
        response.setLast(true);
        Mockito.when(client.getPickupPointsResponse(partnerId, 0)).thenReturn(response);
        List<PickupPointDto> current = pickupPointService.getPickupPoints();
        updateDataExecutor.doRealJob(null);
        Assertions.assertEquals(current, pickupPointService.getPickupPoints());
    }

    @Test
    @DisplayName("Update orders")
    public void updateOrders() {
        PageableResponse<PvzIntOrderDto> response = new PageableResponse<>();
        response.setContent(Collections.emptyList());
        response.setLast(true);
        Mockito.when(client.getOrdersResponse(partnerId, 0)).thenReturn(response);
        List<OrderDto> current = orderService.ordersByFilter(new OrderFilter(null, null, null));
        updateDataExecutor.doRealJob(null);
        Assertions.assertEquals(current, orderService.ordersByFilter(new OrderFilter(null, null, null)));
    }

}
