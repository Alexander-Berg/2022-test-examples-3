package ru.yandex.market.deepdive;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.OrderController;
import ru.yandex.market.deepdive.domain.controller.PickupPointController;
import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.order.Order;
import ru.yandex.market.deepdive.domain.order.OrderService;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

public class DeepDiveTest {

    @Test
    public void test() {
        //Just a test example
        Assert.assertTrue(true);
    }

    @Test
    public void PickPointController() {

        PickupPoint pickupPoint1 = PickupPoint.builder().id(404L).name("404L").build();
        PickupPoint pickupPoint2 = PickupPoint.builder().id(406L).name("406L").build();

        PickupPointMapper pickupPointMapper = new PickupPointMapper();

        PickupPointDto pickupPointDto1 = pickupPointMapper.map(pickupPoint1);
        PickupPointDto pickupPointDto2 = pickupPointMapper.map(pickupPoint2);

        PickupPointRepository pickupPointRepository = Mockito.mock(PickupPointRepository.class);

        Mockito.when(pickupPointRepository.findAll()).thenReturn((List.of(pickupPoint1, pickupPoint2)));

        PickupPointService pickupPointService = new PickupPointService(
                pickupPointRepository,
                pickupPointMapper
        );

        PickupPointController pickupPointController = new PickupPointController(pickupPointService);
        List<PickupPointDto> outputPickupPointDtos = pickupPointController.getPickupPoints();

        Assert.assertEquals(outputPickupPointDtos, List.of(pickupPointDto1, pickupPointDto2));
    }

    @Test
    public void OrderController() {

        OrderService orderService = Mockito.mock(OrderService.class);

        List<Order> orderList = List.of(
                Order.builder().id(1L).paymentType("CARD").status("CREATED").build(),
                Order.builder().id(2L).paymentType("PREPAID").status("STORAGE_PERIOD_EXPIRED").build(),
                Order.builder().id(3L).paymentType("CARD").status("CREATED").build()
        );

        List<Order> filteredOrderList = List.of(
                orderList.get(0),
                orderList.get(2)
        );

        OrderController orderController = new OrderController(orderService);

        Mockito.when(orderService.findAllByStatus("CREATED")).thenReturn(filteredOrderList);
        Assert.assertEquals(orderController.allOrdersByStatus("CREATED"), filteredOrderList);

        Mockito.when(orderService.findAllByPaymentType("CARD")).thenReturn(filteredOrderList);
        Assert.assertEquals(orderController.allOrdersByPaymentType("CARD"), filteredOrderList);
    }
}
