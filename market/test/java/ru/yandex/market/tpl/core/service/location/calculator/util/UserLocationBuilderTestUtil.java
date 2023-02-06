package ru.yandex.market.tpl.core.service.location.calculator.util;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import ru.yandex.market.tpl.core.domain.usershift.location.UserLocation;

@UtilityClass
public class UserLocationBuilderTestUtil {

    public List<UserLocation> buildUserLocations(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> buildUserLocation())
                .collect(Collectors.toList());
    }

    public UserLocation buildUserLocation() {
        return buildUserLocation(0, 0);
    }

    public UserLocation buildUserLocation(double latitude, double longitude) {
        return buildUserLocation(latitude, longitude, Instant.now());
    }

    @SneakyThrows
    public UserLocation buildUserLocation(double latitude, double longitude, Instant trackTime) {
        UserLocation userLocation = new UserLocation();
        userLocation.setLatitude(BigDecimal.valueOf(latitude));
        userLocation.setLongitude(BigDecimal.valueOf(longitude));

        userLocation.setPostedAt(trackTime);

        return userLocation;
    }

}
