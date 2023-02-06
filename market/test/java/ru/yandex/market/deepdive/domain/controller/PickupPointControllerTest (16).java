package ru.yandex.market.deepdive.domain.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;


@RunWith(MockitoJUnitRunner.class)
public class PickupPointControllerTest {

    PickupPointController pickupPointController;

    PickupPointService pickupPointService;

    PickupPointRepository pickupPointRepository;

    PickupPointMapper pickupPointMapper;

    private List<PickupPoint> pickupPoints = new ArrayList<>();

    @Before
    public void setUp() {
        pickupPointMapper = new PickupPointMapper();
        pickupPointRepository = Mockito.mock(PickupPointRepository.class);
        pickupPoints = List.of(
                PickupPoint.builder().name("John").id(1L).active(true).build(),
                PickupPoint.builder().name("Mike").id(2L).active(false).build(),
                PickupPoint.builder().name("Sarah").id(99L).active(true).build()
        );

        Mockito.when(pickupPointRepository.findAll()).thenReturn(pickupPoints);

        pickupPointService = new PickupPointService(pickupPointRepository, pickupPointMapper);
        pickupPointController = new PickupPointController(pickupPointService);
    }

    @Test
    public void getPickupPoints() {
        var expected = pickupPoints
                .stream()
                .map(pickupPointMapper::map)
                .collect(Collectors.toList());

        List<PickupPointDto> controllerResponse = pickupPointController.getPickupPoints();

        Assert.assertEquals(expected, controllerResponse);
    }
}
