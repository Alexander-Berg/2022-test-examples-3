package ru.yandex.market.logistics.management.domain.converter.lgw;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.gateway.common.model.delivery.Person;
import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.domain.entity.Contact;

public class ContactConverterTest extends AbstractTest {

    private static final Person PERSON =
        new Person.PersonBuilder("Арсений", "Петров")
            .setPatronymic("Сергеевич")
            .build();

    private static final Contact CONTACT_ENTITY =
        new Contact()
            .setName("Арсений")
            .setSurname("Петров")
            .setPatronymic("Сергеевич");

    private static final ContactConverter
        CONVERTER = new ContactConverter();

    @Test
    void convertToPerson() {
        softly.assertThat(PERSON).isEqualTo(CONVERTER.convert(CONTACT_ENTITY));
    }

    @Test
    void convertNullContact() {
        softly.assertThat(CONVERTER.convert(null)).isEqualTo(null);
    }
}
