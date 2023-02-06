package ru.yandex.market.deepdive.domain.controller;

import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.order_report.OrderReportMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PickupPointControllerTest {
    @Test
    public void getPickupPoints() {
        List<PickupPoint> input = List.of(
                PickupPoint.builder().id(1L).name("test1").active(true).build(),
                PickupPoint.builder().id(2L).name("test2").active(true).build()
        );
        List<PickupPointDto> expected = List.of(
                PickupPointDto.builder().id(1L).name("test1").active(true).build(),
                PickupPointDto.builder().id(2L).name("test2").active(true).build()
        );

        PickupPointMapper mapper = new PickupPointMapper();
        OrderReportMapper orderReportMapper = new OrderReportMapper();
        PickupPointRepository repository = Mockito.mock(PickupPointRepository.class);
        Mockito.when(repository.findAll()).thenReturn(input);
        PickupPointService service = new PickupPointService(
                repository, mapper, orderReportMapper
        );
        PickupPointController controller = new PickupPointController(service);

        List<PickupPointDto> output = controller.getPickupPoints();
        assertThat(output, is(expected));
    }
}
