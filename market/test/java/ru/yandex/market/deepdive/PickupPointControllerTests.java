package ru.yandex.market.deepdive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import ru.yandex.market.deepdive.domain.controller.PickupPointController;
import ru.yandex.market.deepdive.domain.controller.dto.OrderDto;
import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.order.OrderService;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

@RunWith(MockitoJUnitRunner.class)
public class PickupPointControllerTests {

    @Mock
    private PickupPointService pickupPointService;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private PickupPointController controller;

    @Test
    public void getPickupPoints() {
        Mockito.when(pickupPointService.getPickupPoints()).thenReturn(new ArrayList<PickupPointDto>());
        Assert.assertEquals(controller.getPickupPoints().size(), 0);
    }

    @Test
    public void getOrdersWithoutFilters() {
        Page<OrderDto> page = new PageImpl<>(Collections.nCopies(2, OrderDto.builder().build()));
        Mockito.when(orderService.getOrders(1, null, null, PageRequest.of(0, 3))).thenReturn(page);
        Assert.assertEquals(controller.getOrders(1, null, null, PageRequest.of(0, 3)).getTotalPages(),
                1);
        Assert.assertEquals(controller.getOrders(1, null, null, PageRequest.of(0, 3)).getTotalElements(),
                2);
    }

    @Test
    public void getOrdersWithFilters() {
        OrderDto order1 = OrderDto.builder().id(1).status("CREATED").paymentType("CARD").build();
        OrderDto order2 = OrderDto.builder().id(2).status("ARRIVED_TO_PICKUP_POINT").paymentType("PREPAID").build();
        OrderDto order3 = OrderDto.builder().id(4).status("STORAGE_PERIOD_EXPIRED").paymentType("PREPAID").build();
        OrderDto order4 = OrderDto.builder().id(5).status("CREATED").paymentType("PREPAID").build();
        Page<OrderDto> pageCreated = new PageImpl<OrderDto>(List.of(order1, order4));
        Page<OrderDto> pagePrepaid = new PageImpl<OrderDto>(List.of(order2, order3, order4));
        Page<OrderDto> pageCreatedAndPrepaid = new PageImpl<OrderDto>(List.of(order4));


        Mockito.when(orderService.getOrders(1,
                "CREATED",
                null,
                PageRequest.of(0, 3))
                ).thenReturn(pageCreated);
        Mockito.when(orderService.getOrders(1,
                null,
                "PREPAID",
                PageRequest.of(0, 3))
        ).thenReturn(pagePrepaid);
        Mockito.when(orderService.getOrders(1,
                "CREATED",
                "PREPAID",
                PageRequest.of(0, 3))
        ).thenReturn(pageCreatedAndPrepaid);
        Assert.assertEquals(controller.getOrders(1,
                        "CREATED",
                        null,
                        PageRequest.of(0, 3)
                ).getTotalElements(), 2);
        Assert.assertEquals(controller.getOrders(1,
                        null,
                        "PREPAID",
                        PageRequest.of(0, 3)
                ).getTotalElements(), 3);
        Assert.assertEquals(controller.getOrders(1,
                        "CREATED",
                        "PREPAID",
                        PageRequest.of(0, 3)
                ).getTotalElements(), 1);
    }

}
