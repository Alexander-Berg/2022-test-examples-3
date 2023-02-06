package ru.yandex.market.logistics.management.domain.converter;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.type.PhoneType;

class PhoneConverterTest extends AbstractTest {

    private static final Phone PHONE_DTO = Phone.newBuilder()
        .number("+78005553535")
        .internalNumber("")
        .comment("number")
        .type(PhoneType.PRIMARY)
        .build();

    private static final ru.yandex.market.logistics.management.domain.entity.Phone PHONE_ENTITY =
        new ru.yandex.market.logistics.management.domain.entity.Phone()
            .setNumber("+78005553535")
            .setInternalNumber("")
            .setComment("number")
            .setType(ru.yandex.market.logistics.management.domain.entity.type.PhoneType.PRIMARY);

    private static final PhoneConverter CONVERTER = new PhoneConverter();

    @Test
    void toDto() {
        softly.assertThat(PHONE_DTO).isEqualTo(CONVERTER.toDto(PHONE_ENTITY));
    }

    @Test
    void toEntity() {
        softly.assertThat(PHONE_ENTITY).isEqualTo(CONVERTER.toEntity(PHONE_DTO));
    }

    @Test
    void propagateEntity() {
        softly.assertThat(PHONE_ENTITY).isEqualTo(CONVERTER.propagateEntity(
            PHONE_DTO,
            new ru.yandex.market.logistics.management.domain.entity.Phone()
        ));
    }
}
