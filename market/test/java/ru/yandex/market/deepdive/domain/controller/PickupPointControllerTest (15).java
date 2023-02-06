package ru.yandex.market.deepdive.domain.controller;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;


public class PickupPointControllerTest {

    @Test
    public void getPickUpPointsTest() {
        PickupPointRepository repository = Mockito.mock(PickupPointRepository.class);
        PickupPointService service = new PickupPointService(repository, new PickupPointMapper());

        List<PickupPoint> pickupPoints = new ArrayList<>();
        pickupPoints.add(PickupPoint.builder()
                .id(1L)
                .name("first")
                .active(false)
                .build());
        pickupPoints.add(PickupPoint.builder()
                .id(22L)
                .name("twenty two")
                .active(true)
                .build());

        Mockito.when(repository.findAll()).thenReturn(pickupPoints);
        PickupPointController controller = new PickupPointController(service);

        List<PickupPointDto> outputList = new ArrayList<>();
        outputList.add(PickupPointDto.builder()
                .id(1L)
                .name("first")
                .build());
        outputList.add(PickupPointDto.builder()
                .id(22L)
                .name("twenty two")
                .build());

        Assert.assertArrayEquals(outputList.toArray(), controller.getPickupPoints().toArray());
    }
}
