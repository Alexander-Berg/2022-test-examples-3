package ru.yandex.market.deepdive.domain.controller;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

public class PickupPointControllerTest {


    PickupPointRepository pickupPointRepository;

    @Before
    public void setUp() {
        pickupPointRepository = Mockito.mock(PickupPointRepository.class);
    }

    @Test
    public void testGetPickupPoints() {

        List<PickupPoint> data = new ArrayList<>();
        data.add(new PickupPoint(1L, "mock_1", true));
        data.add(new PickupPoint(2L, "mock_2", false));
        Mockito.when(pickupPointRepository.findAll()).thenReturn(data);
        PickupPointMapper pickupPointMapper = new PickupPointMapper();
        PickupPointService pickupPointService = new PickupPointService(pickupPointRepository, pickupPointMapper);
        PickupPointController pickupPointController = new PickupPointController(pickupPointService);
        List<PickupPointDto> expectedData = new ArrayList<>();
        expectedData.add(PickupPointDto.builder().id(1L).name("mock_1").build());
        expectedData.add(PickupPointDto.builder().id(2L).name("mock_2").build());
        Assert.assertArrayEquals(expectedData.toArray(), pickupPointController.getPickupPoints().toArray());
    }
}
