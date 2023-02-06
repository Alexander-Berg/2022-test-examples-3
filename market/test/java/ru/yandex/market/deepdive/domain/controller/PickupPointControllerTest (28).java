package ru.yandex.market.deepdive.domain.controller;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class PickupPointControllerTest {

    @Mock
    private PickupPointService pickupPointService;

    @InjectMocks
    private PickupPointController underTest;

    @Test
    public void getPickupPoints() {

        List<PickupPointDto> given = List.of(
                PickupPointDto.builder().id(1L).name("yandex").build(),
                PickupPointDto.builder().id(2L).name("yandex").build());

        given(pickupPointService.getPickupPoints()).willReturn(given);

        List<PickupPointDto> actual = underTest.getPickupPoints();

        Assert.assertEquals(given, actual);
        Assert.assertNotNull(actual);
    }
}
