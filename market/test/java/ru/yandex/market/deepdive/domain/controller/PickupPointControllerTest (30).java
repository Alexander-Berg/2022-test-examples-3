package ru.yandex.market.deepdive.domain.controller;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;


public class PickupPointControllerTest {

    @Test
    public void getPickupPoints() {
        PickupPointService pickupPointService = Mockito.mock(PickupPointService.class);
        PickupPointController pickupPointController = new PickupPointController(pickupPointService);

        List<PickupPointDto> pps = List.of(PickupPointDto.builder().id(1L).name("sisco").build());

        Mockito.when(pickupPointService.getPickupPoints()).thenReturn(pps);

        Assert.assertEquals(pickupPointController.getPickupPoints(), pps);


    }
}
