package ru.yandex.market.deepdive.domain.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PickupPointServiceTest {

    @Mock
    private PickupPointRepository pickupPointRepository;
    @Test
    @Transactional
    public void getPickupPointsTest() {
        List<PickupPoint> pickupPoints = List.of(
                new PickupPoint(1L, "first", true, new ArrayList<>()),
                new PickupPoint(2L, "second", false, new ArrayList<>()),
                new PickupPoint(3L, "third", true, new ArrayList<>()));
        pickupPointRepository.saveAll(pickupPoints);
        PickupPointService pickupPointService = new PickupPointService(pickupPointRepository);
        List<PickupPointDto> pickupPointDtos = pickupPoints.stream()
                .map(PickupPointMapper::map)
                .collect(Collectors.toList());
        when(pickupPointRepository.findAll()).thenReturn(pickupPoints);
        List<PickupPointDto> resultPoints = pickupPointService.getPickupPoints();
        Assert.assertEquals(pickupPointDtos, resultPoints);
    }
}
