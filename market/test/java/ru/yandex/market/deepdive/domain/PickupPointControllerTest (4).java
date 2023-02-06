package ru.yandex.market.deepdive.domain;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.PickupPointController;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

public class PickupPointControllerTest {

    @Test
    public void getPickupPoints() {
        PickupPointRepository pickupPointRepository = Mockito.mock(PickupPointRepository.class);
        PickupPointMapper pickupPointMapper = new PickupPointMapper();
        PickupPointService pickupPointService = new PickupPointService(pickupPointRepository, pickupPointMapper);
        PickupPointController pickupPointController = new PickupPointController(pickupPointService);

        PickupPoint pickupPoint = PickupPoint.builder().id(0L).name("kek").active(true).pvzMarketId(0L).build();
        List<PickupPoint> pickupPoints = List.of(pickupPoint);

        Mockito.when(pickupPointRepository.findAll()).thenReturn(pickupPoints);

        Assert.assertEquals(
                pickupPointController.getPickupPoints(),
                pickupPoints.stream().map(pickupPointMapper::map).collect(Collectors.toList())
        );

    }
}
