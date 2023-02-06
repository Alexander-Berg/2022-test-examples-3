package ru.yandex.market.deepdive.domain.controller;


import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;


@RunWith(MockitoJUnitRunner.class)
public class PickupPointControllerTest {

    @Mock
    PickupPointRepository pickupPointRepository;

    @Test
    public void findAll() {

        PickupPointService serv = Mockito.mock(PickupPointService.class);
        PickupPointController pickupPointController = new PickupPointController(serv);

        List<PickupPointDto> list1 = new ArrayList<>();
        list1.add(PickupPointDto.builder().id(23L).name("Some text!").build());
        list1.add(PickupPointDto.builder().id(873L).name("new Text").build());

        Mockito.when(pickupPointController.getPickupPoints()).thenReturn(list1);

        Assert.assertEquals(pickupPointController.getPickupPoints().get(0).getName(), "Some text!");
        Assert.assertEquals(pickupPointController.getPickupPoints(), list1);
    }

    @Test
    public void findById() {
        PickupPoint p1 = new PickupPoint(236L, "Some text 1", true);

        Mockito.when(pickupPointRepository.findById(236L)).thenReturn(java.util.Optional.of(p1));

        Assert.assertEquals(pickupPointRepository.findById(236L).get().getName(), "Some text 1");
    }
}
