package ru.yandex.market.core.outlet.geo;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(value = Parameterized.class)
public class GpsPointLocationTest {

    private final static Double delta = 0.00000001;
    private String locationString;
    private Double latitude;
    private Double longitude;

    public GpsPointLocationTest(String locationString, Double longitude, Double latitude) {
        this.locationString = locationString;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"37.593291 55.747439", 37.593291, 55.747439},
                {"37.593291,55.747439", 37.593291, 55.747439},
                {"37.593291, 55.747439", 37.593291, 55.747439},
                {" 37.593291 , 55.747439 ", 37.593291, 55.747439},
                {"37.593291", null, null},
                {"37.593291 , hg , 55.747439 ", null, null},
                {"", null, null},
                {"olololo,ogogogo", null, null},
                {null, null, null},
        });
    }

    @Test
    public void setLocationTest() throws Exception {
        GpsPointLocation gpsPointLocation = GpsPointLocation.parseLocation(locationString);
        if (latitude != null && longitude != null) {
            Assert.assertNotNull(gpsPointLocation);
            Assert.assertEquals(latitude, gpsPointLocation.getLatitude(), delta);
            Assert.assertEquals(longitude, gpsPointLocation.getLongitude(), delta);
        } else {
            Assert.assertNull(gpsPointLocation);
        }
    }

}
