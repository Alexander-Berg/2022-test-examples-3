package ru.yandex.market.tpl.core.domain.order.address;

import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.external.geocoder.GeocoderQueryBuilderMode;

import static org.assertj.core.api.Assertions.assertThat;

class AddressStringTest {

    @Test
    void getAddressStringDropDefaultCity() {
        String addressString = AddressString.builder()
                .dropDefaultCityFromAddressString(true)
                .city("Москва")
                .street("Зубовский бульвар")
                .house("17")
                .entrance("4")
                .floor("4")
                .build().getAddressString();

        assertThat(addressString).isEqualTo("Зубовский бульвар, д. 17, подъезд 4, этаж 4");
    }

    @Test
    void getAddressStringDropDefaultCityDoesntAffectOtherCities() {
        String addressString = AddressString.builder()
                .dropDefaultCityFromAddressString(true)
                .city("Московский")
                .street("Зубовский бульвар")
                .house("17")
                .entrance("4")
                .floor("4")
                .build().getAddressString();

        assertThat(addressString).isEqualTo("г. Московский, Зубовский бульвар, д. 17, подъезд 4, этаж 4");
    }

    @Test
    void getAddressStringDoNotDropDefaultCity() {
        String addressString = AddressString.builder()
                .city("Москва")
                .street("Зубовский бульвар")
                .house("17")
                .building("1")
                .housing("2")
                .apartment("3")
                .entrance("4")
                .floor("4")
                .entryPhone("1234")
                .build().getAddressString();

        assertThat(addressString).isEqualTo("г. Москва, Зубовский бульвар, д. 17, стр. 1, к. 2, " +
                "подъезд 4, кв. 3, этаж 4, домофон 1234");
    }

    @Test
    void getGeocoderAddressString() {
        String addressString = AddressString.builder()
                .city("Москва")
                .street("Зубовский бульвар")
                .house("17")
                .building("с 1")
                .housing("к 2")
                .apartment("3")
                .entrance("4")
                .floor("4")
                .entryPhone("1234")
                .build().getGeocoderAddressString();

        assertThat(addressString).isEqualTo("г. Москва, Зубовский бульвар, 17, с 1, к 2, подъезд 4");
    }

    @Test
    void getGeocoderAddressString_whenRegionIdentified() {
        //when
        String addressString = AddressString.builder()
                .city("Москва")
                .street("Зубовский бульвар")
                .house("17")
                .building("с 1")
                .housing("к 2")
                .apartment("3")
                .entrance("4")
                .floor("4")
                .entryPhone("1234")
                .build().getGeocoderAddressString(GeocoderQueryBuilderMode.REGION_IDENTIFIED);

        //then
        assertThat(addressString).isEqualTo("Москва, Зубовский бульвар, 17, с 1, к 2, подъезд 4");
    }
}
