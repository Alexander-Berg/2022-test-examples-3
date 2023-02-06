package ru.yandex.market.fulfillment.wrap.marschroute.factory;

import ru.yandex.market.fulfillment.wrap.marschroute.model.MarschrouteLocation;

/**
 * Класс по созданию экземпляров класса MarschrouteLocation для тестов.
 */
public class MarschrouteLocations {

    public static final String TEST_LOCALITY = "Хабаровск";

    public static MarschrouteLocation location(String cityId) {
        return location(cityId, TEST_LOCALITY);
    }

    public static MarschrouteLocation locationWithPrefix(String cityId, String prefix) {
        return location(cityId, prefix + " " + TEST_LOCALITY);
    }

    private static MarschrouteLocation location(String cityId, String locality) {
        MarschrouteLocation location = new MarschrouteLocation();
        location.setCityId(cityId);
        location.setLocality(locality);
        return location;
    }
}
