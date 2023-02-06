package ru.yandex.market.deepdive.controller;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.PickupPointController;
import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

public class PickupPointControllerTest {

    @Test
    public void getPickupPoints() {
        PickupPointService pickupPointService = Mockito.mock(PickupPointService.class);

        List<PickupPointDto> pickupPoints = List.of(
                PickupPointDto.builder().id(0).name("Name1").build(),
                PickupPointDto.builder().id(1).name("Name2").build(),
                PickupPointDto.builder().id(2).name("Name3").build()
        );
        Mockito.when(pickupPointService.getPickupPoints()).thenReturn(pickupPoints);

        PickupPointController pickupPointController = new PickupPointController(pickupPointService);

        Assert.assertEquals(pickupPoints, pickupPointController.getPickupPoints());
    }
}
