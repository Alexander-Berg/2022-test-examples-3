package ru.yandex.market.pvz.core.domain.pickup_point.model;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pvz.core.domain.pickup_point.location.PickupPointLocation;

import static org.assertj.core.api.Assertions.assertThat;

class PickupPointLocationTest {

    @Test
    void testFullestAddress() {
        PickupPointLocation location = PickupPointLocation.builder()
                .locality("Москва")
                .street("улица Новаторов")
                .house("5")
                .building("2")
                .housing("4")
                .office("3")
                .zipCode("123456")
                .build();

        String actual = location.getAddress();
        String expected = "123456, Москва, улица Новаторов, дом 5, корпус 4, строение 2, офис 3";

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void testAddressWithoutZipCode() {
        PickupPointLocation location = PickupPointLocation.builder()
                .locality("Москва")
                .street("улица Новаторов")
                .house("5")
                .building("2")
                .housing("4")
                .build();

        String actual = location.getAddress();
        String expected = "Москва, улица Новаторов, дом 5, корпус 4, строение 2";

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void testAddressWithoutBuilding() {
        PickupPointLocation location = PickupPointLocation.builder()
                .locality("Москва")
                .street("улица Новаторов")
                .house("5")
                .housing("4")
                .zipCode("123456")
                .build();

        String actual = location.getAddress();
        String expected = "123456, Москва, улица Новаторов, дом 5, корпус 4";

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void testAddressWithoutHousing() {
        PickupPointLocation location = PickupPointLocation.builder()
                .locality("Москва")
                .street("улица Новаторов")
                .house("5")
                .building("2")
                .zipCode("123456")
                .build();

        String actual = location.getAddress();
        String expected = "123456, Москва, улица Новаторов, дом 5, строение 2";

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void testShortestAddress() {
        PickupPointLocation location = PickupPointLocation.builder()
                .locality("Москва")
                .street("улица Новаторов")
                .house("5")
                .build();

        String actual = location.getAddress();
        String expected = "Москва, улица Новаторов, дом 5";

        assertThat(actual).isEqualTo(expected);
    }
}
