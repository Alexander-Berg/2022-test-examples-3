package ru.yandex.market.deepdive.domain.controller;

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
    //yo
    @Test
    public void getPickUpPointsTest() {
        List<PickupPoint> methodInput = List.of(
                PickupPoint.builder().id(1L).name("Anton").active(false).build(),
                PickupPoint.builder().id(2L).name("Ilya").active(true).build(),
                PickupPoint.builder().id(3L).name("Sasha").active(false).build(),
                PickupPoint.builder().id(4L).name("Inom").active(true).build()
        );

        List<PickupPointDto> methodResult = List.of(
                PickupPointDto.builder().id(1L).name("Anton").build(),
                PickupPointDto.builder().id(2L).name("Ilya").build(),
                PickupPointDto.builder().id(3L).name("Sasha").build(),
                PickupPointDto.builder().id(4L).name("Inom").build()
        );

        PickupPointRepository mockPickupPointRepository = Mockito.mock(PickupPointRepository.class);
        PickupPointMapper mockPickupPointMapper = Mockito.mock(PickupPointMapper.class);
        Mockito.when(mockPickupPointRepository.findAll()).thenReturn(methodInput);

        for (int i = 0; i < methodInput.size(); i++) {
            Mockito.when(mockPickupPointMapper.map(methodInput.get(i))).thenReturn(methodResult.get(i));
        }

        PickupPointService pickupPointService = new PickupPointService(mockPickupPointRepository,
                mockPickupPointMapper);
        PickupPointController pickupPointController = new PickupPointController(pickupPointService);

        Assert.assertEquals(methodResult, pickupPointController.getPickupPoints());
        Assert.assertEquals(1, 1);
    }
}
