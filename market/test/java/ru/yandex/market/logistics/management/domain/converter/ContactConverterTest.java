package ru.yandex.market.logistics.management.domain.converter;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.entity.response.point.Contact;

class ContactConverterTest extends AbstractTest {

    private static final Contact CONTACT_DTO = new Contact(
        "Арсений",
        "Петров",
        "Сергеевич"
    );
    private static final ru.yandex.market.logistics.management.domain.entity.Contact CONTACT_ENTITY =
        new ru.yandex.market.logistics.management.domain.entity.Contact()
            .setName("Арсений")
            .setSurname("Петров")
            .setPatronymic("Сергеевич");

    private static final ContactConverter CONVERTER = new ContactConverter();

    @Test
    void convertToDto() {
        softly.assertThat(CONTACT_DTO).isEqualTo(CONVERTER.toDto(CONTACT_ENTITY));
    }

    @Test
    void convertToEntity() {
        softly.assertThat(CONTACT_ENTITY).isEqualTo(CONVERTER.toEntity(CONTACT_DTO));
    }
}
