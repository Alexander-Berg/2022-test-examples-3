package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.domain.v2.NearestRegion;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.is;
import static ru.yandex.market.api.ApiMatchers.map;

/**
 * Created by fettsery on 30.01.19.
 */
public class NearestRegionMatcher {
    public static Matcher<NearestRegion> nearestRegion(Matcher<NearestRegion> ... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<NearestRegion> regionId(int regionId) {
        return map(
            NearestRegion::getRegionId,
            "'regionId'",
            is(regionId),
            NearestRegionMatcher::toStr
        );
    }

    public static Matcher<NearestRegion> distanceKm(BigDecimal distanceKm) {
        return map(
            NearestRegion::getDistanceKm,
            "'distanceKm'",
            is(distanceKm),
            NearestRegionMatcher::toStr
        );
    }

    public static Matcher<NearestRegion> courierAvailable(Boolean courierAvailable) {
        return map(
            NearestRegion::getCourierAvailable,
            "'courierAvailable'",
            is(courierAvailable),
            NearestRegionMatcher::toStr
        );
    }

    public static Matcher<NearestRegion> pickupAvailable(Boolean pickupAvailable) {
        return map(
            NearestRegion::getPickupAvailable,
            "'pickupAvailable'",
            is(pickupAvailable),
            NearestRegionMatcher::toStr
        );
    }

    public static Matcher<NearestRegion> postAvailable(Boolean postAvailable) {
        return map(
            NearestRegion::getPostAvailable,
            "'postAvailable'",
            is(postAvailable),
            NearestRegionMatcher::toStr
        );
    }

    public static Matcher<NearestRegion> subtitle(String subtitle) {
        return map(
            NearestRegion::getSubtitle,
            "'subtitle'",
            is(subtitle),
            NearestRegionMatcher::toStr
        );
    }

    public static String toStr(NearestRegion nearestRegion) {
        if (null == nearestRegion) {
            return "null";
        }
        return MoreObjects.toStringHelper(nearestRegion)
            .add("regionId", nearestRegion.getRegionId())
            .add("distanceKm", nearestRegion.getDistanceKm())
            .add("courierAvailable", nearestRegion.getCourierAvailable())
            .add("pickupAvailable", nearestRegion.getPickupAvailable())
            .add("postAvailable", nearestRegion.getPostAvailable())
            .add("subtitle", nearestRegion.getSubtitle())
            .toString();
    }
}
