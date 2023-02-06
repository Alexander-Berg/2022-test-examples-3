package ru.yandex.market.deepdive.pickup_controller;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.deepdive.domain.controller.PickupPointController;
import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PickupPointControllerTest {

    @Test
    public void empty_test_point_controller() {
        var repository = Mockito.mock(PickupPointRepository.class);
        var input = new ArrayList<PickupPoint>();
        var result = new ArrayList<PickupPointDto>();

        Mockito.when(repository.findAll()).thenReturn(input);
        var service = new PickupPointService(repository, new PickupPointMapper());
        var controller = new PickupPointController(service);

        Assert.assertEquals(result, controller.getPickupPoints());
    }

    @Test
    public void test_point_controller() {
        var repository = Mockito.mock(PickupPointRepository.class);
        List<PickupPoint> input = Arrays.asList(
                new PickupPoint(1L, "first", true),
                new PickupPoint(2L, "second", false)
        );
        List<PickupPointDto> result = Arrays.asList(
                PickupPointDto.builder().id(1L).name("first").build(),
                PickupPointDto.builder().id(2L).name("second").build()
        );

        Mockito.when(repository.findAll()).thenReturn(input);
        var service = new PickupPointService(repository, new PickupPointMapper());
        var controller = new PickupPointController(service);


        Assert.assertEquals(result, controller.getPickupPoints());
    }
}
