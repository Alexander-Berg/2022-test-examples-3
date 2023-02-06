package ru.yandex.market.deepdive.domain.controller;

import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

import static org.junit.Assert.assertEquals;

public class PickupPointControllerTest {

    @Test
    public void getPickupPoints() {
        PickupPointService service = Mockito.mock(PickupPointService.class);
        List<PickupPointDto> points = List.of(
                PickupPointDto.builder().id(1).name("pvz1").build(),
                PickupPointDto.builder().id(2).name("pvz2").build()
        );
        Mockito.when(service.getPickupPoints()).thenReturn(points);

        PickupPointController controller = new PickupPointController(service);
        List<PickupPointDto> result = controller.getPickupPoints();
        assertEquals(2, result.size());
        assertEquals(PickupPointDto.builder().id(1).name("pvz1").build(), result.get(0));
        assertEquals(PickupPointDto.builder().id(2).name("pvz2").build(), result.get(1));
    }
}
