package ru.yandex.market.deepdive;

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.PickupPointController;
import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

public class PickupPointControllerTest {
    @Test
    public void testing() {
        PickupPointService pickupPointService = Mockito.mock(PickupPointService.class);
//sample comment just to activate nanny and tsum
        List<PickupPointDto> list = new LinkedList<>();
        list.add(PickupPointDto.builder().id(0).name("Hello").build());
        list.add(PickupPointDto.builder().id(1).name("world").build());
        list.add(PickupPointDto.builder().id(2).name("").build());

        PickupPointController pickupPointController = new PickupPointController(pickupPointService);

        Mockito.when(pickupPointService.getPickupPoints()).thenReturn(list);

        Assert.assertEquals(pickupPointController.getPickupPoints(), list);
    }
}
