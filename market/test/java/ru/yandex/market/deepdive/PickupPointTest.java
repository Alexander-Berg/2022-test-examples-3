package ru.yandex.market.deepdive;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.PickupPointController;
import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

public class PickupPointTest {

    private PickupPointService service;
    private PickupPointController controller;
    private PickupPointRepository repository;
    private List<PickupPoint> points;

    @Before
    public void init() {
        points = Arrays.asList(
                new PickupPoint(1L, "first", false),
                new PickupPoint(2L, "second", true)
        );

        repository = Mockito.mock(PickupPointRepository.class);
        Mockito.when(repository.findAll()).thenReturn(points);

        service = new PickupPointService(repository, new PickupPointMapper());
        controller = new PickupPointController(service);
    }

    @Test
    public void GetPickUppointsTest() {
        List<PickupPointDto> dtos = Arrays.asList(
                PickupPointDto.builder().id(1L).name("first").build(),
                PickupPointDto.builder().id(2L).name("second").build()
        );
        Assert.assertEquals(dtos, controller.getPickupPoints());

        Assert.assertEquals(dtos.size(), controller.getPickupPoints().size()); // Just to test pull request creation
    }
}
