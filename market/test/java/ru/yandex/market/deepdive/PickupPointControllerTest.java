package ru.yandex.market.deepdive;


import java.util.LinkedList;

import ru.yandex.market.deepdive.domain.controller.PickupPointController;
import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class PickupPointControllerTest {
    @Test
    public void test() {
        PickupPointService pps = Mockito.mock(PickupPointService.class);
        LinkedList<PickupPointDto> list = new LinkedList<>();
        list.add(PickupPointDto.builder().id(1).name("Moscow").build());
        list.add(PickupPointDto.builder().id(2).name("Paris").build());
        list.add(PickupPointDto.builder().id(3).name("New York").build());

        Mockito.when(pps.getPickupPoints()).thenReturn(list);

        PickupPointController ppc = new PickupPointController(pps);

        Assert.assertEquals(list, ppc.getPickupPoints());
    }

    @Test
    public void dummyTest() {
        Assert.assertTrue(true);
    }
}
