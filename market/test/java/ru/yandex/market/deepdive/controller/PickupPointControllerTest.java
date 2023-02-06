package ru.yandex.market.deepdive.controller;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


import org.mockito.Mockito;


import ru.yandex.market.deepdive.domain.controller.PickupPointController;
import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

import java.util.Arrays;
import java.util.List;


public class PickupPointControllerTest {


    PickupPointRepository pickupPointRepository;

    PickupPointService pickupPointService;

    List<PickupPoint> pickupPoints;

    @Before
    public void init() {
        pickupPoints = Arrays.asList(
                new PickupPoint(1L, "first", true ),
                new PickupPoint(2L, "second", false )
        );
        pickupPointRepository = Mockito.mock(PickupPointRepository.class);
        pickupPointService = new PickupPointService(pickupPointRepository, new PickupPointMapper());
        Mockito.when(pickupPointRepository.findAll()).thenReturn(pickupPoints);
    }

    @Test
    public void getPickupPointsTest() {
        List<PickupPointDto> pickupPointDtos = Arrays.asList(
                PickupPointDto.builder().id(1L).name("first").build(),
                PickupPointDto.builder().id(2L).name("second").build()
                );
        PickupPointController pickupPointController = new PickupPointController(pickupPointService);
        Assert.assertEquals(pickupPointDtos, pickupPointController.getPickupPoints());

    }





}
