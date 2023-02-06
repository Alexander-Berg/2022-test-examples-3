package ru.yandex.market.api.internal.report.parsers.json;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.internal.report.data.DeliveryAvailable;
import ru.yandex.market.api.util.ResourceHelpers;

import java.math.BigDecimal;

import static ru.yandex.market.api.matchers.NearestRegionMatcher.*;

/**
 * Created by fettsery on 30.01.19.
 */
public class DeliveryAvailabilityJsonParserTest {
    @Test
    public void testParseWithourNearestRegions() {
        DeliveryAvailabilityJsonParser parser = new DeliveryAvailabilityJsonParser();

        DeliveryAvailable deliveryAvailable = parser.parse(ResourceHelpers.getResource("delivery_available.json"));

        Assert.assertTrue(deliveryAvailable.getDeliveryAvailable());
        Assert.assertEquals("Ленский район, Архангельская область, Россия", deliveryAvailable.getSubtitle());
        Assert.assertNull(deliveryAvailable.getNearestRegions());
    }

    @Test
    public void testParseWithNearestRegions() {
        DeliveryAvailabilityJsonParser parser = new DeliveryAvailabilityJsonParser();

        DeliveryAvailable deliveryAvailable = parser.parse(ResourceHelpers.getResource("delivery_not_available.json"));

        Assert.assertFalse(deliveryAvailable.getDeliveryAvailable());

        Assert.assertEquals("Ленский район, Архангельская область, Россия", deliveryAvailable.getSubtitle());

        Assert.assertThat(
            deliveryAvailable.getNearestRegions(),
            Matchers.contains(
                nearestRegion(
                    regionId(124822),
                    distanceKm(new BigDecimal("17.53759008")),
                    courierAvailable(false),
                    pickupAvailable(false),
                    postAvailable(true),
                    subtitle("Ленский район, Архангельская область, Россия")
                ),
                nearestRegion(
                    regionId(148061),
                    distanceKm(new BigDecimal("30.01705314")),
                    courierAvailable(false),
                    pickupAvailable(false),
                    postAvailable(true),
                    subtitle("Ленский район, Архангельская область, Россия")
                )
            )
        );


    }
}
