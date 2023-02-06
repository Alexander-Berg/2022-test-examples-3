package ru.yandex.market.logistics.lom.utils;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.entity.Contact;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.OrderContact;
import ru.yandex.market.logistics.lom.entity.embedded.Address;
import ru.yandex.market.logistics.lom.entity.embedded.PickupPoint;
import ru.yandex.market.logistics.lom.entity.embedded.Recipient;
import ru.yandex.market.logistics.lom.entity.enums.ContactType;
import ru.yandex.market.logistics.lom.entity.enums.DeliveryType;

@DisplayName("Unit тесты для LesUtils")
class LesUtilsTest extends AbstractTest {

    static Stream<Arguments> arguments() {
        return Stream.of(
            Arguments.of(
                List.of(
                    getOrderContact(ContactType.CREDENTIALS, "+79991234567"),
                    getOrderContact(ContactType.RECIPIENT, "+39981234567")
                )
            ),
            Arguments.of(
                List.of(
                    getOrderContact(ContactType.CREDENTIALS, "+79991234567"),
                    getOrderContact(ContactType.CONTACT, "+79981234567")
                )
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> argumentsPersonalPhoneId() {
        return Stream.of(
            Arguments.of(
                List.of(
                    getOrderContact(ContactType.CREDENTIALS, "+79991234567"),
                    getOrderContact(ContactType.RECIPIENT, "+39981234567")
                ),
                null
            ),
            Arguments.of(
                List.of(
                    getOrderContact(ContactType.CREDENTIALS, "+39981234567"),
                    getOrderContact(ContactType.RECIPIENT, "+39981234567", "")
                ),
                null
            ),
            Arguments.of(
                List.of(
                    getOrderContact(ContactType.RECIPIENT, "+39981234567", "test"),
                    getOrderContact(ContactType.CREDENTIALS, "+39981234567")
                ),
                "test"
            ),
            Arguments.of(
                List.of(
                    getOrderContact(ContactType.CREDENTIALS, "+79991234567", "personal-phone-id-1"),
                    getOrderContact(ContactType.CONTACT, "+79981234567", "personal-phone-id-1")
                ),
                null
            ),
            Arguments.of(
                List.of(
                    getOrderContact(ContactType.RECIPIENT, "+79991234567", ""),
                    getOrderContact(ContactType.CONTACT, "+79981234567", "personal-phone-id-1")
                ),
                null
            )
        );
    }

    private static Order getOrder(List<OrderContact> contacts) {
        return new Order().setOrderContacts(contacts);
    }

    private static OrderContact getOrderContact(ContactType contactType, String phone) {
        return new OrderContact()
            .setContactType(contactType)
            .setContact(new Contact().setPhone(phone));
    }

    @Nonnull
    private static OrderContact getOrderContact(ContactType contactType, String phone, String personalPhoneId) {
        OrderContact contact = getOrderContact(contactType, phone);
        contact.getContact().setPersonalPhoneId(personalPhoneId);
        return contact;
    }

    @Test
    @DisplayName("Успешное получение номера телефона получателя")
    void success() {
        String actualPhoneNumber = LesUtils.getPhone(getOrder(List.of(
            getOrderContact(ContactType.CREDENTIALS, "+79991234567"),
            getOrderContact(ContactType.RECIPIENT, "+79981234567")
        )));

        softly.assertThat(actualPhoneNumber).isEqualTo("+79981234567");
    }


    @ParameterizedTest
    @MethodSource("argumentsPersonalPhoneId")
    @DisplayName("Успешное получение идентификатора персонального номера телефона получателя")
    void successPersonalPhoneId(List<OrderContact> contacts, String personalPhoneIdExpected) {
        String personalPhoneId = LesUtils.getPersonalPhoneId(getOrder(contacts));
        softly.assertThat(personalPhoneId).isEqualTo(personalPhoneIdExpected);
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("Неуспешное получение номера телефона получателя")
    void normalizePhoneNumber(List<OrderContact> contacts) {
        softly.assertThat(LesUtils.getPhone(getOrder(contacts))).isEqualTo(null);
    }

    @Test
    @DisplayName("Получение geo id получателя для PICKUP заказа")
    void getPickupOrderRecipientGeoId() {
        Order order = new Order()
            .setDeliveryType(DeliveryType.PICKUP)
            .setPickupPoint(
                new PickupPoint()
                    .setAddress(new Address().setGeoId(213))
            );

        LesUtils.getRecipientGeoId(order);
        softly.assertThat(LesUtils.getRecipientGeoId(order)).isEqualTo(213);
    }

    @Test
    @DisplayName("Получение geo id получателя для COURIER заказа")
    void getCourierOrderRecipientGeoId() {
        Order order = new Order()
            .setDeliveryType(DeliveryType.COURIER)
            .setRecipient(
                new Recipient()
                    .setAddress(new Address().setGeoId(213))
            );

        LesUtils.getRecipientGeoId(order);
        softly.assertThat(LesUtils.getRecipientGeoId(order)).isEqualTo(213);
    }
}
