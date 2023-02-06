package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import ru.yandex.market.api.geo.domain.GeoAddress;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.api.ApiMatchers.map;

public class GeoAddressMatcher {
    public static Matcher<GeoAddress> geoAddress(Matcher<GeoAddress>... geoAddress) {
        return allOf(geoAddress);
    }

    public static Matcher<GeoAddress> regionId(int id) {
        return map(
            GeoAddress::getRegionId,
            "'regionId'",
            is(id),
            GeoAddressMatcher::toStr
        );
    }

    public static Matcher<GeoAddress> country(String country) {
        return map(
            GeoAddress::getCountry,
            "'country'",
            is(country),
            GeoAddressMatcher::toStr
        );
    }

    public static Matcher<GeoAddress> region(String region) {
        return map(
            GeoAddress::getRegion,
            "'region'",
            is(region),
            GeoAddressMatcher::toStr
        );
    }

    public static Matcher<GeoAddress> subregion(String subregion) {
        return map(
            GeoAddress::getSubLocality,
            "'subregion'",
            is(subregion),
            GeoAddressMatcher::toStr
        );
    }

    public static Matcher<GeoAddress> locality(String locality) {
        return map(
            GeoAddress::getLocality,
            "'locality'",
            is(locality),
            GeoAddressMatcher::toStr
        );
    }

    public static Matcher<GeoAddress> thoroughfare(String thoroughfare) {
        return map(
            GeoAddress::getThoroughfare,
            "'thoroughfare'",
            is(thoroughfare),
            GeoAddressMatcher::toStr
        );
    }

    public static Matcher<GeoAddress> premiseNumber(String premiseNumber) {
        return map(GeoAddress::getPremiseNumber, "'premiseNumber'", is(premiseNumber), GeoAddressMatcher::toStr);
    }

    private static String toStr(GeoAddress geoAddress) {
        if (null == geoAddress) {
            return "null";
        }
        return MoreObjects.toStringHelper(GeoAddress.class)
            .add("regionId", geoAddress.getRegionId())
            .add("country", geoAddress.getCountry())
            .add("region", geoAddress.getRegion())
            .add("subregion", geoAddress.getSubRegion())
            .add("locality", geoAddress.getLocality())
            .add("thoroughfare", geoAddress.getThoroughfare())
            .add("premiseNumber", geoAddress.getPremiseNumber())
            .toString();
    }
}
