package ru.yandex.market.deepdive.domain.pickup_point;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.order.OrderRepository;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PickupPointServiceTest {

    @Mock
    private PickupPointRepository mockRepository;

    @Mock
    private OrderRepository orderRepository;

    @Test
    @Transactional
    public void getPickupPoints() {
        var pickupPointService = new PickupPointService(mockRepository, orderRepository);

        List<PickupPoint> testPickupPoints = List.of(
                new PickupPoint(1L, "name1", true, 10L),
                new PickupPoint(2L, "name2", true, 20L));

        List<PickupPointDto> testPickupPointDtos = testPickupPoints.stream()
                .map(PickupPointMapper::map).collect(Collectors.toList());

        mockRepository.saveAll(testPickupPoints);
        when(mockRepository.findAll()).thenReturn(testPickupPoints);
        assertEquals(testPickupPointDtos, pickupPointService.getPickupPoints());
    }
}
