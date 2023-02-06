package ru.yandex.market.delivery.mdbapp.integration.payload;

import org.junit.Assert;
import org.junit.Test;
import steps.LocationSteps;

import ru.yandex.market.delivery.mdbapp.components.geo.Location;

public class ExtendedOrderDataTest {
    ExtendedOrder.OrderData orderData = new ExtendedOrder.OrderData();

    @Test
    public void getAndSetLocationFromTest() {
        Location locationFrom = LocationSteps.getLocation();
        orderData.setLocationFrom(locationFrom);

        Assert.assertEquals("Unexpected locationFrom", locationFrom, orderData.getLocationFrom());
    }

    @Test
    public void getAndSetLocationToTest() {
        Location locationTo = LocationSteps.getLocation();
        orderData.setLocationTo(locationTo);

        Assert.assertEquals("Unexpected locationTo", locationTo, orderData.getLocationTo());
    }
}
