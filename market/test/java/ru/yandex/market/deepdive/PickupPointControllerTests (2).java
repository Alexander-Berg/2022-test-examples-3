package ru.yandex.market.deepdive;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.market.deepdive.domain.controller.PickupPointController;
import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest
public class PickupPointControllerTests {
    @Mock
    PickupPointService mockService;

    @Test
    public void pickupPointControllerTest() {
        List<PickupPointDto> pointDtoList = List.of(
                PickupPointDto.builder().id(1L).name("qwe").build(),
                PickupPointDto.builder().id(2L).name("asd").build(),
                PickupPointDto.builder().id(3L).name("zxc").build()
        );

        Mockito.when(mockService.getPickupPoints()).thenReturn(pointDtoList);

        PickupPointController controller = new PickupPointController(mockService);

        Assert.assertEquals(controller.getPickupPoints(), pointDtoList);
    }
}
