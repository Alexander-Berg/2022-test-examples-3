package ru.yandex.market.deepdive.domain.controller;

import java.util.LinkedList;
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
    public void controllerTest() {
        PickupPointRepository repository = Mockito.mock(PickupPointRepository.class);
        PickupPointMapper mapper = new PickupPointMapper();
        List<PickupPoint> pickupPoints = new LinkedList<>();
        PickupPoint pickupPoint = new PickupPoint();

        pickupPoint.setId(1L);
        pickupPoint.setName("p1");
        pickupPoint.setActive(true);
        pickupPoints.add(pickupPoint);
        pickupPoint.setId(2L);
        pickupPoint.setName("p2");
        pickupPoint.setActive(false);
        pickupPoints.add(pickupPoint);

        Mockito.when(repository.findAll()).thenReturn(pickupPoints);

        PickupPointService service = new PickupPointService(repository, mapper);
        PickupPointController controller = new PickupPointController(service);

        List<PickupPointDto> gottenPoints = controller.getPickupPoints();

        Assert.assertArrayEquals(pickupPoints.stream().map(mapper::map).toArray(), gottenPoints.toArray());
    }
}
