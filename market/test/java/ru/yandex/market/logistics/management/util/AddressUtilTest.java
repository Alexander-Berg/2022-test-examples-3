package ru.yandex.market.logistics.management.util;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.gateway.common.model.delivery.Location;
import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.domain.entity.Address;
import ru.yandex.market.logistics.management.domain.entity.LogisticsPoint;

class AddressUtilTest extends AbstractTest {

    private static final BigDecimal LATITUDE_1 = BigDecimal.valueOf(1);
    private static final BigDecimal LATITUDE_1_SCALED = new BigDecimal("1.000000");
    private static final BigDecimal LATITUDE_2 = BigDecimal.valueOf(2.0);
    private static final BigDecimal LATITUDE_2_SCALED = new BigDecimal("2.000000");
    private static final BigDecimal LONGITUDE_1 = BigDecimal.valueOf(3);
    private static final BigDecimal LONGITUDE_1_SCALED = new BigDecimal("3.000000");
    private static final BigDecimal LONGITUDE_2 = BigDecimal.valueOf(4.0);
    private static final BigDecimal LONGITUDE_2_SCALED = new BigDecimal("4.000000");
    private static final int EXACT_LOCATION_ID = 123;

    @Test
    void shouldUpdateCoordinatesAndWipeExactLocationId_whenHasNewCoordinates() {
        // given:
        var logisticsPoint = logisticsPoint();
        var location = location(LATITUDE_2, LONGITUDE_2);

        // when:
        AddressUtil.fillAddress(logisticsPoint, location);

        // then:
        Address actualAddress = logisticsPoint.getAddress();
        softly.assertThat(actualAddress.getLatitude()).isEqualTo(LATITUDE_2_SCALED);
        softly.assertThat(actualAddress.getLongitude()).isEqualTo(LONGITUDE_2_SCALED);
        softly.assertThat(actualAddress.getExactLocationId()).isEqualTo(null);
    }

    @Test
    void shouldNotUpdateCoordinatesAndExactLocationId_whenHasSameCoordinates() {
        // given:
        var logisticsPoint = logisticsPoint();
        var location = location(LATITUDE_1, LONGITUDE_1);

        // when:
        AddressUtil.fillAddress(logisticsPoint, location);

        // then:
        Address actualAddress = logisticsPoint.getAddress();
        softly.assertThat(actualAddress.getLatitude()).isEqualTo(LATITUDE_1_SCALED);
        softly.assertThat(actualAddress.getLongitude()).isEqualTo(LONGITUDE_1_SCALED);
        softly.assertThat(actualAddress.getExactLocationId()).isEqualTo(EXACT_LOCATION_ID);
    }

    private LogisticsPoint logisticsPoint() {
        LogisticsPoint logisticsPoint = new LogisticsPoint();
        logisticsPoint.setAddress(address());
        return logisticsPoint;
    }

    private Address address() {
        Address address = new Address();
        address.setLatitude(LATITUDE_1);
        address.setLongitude(LONGITUDE_1);
        address.setExactLocationId(EXACT_LOCATION_ID);
        return address;
    }

    private Location location(BigDecimal latitude, BigDecimal longitude) {
        return new Location(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            latitude,
            longitude,
            null,
            null
        );
    }
}
