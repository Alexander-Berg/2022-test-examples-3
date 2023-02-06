package ru.yandex.market.deepdive.domain.pickup_point;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;


public class PickupPointServiceTest {

    @Test
    public void getPickupPoints() {

        List<PickupPoint> list = new ArrayList<>();
        list.add(new PickupPoint(1L, "katejud", true, new ArrayList<>()));
        list.add(new PickupPoint(2L, "katejud2", false, new ArrayList<>()));

        List<PickupPointDto> listDto = list.stream().map(PickupPointMapper::map)
                .collect(Collectors.toList());

        PickupPointRepository pickupPointRepository = Mockito.mock(PickupPointRepository.class);
        Mockito.when(pickupPointRepository.findAll()).thenReturn(list);


        PickupPointService pickupPointService = new PickupPointService(pickupPointRepository);
        Assert.assertEquals(listDto, pickupPointService.getPickupPoints());
    }
}
