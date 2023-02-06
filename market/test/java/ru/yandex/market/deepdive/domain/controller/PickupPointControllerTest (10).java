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
    private final List<PickupPoint> input = new ArrayList();
    private final List<PickupPointDto> expected = new ArrayList();
    private PickupPointRepository mockRepository = Mockito.mock(PickupPointRepository.class);
    private PickupPointController controller;

    @Before
    public void setUp() {
        input.add(PickupPoint.builder().id(1L).name("one").build());
        input.add(PickupPoint.builder().id(2L).name("two").build());
        input.add(PickupPoint.builder().id(3L).name("three").build());
        expected.add(PickupPointDto.builder().id(1).name("one").build());
        expected.add(PickupPointDto.builder().id(2).name("two").build());
        expected.add(PickupPointDto.builder().id(3).name("three").build());
        controller = new PickupPointController(new PickupPointService(mockRepository, new PickupPointMapper()));
    }

    @Test
    public void getPickupPoints() {
        Mockito.when(mockRepository.findAll()).thenReturn(input);
        Assert.assertEquals(expected, controller.getPickupPoints());
    }
}
