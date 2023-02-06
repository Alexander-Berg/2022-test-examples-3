package ru.yandex.market.deepdive.domain.controller;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PickupPointControllerTest {
    @InjectMocks
    private PickupPointController pickupPointController;

    @Mock
    private PickupPointService pickupPointService;

    private List<PickupPointDto> pickupPointDtos;

    @Before
    public void beforeTests() {
        pickupPointDtos = List.of(PickupPointDto.builder().id(1L).name("yandex").build());
        when(pickupPointService.getPickupPoints()).thenReturn(pickupPointDtos);
    }

    @Test
    public void getPickupPoints() {
        assertEquals(pickupPointController.getPickupPoints(), pickupPointService.getPickupPoints());
    }
}
