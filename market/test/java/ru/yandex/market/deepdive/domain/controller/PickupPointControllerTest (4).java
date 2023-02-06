package ru.yandex.market.deepdive.domain.controller;

import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PickupPointControllerTest {

    @Test
    public void getPickupPoints() {
        List<PickupPoint> input = List.of(
                PickupPoint.builder().id(1L).name("test1").build(),
                PickupPoint.builder().id(2L).name("test2").build()
        );
        List<PickupPointDto> expected = List.of(
                PickupPointDto.builder().id(1L).name("test1").build(),
                PickupPointDto.builder().id(2L).name("test2").build()
        );

        PickupPointMapper mapper = new PickupPointMapper();
        PickupPointRepository repository = Mockito.mock(PickupPointRepository.class);
        Mockito.when(repository.findAll()).thenReturn(input);
        PickupPointService service = new PickupPointService(
                repository, mapper
        );
        PickupPointController controller = new PickupPointController(service);

        List<PickupPointDto> output = controller.getPickupPoints();
        assertThat(output, is(expected));
    }
}
