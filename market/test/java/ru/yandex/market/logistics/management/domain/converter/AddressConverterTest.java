package ru.yandex.market.logistics.management.domain.converter;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.entity.response.core.Address;

class AddressConverterTest extends AbstractTest {

    private static final Address.AddressBuilder ADDRESS_DTO_BUILDER = Address.newBuilder()
        .locationId(12345)
        .country("Россия")
        .settlement("Москва")
        .postCode("555666")
        .latitude(new BigDecimal("100"))
        .longitude(new BigDecimal("200"))
        .street("Октябрьская")
        .house("5")
        .housing("3")
        .building("2")
        .apartment("1")
        .comment("comment")
        .region("region")
        .subRegion("subRegion")
        .addressString("Строка адреса")
        .shortAddressString("Строка адреса");

    private static final Address ADDRESS_DTO = ADDRESS_DTO_BUILDER.build();

    private static final Address ADDRESS_DTO_SCALED_COORDINATES = ADDRESS_DTO_BUILDER
        .latitude(new BigDecimal("100.000000"))
        .longitude(new BigDecimal("200.000000"))
        .build();

    private static final ru.yandex.market.logistics.management.domain.entity.Address ADDRESS_ENTITY =
        new ru.yandex.market.logistics.management.domain.entity.Address()
            .setCountry("Россия")
            .setLocationId(12345)
            .setSettlement("Москва")
            .setPostCode("555666")
            .setLatitude(new BigDecimal("100.000000"))
            .setLongitude(new BigDecimal("200.000000"))
            .setStreet("Октябрьская")
            .setHouse("5")
            .setHousing("3")
            .setBuilding("2")
            .setApartment("1")
            .setComment("comment")
            .setAddressString("Строка адреса")
            .setShortAddressString("Строка адреса")
            .setRegion("region")
            .setSubRegion("subRegion");

    private static final AddressConverter CONVERTER = new AddressConverter(Mappers.getMapper(AddressMapper.class));

    @Test
    void convertToDto() {
        softly.assertThat(ADDRESS_DTO_SCALED_COORDINATES).isEqualTo(CONVERTER.toDto(ADDRESS_ENTITY));
    }

    @Test
    void convertToDtoWithoutCountry() {
        ADDRESS_ENTITY.setCountry(null);
        softly.assertThat(ADDRESS_DTO_SCALED_COORDINATES).isEqualTo(CONVERTER.toDto(ADDRESS_ENTITY));
    }

    @Test
    void convertToEntity() {
        softly.assertThat(ADDRESS_ENTITY).isEqualTo(CONVERTER.toEntity(ADDRESS_DTO));
    }
}
