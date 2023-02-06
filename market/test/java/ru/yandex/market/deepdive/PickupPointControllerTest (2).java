package ru.yandex.market.deepdive;

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
        PickupPointService service = Mockito.mock(PickupPointService.class);
        List<PickupPointDto> list = new ArrayList<>();
        list.add(PickupPointDto.builder().id(1L).name("Alive").build());
        list.add(PickupPointDto.builder().id(2L).name("Bob").build());
        Mockito.when(service.getPickupPoints()).thenReturn(list);

        PickupPointController controller = new PickupPointController(service);
        Assert.assertEquals(controller.getPickupPoints(), list);
    }
}
