package ru.yandex.market.deepdive.domain.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

import static org.junit.Assert.assertEquals;

public class PickupPointControllerTest {


    @Test
    public void testGetPickupPoints() {
        List<PickupPoint> mockList = new ArrayList<>(3);
        mockList.add(PickupPoint.builder().id(1L).name("Pick-one").build());
        mockList.add(PickupPoint.builder().id(2L).name("Pick-two").build());
        mockList.add(PickupPoint.builder().id(10L).name("Pick-ten").build());

        PickupPointRepository repository = Mockito.mock(PickupPointRepository.class);
        PickupPointMapper mapper = Mockito.mock(PickupPointMapper.class);
        Mockito.when(repository.findAll()).thenReturn(mockList);

        PickupPointController testController = new PickupPointController(new PickupPointService(repository, mapper));

        assertEquals(mockList.stream().map(mapper::map).collect(Collectors.toList()), testController.getPickupPoints());
    }

}
