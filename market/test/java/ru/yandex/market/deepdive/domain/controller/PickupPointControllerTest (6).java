package ru.yandex.market.deepdive.domain.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

import static org.assertj.core.api.Assertions.assertThat;

public class PickupPointControllerTest {

    @Test
    public void getPickupPoints() {
        List<PickupPoint> points = List.of(
                PickupPoint.builder()
                        .id(0L)
                        .name("Test point 1")
                        .active(true)
                        .build(),
                PickupPoint.builder()
                        .id(1L)
                        .name("Test point 2")
                        .active(false)
                        .build(),
                PickupPoint.builder()
                        .id(2L)
                        .name("Test point 3")
                        .active(true)
                        .build()
        );
        PickupPointRepository pointRepository = Mockito.mock(PickupPointRepository.class);
        Mockito.when(pointRepository.findAll()).thenReturn(points);
        PickupPointMapper pointMapper = new PickupPointMapper();
        PickupPointController pointController = new PickupPointController(
                new PickupPointService(pointRepository, pointMapper)
        );
        List<PickupPointDto> expectedPoints = points.stream()
                .map(pointMapper::map)
                .collect(Collectors.toList());
        List<PickupPointDto> actualPoints = pointController.getPickupPoints();
        assertThat(actualPoints).isEqualTo(expectedPoints);
    }
}
