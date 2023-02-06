package ru.yandex.market.deepdive.domain.pickup_point;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.order.OrderMapper;
import ru.yandex.market.deepdive.domain.order.OrderRepository;

import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class PickupPointServiceTest {
    @Mock
    private PickupPointRepository pickupPointRepository;

    @Mock
    private OrderRepository orderRepository;

    @Test
    @Transactional
    public void getPickupPointsTest() {
        List<PickupPoint> pickupPoints = List.of(
                PickupPoint.builder()
                        .id(1L)
                        .name("first")
                        .build(),
                PickupPoint.builder()
                        .id(2L)
                        .name("second")
                        .build(),
                PickupPoint.builder()
                        .id(3L)
                        .name("third")
                        .build());
        pickupPointRepository.saveAll(pickupPoints);
        PickupPointMapper pickupPointMapper = new PickupPointMapper();
        OrderMapper orderMapper = new OrderMapper();
        PickupPointService pickupPointService = new PickupPointService(
                pickupPointRepository, orderRepository, pickupPointMapper, orderMapper);
        List<PickupPointDto> pickupPointDtos = pickupPoints.stream()
                .map(pickupPointMapper::map)
                .collect(Collectors.toList());
        when(pickupPointRepository.findAll()).thenReturn(pickupPoints);
        List<PickupPointDto> resultPoints = pickupPointService.getPickupPoints();
        Assert.assertEquals(pickupPointDtos, resultPoints);
    }

}
