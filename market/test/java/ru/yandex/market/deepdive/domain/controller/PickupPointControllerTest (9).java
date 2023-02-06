package ru.yandex.market.deepdive.domain.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

public class PickupPointControllerTest {

    @Test
    public void getPickupPoints() {

        List<PickupPoint> pickupPointList = new ArrayList<>(List.of(
                PickupPoint.builder().id(1L).name("first").active(true).build(),
                PickupPoint.builder().id(2L).name("second").active(true).build(),
                PickupPoint.builder().id(3L).name("third").active(true).build())
        );

        PickupPointRepository repository = Mockito.mock(PickupPointRepository.class);
        Mockito.when(repository.findAll()).thenReturn(pickupPointList);
        PickupPointMapper mapper = new PickupPointMapper();
        PickupPointService service = new PickupPointService(repository, mapper);
        PickupPointController controller = new PickupPointController(service);

        Assertions.assertEquals(
                controller.getPickupPoints(),
                pickupPointList.stream().map(mapper::map).collect(Collectors.toList())
        );
    }
}
