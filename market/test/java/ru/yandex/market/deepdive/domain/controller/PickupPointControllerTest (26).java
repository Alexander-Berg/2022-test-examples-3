package ru.yandex.market.deepdive.domain.controller;

import java.util.List;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

@RunWith(MockitoJUnitRunner.class)
public class PickupPointControllerTest extends TestCase {

    private final PickupPointService pickupPointService = Mockito.mock(PickupPointService.class);

    private final PickupPointController pickupPointController = new PickupPointController(pickupPointService, null);

    @Test
    public void testGetPickupPoints() {
        //given
        List<PickupPointDto> pickupPoints = List.of(
                PickupPointDto.builder()
                        .id(1)
                        .name("1")
                        .build(),
                PickupPointDto.builder()
                        .id(2)
                        .name("2")
                        .build()
        );
        Mockito.when(pickupPointService.getPickupPoints()).thenReturn(pickupPoints);

        //when
        List<PickupPointDto> result = pickupPointController.getPickupPoints();

        //then
        Assert.assertEquals(pickupPoints, result);
    }

}
