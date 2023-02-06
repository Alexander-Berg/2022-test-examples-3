package ru.yandex.market.deepdive.domain.controller;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.controller.dto.PickupPointOrderDto;
import ru.yandex.market.deepdive.domain.order.PickupPointOrder;
import ru.yandex.market.deepdive.domain.order.PickupPointOrderMapper;
import ru.yandex.market.deepdive.domain.order.PickupPointOrderRepository;
import ru.yandex.market.deepdive.domain.order.PickupPointOrderService;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

public class PickupPointOrdersControllerTest {

    @Test
    public void getPickUpPointsOrdersTest() {
        PickupPointOrderRepository repository = Mockito.mock(PickupPointOrderRepository.class);
        PickupPointOrderService service = new PickupPointOrderService(repository, new PickupPointOrderMapper());

        List<PickupPointOrder> pickupPointOrders = new ArrayList<>();
        pickupPointOrders.add(PickupPointOrder.builder()
                .id(1L)
                .pickupPointId(111L)
                .totalPrice(777.07f)
                .build());
        pickupPointOrders.add(PickupPointOrder.builder()
                .id(22L)
                .pickupPointId(111L)
                .totalPrice(666.07f)
                .paymentType("PREPAID")
                .build());
        pickupPointOrders.add(PickupPointOrder.builder()
                .id(22L)
                .pickupPointId(222L)
                .totalPrice(999.07f)
                .paymentType("UNPAID")
                .build());

        Mockito.when(repository.findAll()).thenReturn(pickupPointOrders);
        PickupPointOrderController controller = new PickupPointOrderController(service);

        List<PickupPointOrderDto> outputList = new ArrayList<>();
        outputList.add(PickupPointOrderDto.builder()
                .id(1L)
                .pickupPointId(111L)
                .totalPrice(777.07f)
                .build());
        outputList.add(PickupPointOrderDto.builder()
                .id(22L)
                .pickupPointId(111L)
                .totalPrice(666.07f)
                .paymentType("PREPAID")
                .build());

        Assert.assertArrayEquals(outputList.toArray(), controller.getPickupPointOrders(111L, null, null).toArray());
    }

    @Test
    public void getPickUpPointsOrdersWithParamsTest() {
        PickupPointOrderRepository repository = Mockito.mock(PickupPointOrderRepository.class);
        PickupPointOrderService service = new PickupPointOrderService(repository, new PickupPointOrderMapper());

        List<PickupPointOrder> pickupPointOrders = new ArrayList<>();
        pickupPointOrders.add(PickupPointOrder.builder()
                .id(1L)
                .pickupPointId(111L)
                .totalPrice(777.07f)
                .paymentType("CARD")
                .status("DELIVERED")
                .build());
        pickupPointOrders.add(PickupPointOrder.builder()
                .id(22L)
                .pickupPointId(111L)
                .totalPrice(666.07f)
                .paymentType("CARD")
                .build());
        pickupPointOrders.add(PickupPointOrder.builder()
                .id(22L)
                .pickupPointId(222L)
                .totalPrice(999.07f)
                .status("DELIVERED")
                .build());

        Mockito.when(repository.findAll()).thenReturn(pickupPointOrders);
        PickupPointOrderController controller = new PickupPointOrderController(service);

        List<PickupPointOrderDto> outputList = new ArrayList<>();
        outputList.add(PickupPointOrderDto.builder()
                .id(1L)
                .pickupPointId(111L)
                .totalPrice(777.07f)
                .paymentType("CARD")
                .status("DELIVERED")
                .build());

        Assert.assertArrayEquals(outputList.toArray(), controller.getPickupPointOrders(111L, "delivered", "CARD").toArray());
    }
}
