package ru.yandex.market.deepdive.domain.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

public class PickupPointControllerTest {

    @Test
    public void testController() {
        PickupPointRepository repository = Mockito.mock(PickupPointRepository.class);

        List<PickupPoint> points = new ArrayList<>();

        points.add(PickupPoint.builder().id(1L).name("First person").active(true).build());
        points.add(PickupPoint.builder().id(2L).name("Second person").active(false).build());
        points.add(PickupPoint.builder().id(3L).name("Third person").active(true).build());

        Mockito.when(repository.findAll()).thenReturn(points);

        PickupPointMapper mapper = new PickupPointMapper();

        PickupPointService service = new PickupPointService(repository, mapper);

        PickupPointController controller = new PickupPointController(service);

        Assert.assertEquals(points.stream()
                        .map(mapper::map)
                        .collect(Collectors.toList()),
                controller.getPickupPoints());
    }
}
