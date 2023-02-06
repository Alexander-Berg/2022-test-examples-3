package ru.yandex.market.deepdive.domain.controller;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

@RunWith(MockitoJUnitRunner.class)
public class PickupPointControllerTest {
    @Test
    public void getPickupPointsEmpty() {
        List<PickupPoint> input = new ArrayList<>();
        List<PickupPointDto> response = new ArrayList<>();

        PickupPointRepository repository = Mockito.mock(PickupPointRepository.class);
        PickupPointMapper mapper = new PickupPointMapper();
        Mockito.when(repository.findAll()).thenReturn(input);

        PickupPointService service = new PickupPointService(repository, mapper);
        PickupPointController controller = new PickupPointController(service);

        Assert.assertArrayEquals(response.toArray(), controller.getPickupPoints().toArray());
    }

    @Test
    public void getPickupPoints() {
        List<PickupPoint> input = new ArrayList<>();
        input.add(PickupPoint.builder().id(1L).name("First").active(true).build());
        input.add(PickupPoint.builder().id(2L).name("Second").active(true).build());

        List<PickupPointDto> response = new ArrayList<>();
        response.add(PickupPointDto.builder().id(1L).name("First").build());
        response.add(PickupPointDto.builder().id(2L).name("Second").build());

        PickupPointRepository repository = Mockito.mock(PickupPointRepository.class);
        PickupPointMapper mapper = Mockito.mock(PickupPointMapper.class);
        Mockito.when(repository.findAll()).thenReturn(input);

        for (int i = 0; i < input.size(); i++) {
            Mockito.when(mapper.map(input.get(i))).thenReturn(response.get(i));
        }

        PickupPointService service = new PickupPointService(repository, mapper);
        PickupPointController controller = new PickupPointController(service);

        Assert.assertArrayEquals(response.toArray(), controller.getPickupPoints().toArray());

    }
}
