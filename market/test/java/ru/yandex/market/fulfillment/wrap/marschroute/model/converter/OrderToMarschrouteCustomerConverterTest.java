package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.wrap.marschroute.configuration.ConversionConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.model.MarschrouteCustomer;
import ru.yandex.market.fulfillment.wrap.marschroute.service.SystemPropertyService;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.GeoInformationProvider;
import ru.yandex.market.logistic.api.model.fulfillment.Email;
import ru.yandex.market.logistic.api.model.fulfillment.Order;
import ru.yandex.market.logistic.api.model.fulfillment.Person;
import ru.yandex.market.logistic.api.model.fulfillment.Phone;
import ru.yandex.market.logistic.api.model.fulfillment.Recipient;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ConversionConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(OrderToMarschrouteCustomerConverter.class)
@MockBean(GeoInformationProvider.class)
@MockBean(SystemPropertyService.class)
class OrderToMarschrouteCustomerConverterTest extends BaseIntegrationTest {

    @Autowired
    private OrderToMarschrouteCustomerConverter converter;

    @Test
    void testConversion() {
        Recipient recipient = createRecipient();
        Order order = createOrder(new ResourceId("yandexId", "ffId"), recipient);

        MarschrouteCustomer customer = converter.convert(order);

        softly.assertThat(customer.getId())
            .as("Asserting id value")
            .isEqualTo(order.getOrderId().getYandexId());

        softly.assertThat(customer.getFirstname())
            .as("Asserting first name value")
            .isEqualTo(recipient.getFio().getName());

        softly.assertThat(customer.getLastname())
            .as("Asserting last name value")
            .isEqualTo(recipient.getFio().getSurname());

        softly.assertThat(customer.getMiddlename())
            .as("Asserting patronymic value")
            .isEqualTo(recipient.getFio().getPatronymic());

        softly.assertThat(customer.getEmail())
            .as("Asserting email value")
            .isNull();

        softly.assertThat(customer.getPhone())
            .as("Asserting first phone")
            .isEqualTo(recipient.getPhones().get(0).getPhoneNumber());

        softly.assertThat(customer.getPhone2())
            .as("Asserting second phone")
            .isEqualTo(recipient.getPhones().get(1).getPhoneNumber());
    }

    @Test
    void testConversionWithIdNull() {
        Recipient recipient = createRecipient();
        Order order = createOrder(null, recipient);

        MarschrouteCustomer customer = converter.convert(order);

        softly.assertThat(customer.getId())
            .as("Asserting that id value is null")
            .isNull();

        softly.assertThat(customer.getFirstname())
            .as("Asserting first name value")
            .isEqualTo(recipient.getFio().getName());

        softly.assertThat(customer.getLastname())
            .as("Asserting last name value")
            .isEqualTo(recipient.getFio().getSurname());

        softly.assertThat(customer.getMiddlename())
            .as("Asserting patronymic value")
            .isEqualTo(recipient.getFio().getPatronymic());

        softly.assertThat(customer.getEmail())
            .as("Asserting email value")
            .isNull();

        softly.assertThat(customer.getPhone())
            .as("Asserting first phone")
            .isEqualTo(recipient.getPhones().get(0).getPhoneNumber());

        softly.assertThat(customer.getPhone2())
            .as("Asserting second phone")
            .isEqualTo(recipient.getPhones().get(1).getPhoneNumber());
    }

    private Recipient createRecipient() {

        Person person = new Person("Name", "Surname", "Patronymic", null);

        Email email = new Email("email@email.com");

        List<Phone> phones = ImmutableList.of(
            new Phone("79169169191", null),
            new Phone("79169169392", null)
        );

        return new Recipient(person, phones, email);
    }

    private Order createOrder(ResourceId orderId, Recipient recipient) {
        return new Order.OrderBuilder(
                orderId,
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
                recipient,
                null,
                null
        ).build();
    }
}
