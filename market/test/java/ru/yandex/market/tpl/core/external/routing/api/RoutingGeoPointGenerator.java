package ru.yandex.market.tpl.core.external.routing.api;

import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.RandomUtils;

/**
 * @author ungomma
 */
@UtilityClass
public class RoutingGeoPointGenerator {

    public static RoutingGeoPoint generateLonLat() {
        return RoutingGeoPoint.ofLatLon(generateLatitude(), generateLongitude());
    }

    public static BigDecimal generateLongitude() {
        return generateCoordinate(37.371169, 37.843106);
    }

    public static BigDecimal generateLatitude() {
        return generateCoordinate(55.572341, 55.909792);
    }

    private static BigDecimal generateCoordinate(double from, double to) {
        return BigDecimal.valueOf(RandomUtils.nextDouble(from, to))
                .setScale(RoutingGeoPoint.GEO_POINT_SCALE, RoundingMode.HALF_UP);
    }

}
