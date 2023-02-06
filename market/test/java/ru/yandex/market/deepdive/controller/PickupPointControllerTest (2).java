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

        List<PickupPointDto> ppdList = new ArrayList<>();
        ppdList.add(PickupPointDto.builder().id(0).name("Name0").build());
        ppdList.add(PickupPointDto.builder().id(1).name("Name1").build());
        ppdList.add(PickupPointDto.builder().id(2).name("Name2").build());
        ppdList.add(PickupPointDto.builder().id(3).name("Name3").build());

        Mockito.when(pickupPointService.getPickupPoints()).thenReturn(ppdList);

        PickupPointController pickupPointController = new PickupPointController(pickupPointService);
        Assert.assertEquals(ppdList, pickupPointController.getPickupPoints());

    }
}
