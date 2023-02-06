package ru.yandex.market.deepdive.domain.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

public class PickupPointControllerTest {
    @Test
    public void pickupPointControllerTest_01() {
        PickupPointService pickupPointService = Mockito.mock(PickupPointService.class);

        List<PickupPointDto> list = Stream.of(
                        PickupPointDto.builder().id(228L).name("Eugene1"),
                        PickupPointDto.builder().id(228228L).name("Eugene2"),
                        PickupPointDto.builder().id(228228228L).name("Eugene3")
                )
                .map(PickupPointDto.PickupPointDtoBuilder::build)
                .collect(Collectors.toCollection(ArrayList::new));

        PickupPointController pickupPointController = new PickupPointController(pickupPointService);

        Mockito.when(pickupPointService.getPickupPoints())
                .thenReturn(list);

        Assert.assertEquals(pickupPointController.getPickupPoints(), list);
    }

    @Test
    public void pickupPointControllerTest_02() {
        Assert.assertEquals("I will be back", new StringBuilder("kcab eb lliw I").reverse().toString());
        Assert.assertNotEquals(0, 1);
    }
}
