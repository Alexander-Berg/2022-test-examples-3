package ru.yandex.market.deepdive.controller;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.PickupPointController;
import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

public class PickupPointControllerTest {

    @Test
    public void test() {
        PickupPointService pickupPointService = Mockito.mock(PickupPointService.class);
        PickupPointController pickupPointController = new PickupPointController(pickupPointService);

        List<PickupPointDto> pickupPoints = new ArrayList<>();
        pickupPoints.add(PickupPointDto.builder().id(1L).name("Test").build());
        pickupPoints.add(PickupPointDto.builder().id(2L).name("Something").build());
        pickupPoints.add(PickupPointDto.builder().id(3L).name("Another").build());

        Mockito.when(pickupPointController.getPickupPoints()).thenReturn(pickupPoints);

        Assert.assertEquals(pickupPointController.getPickupPoints(), pickupPoints);
    }
}
