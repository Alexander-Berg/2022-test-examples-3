package ru.yandex.market.deepdive.domain.controller;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

public class PickupPointControllerTest {

    @Test
    public void test() {
        PickupPointService pickupPointService = Mockito.mock(PickupPointService.class);
        PickupPointController pickupPointController = new PickupPointController(pickupPointService);

        List<PickupPointDto> data = List.of(PickupPointDto.builder().id(2).name("word").build());
        Mockito.when(pickupPointService.getPickupPoints()).thenReturn(data);
        Assert.assertEquals(data, pickupPointController.getPickupPoints());
    }
}
