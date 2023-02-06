package ru.yandex.market.deepdive;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.deepdive.domain.controller.PickupPointController;
import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class PickupPointControllerTest {
    @Mock
    private PickupPointService service;

    @Test
    public void getPickupPointsTest() {
        var points =  List.of(
                PickupPointDto.builder().id(1L).name("qwe1").build(),
                PickupPointDto.builder().id(2L).name("qwe2").build()
        );

        when(service.getPickupPoints()).thenReturn(points);

        var controller = new PickupPointController(service);
        var list = controller.getPickupPoints();
        Assert.assertEquals(points, list);
    }

}
