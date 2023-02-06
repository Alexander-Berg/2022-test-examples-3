package ru.yandex.market.delivery.rupostintegrationapp.service.converter;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.delivery.entities.common.Location;
import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.dao.pickuppoint.RussianPostPickupPoint;

import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

class LocationConverterTest extends BaseTest {

    static Stream<Arguments> getParameters() {
        return Stream.of(
            Arguments.of("location_1", getSimpleRussianPostPickupPoint(), getSimpleLocation()),
            Arguments.of("location_2", getSimpleRussianPostPickupPoint2(), getSimplePickupPoint2()),
            Arguments.of("location_3", getSimpleRussianPostPickupPoint3(), getSimplePickupPoint3())
        );
    }

    private static Location getSimpleLocation() {
        Location location = new Location();
        location.setCountry("Russia");
        location.setSubRegion("simple Sub Admin area");
        location.setLocality("Moscow");
        location.setStreet("Tolstogo");
        location.setHouse("16");
        location.setZipCode("117208");
        location.setRegion("Central Area");

        return location;
    }

    private static RussianPostPickupPoint getSimpleRussianPostPickupPoint() {
        RussianPostPickupPoint russianPostPickupPoint = new RussianPostPickupPoint();
        russianPostPickupPoint.setCountry("Russia");
        russianPostPickupPoint.setSubAdminArea("simple Sub Admin area");
        russianPostPickupPoint.setLocality("Moscow");
        russianPostPickupPoint.setStreet("Tolstogo");
        russianPostPickupPoint.setHouse("16");
        russianPostPickupPoint.setIndex("117208");
        russianPostPickupPoint.setArea("Central Area");

        return russianPostPickupPoint;
    }

    private static RussianPostPickupPoint getSimpleRussianPostPickupPoint2() {
        RussianPostPickupPoint russianPostPickupPoint = getSimpleRussianPostPickupPoint();
        russianPostPickupPoint.setArea(null);

        return russianPostPickupPoint;
    }

    private static Object getSimplePickupPoint2() {
        Location location = getSimpleLocation();
        location.setRegion("Moscow");

        return location;
    }

    private static Object getSimpleRussianPostPickupPoint3() {
        RussianPostPickupPoint russianPostPickupPoint = getSimpleRussianPostPickupPoint();
        russianPostPickupPoint.setLocality("Moscow, Zelenograd city");
        russianPostPickupPoint.setArea(null);

        return russianPostPickupPoint;
    }

    private static Object getSimplePickupPoint3() {
        Location location = getSimpleLocation();
        location.setRegion("Moscow");
        location.setLocality("Moscow, Zelenograd city");

        return location;
    }

    @ParameterizedTest(name = "[" + INDEX_PLACEHOLDER + "] " + "{0}")
    @MethodSource("getParameters")
    void convert(String caseName, RussianPostPickupPoint russianPostPickupPoint, Location location) {
        softly.assertThat(new RuPostPickupPointToLocationConverter().convert(russianPostPickupPoint))
            .as("Location converter cannot convert RuPost PP to Location")
            .isEqualTo(location);
    }

}
