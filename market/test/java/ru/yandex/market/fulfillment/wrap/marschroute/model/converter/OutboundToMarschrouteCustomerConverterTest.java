package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.wrap.marschroute.configuration.ConversionConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.model.MarschrouteCustomer;
import ru.yandex.market.fulfillment.wrap.marschroute.service.SystemPropertyService;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.GeoInformationProvider;
import ru.yandex.market.logistic.api.model.fulfillment.Address;
import ru.yandex.market.logistic.api.model.fulfillment.Courier;
import ru.yandex.market.logistic.api.model.fulfillment.LegalEntity;
import ru.yandex.market.logistic.api.model.fulfillment.Outbound;
import ru.yandex.market.logistic.api.model.fulfillment.Person;
import ru.yandex.market.logistic.api.model.fulfillment.Phone;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ConversionConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@MockBean(GeoInformationProvider.class)
@MockBean(SystemPropertyService.class)
class OutboundToMarschrouteCustomerConverterTest extends BaseIntegrationTest {

    @Autowired
    private OutboundToMarschrouteCustomerConverter converter;

    @Test
    void convert() {
        Person person = new Person("Harry", "Potter", "James", null);

        Courier courier = new Courier.CourierBuilder(Collections.singletonList(person))
            .setPhone(new Phone.PhoneBuilder("88005553535").build())
            .build();

        LegalEntity owner = new LegalEntity.LegalEntityBuilder()
                .setLegalName("ООО \"Костян и его тушканы\"")
                .setInn("456456456456")
                .setBank("Sber, of course")
                .setKpp("123123")
                .setBik("123456123456")
                .setAccount("555888")
                .setCorrespondentAccount("888555")
                .setAddress(new Address("Москва", null))
                .build();

        Outbound outbound = new Outbound.OutboundBuilder(
                new ResourceId("RID", null),
                null,
                null,
                courier,
                owner,
                null
        ).build();

        MarschrouteCustomer marschrouteCustomer = converter.convert(outbound);

        softly.assertThat(marschrouteCustomer.getId())
            .as("Asserting the id")
            .isEqualTo(outbound.getOutboundId().getYandexId());
        softly.assertThat(marschrouteCustomer.getFirstname())
            .as("Asserting the first name")
            .isEqualTo(person.getName());
        softly.assertThat(marschrouteCustomer.getMiddlename())
            .as("Asserting the middle name")
            .isEqualTo(person.getPatronymic());
        softly.assertThat(marschrouteCustomer.getLastname())
            .as("Asserting the last name")
            .isEqualTo(person.getSurname());
        softly.assertThat(marschrouteCustomer.getPhone())
            .as("Asserting that the mobile phone is not null")
            .isNotNull();
        softly.assertThat(marschrouteCustomer.getPhone2())
            .as("Asserting the other phone is null")
            .isNull();
        softly.assertThat(marschrouteCustomer.getCompany())
            .as("Asserting the company")
            .isEqualTo(owner.getLegalName());
        softly.assertThat(marschrouteCustomer.getInn())
            .as("Asserting the inn")
            .isEqualTo(owner.getInn());
        softly.assertThat(marschrouteCustomer.getBank())
            .as("Asserting the bank")
            .isEqualTo(owner.getBank());
        softly.assertThat(marschrouteCustomer.getKpp())
            .as("Asserting the kpp")
            .isEqualTo(owner.getKpp());
        softly.assertThat(marschrouteCustomer.getBik())
            .as("Asserting the bik")
            .isEqualTo(owner.getBik());
        softly.assertThat(marschrouteCustomer.getRs())
            .as("Asserting the rs")
            .isEqualTo(owner.getAccount());
        softly.assertThat(marschrouteCustomer.getKs())
            .as("Asserting the ks")
            .isEqualTo(owner.getCorrespondentAccount());
        softly.assertThat(marschrouteCustomer.getAddress())
            .as("Asserting the address")
            .isEqualTo(owner.getAddress().getCombinedAddress());
    }

    @Test
    void convertOwnerNull() {
        Person person = new Person("Harry", "Potter", "James", null);

        Courier courier = new Courier.CourierBuilder(Collections.singletonList(person))
                .setPhone(new Phone("88005553535", null))
                .build();

        Outbound outbound = new Outbound.OutboundBuilder(
                new ResourceId("RID", null),
                null,
                null,
                courier,
                null,
                null
        ).build();

        MarschrouteCustomer marschrouteCustomer = converter.convert(outbound);

        softly.assertThat(marschrouteCustomer)
            .as("Asserting the converted marschrouteCustomer is null")
            .isNull();
    }

    @Test
    void convertCourierNull() {
        LegalEntity owner = new LegalEntity.LegalEntityBuilder()
                .setLegalName("ООО \"Костян и его тушканы\"")
                .setInn("456456456456")
                .setBank("Sber, of course")
                .setKpp("123123")
                .setBik("123456123456")
                .setAccount("555888")
                .setCorrespondentAccount("888555")
                .setAddress(new Address("Москва", null))
                .build();

        Outbound outbound = new Outbound.OutboundBuilder(
                new ResourceId("RID", null),
                null,
                null,
                null,
                owner,
                null
        ).build();

        MarschrouteCustomer marschrouteCustomer = converter.convert(outbound);

        softly.assertThat(marschrouteCustomer)
            .as("Asserting the converted marschrouteCustomer is null")
            .isNull();
    }

    @Test
    void convertNull() {
        Outbound outbound = new Outbound.OutboundBuilder(
                new ResourceId("RID", null),
                null,
                null,
                null,
                null,
                null
        ).build();


        MarschrouteCustomer marschrouteCustomer = converter.convert(outbound);

        softly.assertThat(marschrouteCustomer)
            .as("Asserting the converted marschrouteCustomer is null")
            .isNull();
    }
}
