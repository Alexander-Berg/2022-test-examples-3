package ru.yandex.market.deepdive.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.PickupPointController;
import ru.yandex.market.deepdive.domain.order.OrderMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;


public class PickupPointControllerTest {

    @Test
    public void getPickupPoints() {

        List<PickupPoint> points = new ArrayList<>();

        points.add(PickupPoint.builder().id(1L).name("sasha").active(false).build());
        points.add(PickupPoint.builder().id(2L).name("anton").active(false).build());
        points.add(PickupPoint.builder().id(3L).name("ilya").active(true).build());

        PickupPointRepository repository = Mockito.mock(PickupPointRepository.class);
        Mockito.when(repository.findAll()).thenReturn(points);
        PickupPointMapper mapper = Mockito.mock(PickupPointMapper.class);
        OrderMapper orderMapper = Mockito.mock(OrderMapper.class);
        PickupPointService service = new PickupPointService(repository, mapper);
        PickupPointController controller = new PickupPointController(service);

        Assert.assertEquals(points.stream()
                        .map(mapper::map)
                        .collect(Collectors.toList()),
                controller.getPickupPoints());
    }
}
