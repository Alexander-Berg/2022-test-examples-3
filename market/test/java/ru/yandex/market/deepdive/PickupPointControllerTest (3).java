package ru.yandex.market.deepdive;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepdive.domain.controller.PickupPointController;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

public class PickupPointControllerTest {
    @Test
    public void compareResults() {
        PickupPointController pickupPointController = Mockito.mock(PickupPointController.class);
        PickupPointService pickupPointService = Mockito.mock(PickupPointService.class);
        Assert.assertEquals(pickupPointService.getPickupPoints(), pickupPointController.getPickupPoints());
    }
}
