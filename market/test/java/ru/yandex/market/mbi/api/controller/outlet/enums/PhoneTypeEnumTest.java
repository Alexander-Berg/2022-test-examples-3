package ru.yandex.market.mbi.api.controller.outlet.enums;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.common.util.phone.PhoneType;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.PhoneTypeDTO;
import ru.yandex.common.util.id.HasId;

class PhoneTypeEnumTest {

    static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of(PhoneType.FAX, PhoneTypeDTO.FAX),
                Arguments.of(PhoneType.PHONE, PhoneTypeDTO.PHONE),
                Arguments.of(PhoneType.PHONE_FAX, PhoneTypeDTO.PHONE_FAX),
                Arguments.of(PhoneType.UNKNOWN, PhoneTypeDTO.UNKNOWN)
        );
    }

    @ParameterizedTest
    @MethodSource("args")
    void moderationLevel(PhoneType phoneType, PhoneTypeDTO phoneTypeDTO) {

        Assertions.assertEquals(phoneTypeDTO, HasId.findById(PhoneTypeDTO.class, phoneType.name()).orElse(null));
        Assertions.assertEquals(phoneType, PhoneType.valueOf(phoneTypeDTO.name()));
    }
}
