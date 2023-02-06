package ru.yandex.market.deepdive.domain.controller;

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.deepdive.domain.controller.dto.OrderDto;
import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.order.OrderService;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PickupPointControllerTest {

    private static final Pageable DEFAULT_PAGE_REQUEST = PageRequest.of(0, 10);

    @Mock
    private PickupPointService pickupPointService;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private PickupPointController controller;

    @Test
    public void testGetPickupPoints() {
        PickupPointDto point = PickupPointDto.builder().id(1).name("A").build();
        List<PickupPointDto> points = List.of(point);
        when(pickupPointService.getPickupPoints()).thenReturn(points);

        List<PickupPointDto> result = controller.getPickupPoints();

        verify(pickupPointService, times(1)).getPickupPoints();
        assertEquals(points, result);
    }

    @Test
    public void testGetOrders() {
        Long testPickupPointId = 10L;
        OrderDto order = OrderDto.builder().id(1).status("1").build();
        List<OrderDto> orders = List.of(order);
        Page<OrderDto> orderDtoPage = new PageImpl<>(
                orders,
                DEFAULT_PAGE_REQUEST,
                2
        );
        when(orderService.getOrders(any(), any(), any(), any())).thenReturn(orderDtoPage);

        Page<OrderDto> result = controller.getOrders(
                testPickupPointId,
                DEFAULT_PAGE_REQUEST,
                Optional.empty(),
                Optional.empty()
        );

        assertEquals(orders, result.getContent());
    }
}
