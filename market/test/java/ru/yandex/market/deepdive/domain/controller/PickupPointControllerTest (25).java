package ru.yandex.market.deepdive.domain.controller;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

public class PickupPointControllerTest {
    @Test
    public void controllerTest() throws Exception {
        PickupPointService service = Mockito.mock(PickupPointService.class);
        PickupPointController pickupPointController = new PickupPointController(service);

        List<PickupPointDto> list = new ArrayList<>();
        list.add(PickupPointDto.builder().id(1).name("test").build());

        Mockito.when(service.getPickupPoints()).thenReturn(list);

        Assert.assertEquals(pickupPointController.getPickupPoints(), list);
    }
}

