package ru.yandex.market.deepdive.domain.controller;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;



@RunWith(MockitoJUnitRunner.class)
public class PickupPointControllerTest {

    @Mock
    PickupPointRepository repository;

    @Test
    public void getPickUpPointsTest() {
        PickupPointMapper mapper = new PickupPointMapper();
        PickupPointService service = new PickupPointService(repository, mapper);
        List<PickupPoint> pickupPointList = new ArrayList<>();
        pickupPointList.add(PickupPoint.builder().id(0L).name("foo").active(false).build());
        pickupPointList.add(PickupPoint.builder().id(1L).name("bar").active(false).build());
        pickupPointList.add(PickupPoint.builder().id(2L).name("gee").active(true).build());
        Mockito.when(repository.findAll()).thenReturn(pickupPointList);
        PickupPointController controller = new PickupPointController(service);
        Assert.assertArrayEquals(
                pickupPointList.stream().map(mapper::map).toArray(),
                controller.getPickupPoints().toArray());
    }
}
