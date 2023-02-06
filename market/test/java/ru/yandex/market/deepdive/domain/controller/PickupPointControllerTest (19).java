package ru.yandex.market.deepdive.domain.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import ru.yandex.market.deepdive.domain.controller.dto.PickupPointDto;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;


@RunWith(MockitoJUnitRunner.class)
public class PickupPointControllerTest {

    PickupPointController pickupPointController;

    PickupPointService pickupPointService;

    PickupPointRepository pickupPointRepository;

    PickupPointMapper pickupPointMapper;

    private List<PickupPoint> pickupPointDtoList = new ArrayList<>();

    private AutoCloseable autoCloseable;

    @Before
    public void setUp() {
        pickupPointMapper = new PickupPointMapper();
        pickupPointRepository = Mockito.mock(PickupPointRepository.class);
        pickupPointDtoList = List.of(
                PickupPoint.builder().name("John").id(1L).active(true).build(),
                PickupPoint.builder().name("Mike").id(2L).active(false).build(),
                PickupPoint.builder().name("Sarah").id(99L).active(true).build()
        );

        Mockito.when(pickupPointRepository.findAll()).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return pickupPointDtoList;
            }
        });

        pickupPointService = new PickupPointService(pickupPointRepository, pickupPointMapper);
        pickupPointController = new PickupPointController(pickupPointService);
        autoCloseable = MockitoAnnotations.openMocks(this);
    }

    @Test
    public void getPickupPoints() {
        var expected = pickupPointDtoList
                .stream()
                .map(pickupPointMapper::map)
                .collect(Collectors.toList());

        List<PickupPointDto> controllerResponse = pickupPointController.getPickupPoints();

        Assert.assertEquals(expected, controllerResponse);
    }

    @After
    public void tearDown() throws Exception {
        autoCloseable.close();
    }
}
