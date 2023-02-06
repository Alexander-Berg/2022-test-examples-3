package ru.yandex.market.logistic.gateway.utils;

import java.math.BigDecimal;
import java.util.List;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistic.gateway.common.model.common.LocationFilter;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistics.personal.model.Address;
import ru.yandex.market.personal.client.model.FullName;
import ru.yandex.market.personal.client.model.GpsCoord;
import ru.yandex.market.personal.enums.PersonalDataType;
import ru.yandex.market.personal.model.PersonalResponseItem;

@UtilityClass
public class CommonDtoFactory {

    public static Partner createPartner() {
        return new Partner(145L);
    }

    public static LocationFilter createLocationFilterRussia() {
        return LocationFilter.builder().setCountry("Россия").build();
    }

    public static Address createAddress() {
        return Address.builder()
            .country("country_from_personal")
            .federalDistrict("federalDistrict_from_personal")
            .region("region_from_personal")
            .locality("locality_from_personal")
            .subRegion("subRegion_from_personal")
            .settlement("settlement_from_personal")
            .district("district_from_personal")
            .street("street_from_personal")
            .house("house_from_personal")
            .building("building_from_personal")
            .housing("housing_from_personal")
            .room("room_from_personal")
            .zipCode("zipCode_from_personal")
            .porch("porch_from_personal")
            .floor(12)
            .metro("metro_from_personal")
            .geoId(123)
            .intercom("intercom_from_personal")
            .comment("comment_from_personal")
            .build();
    }

    public static GpsCoord createGpsCoord() {
        return new GpsCoord()
            .latitude(BigDecimal.valueOf(1010101))
            .longitude(BigDecimal.valueOf(1234567));
    }

    public static FullName createFullName() {
        return new FullName()
            .surname("surname_from_personal")
            .forename("forename_from_personal")
            .patronymic("patronymic_from_personal");
    }
    public static List<PersonalResponseItem> createPersonalResponse() {
        return List.of(
            new PersonalResponseItem("location_to_address_id", PersonalDataType.ADDRESS, createAddress()),
            new PersonalResponseItem("location_to_gps_id", PersonalDataType.GPS_COORD, createGpsCoord()),
            new PersonalResponseItem("pErSoNalFiO", PersonalDataType.FULL_NAME, createFullName()),
            new PersonalResponseItem("pErSoNaLPhOnE1", PersonalDataType.PHONE, "phone_from_personal_1"),
            new PersonalResponseItem("pErSoNaLPhOnE2", PersonalDataType.PHONE, "phone_from_personal_2"),
            new PersonalResponseItem("pErSoNalEmAiL", PersonalDataType.EMAIL, "email_from_personal@gmail.com")
        );
    }

}
