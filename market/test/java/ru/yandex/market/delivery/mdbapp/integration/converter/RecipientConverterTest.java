package ru.yandex.market.delivery.mdbapp.integration.converter;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.gateway.common.model.delivery.Person;
import ru.yandex.market.logistic.gateway.common.model.delivery.PersonalDataStatus;
import ru.yandex.market.logistic.gateway.common.model.delivery.Phone;
import ru.yandex.market.logistic.gateway.common.model.delivery.Recipient;
import ru.yandex.market.logistic.gateway.common.model.delivery.RecipientData;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static steps.orderSteps.BuyerSteps.getBuyer;
import static steps.orderSteps.OrderSteps.getRedMultipleOrder;
import static steps.orderSteps.RecipientSteps.getRecipient;

public class RecipientConverterTest {

    private RecipientConverter recipientConverter = new RecipientConverter();

    @Test
    public void convertRecipientSuccess() {
        assertThat(recipientConverter.convert(getBuyer(), getRecipient(), getRedMultipleOrder()))
            .as("Buyer to Recipient conversion")
            .isEqualTo(getExpectedRecipient());
    }

    private Recipient getExpectedRecipient() {
        return new Recipient.RecipientBuilder(
            new Person.PersonBuilder(
                "RecipientFirstName",
                "RecipientLastName"
            )
                .setPatronymic("RecipientMiddleName")
                .build(),
            Collections.singletonList(new Phone.PhoneBuilder("71234567891").build())
        )
            .setEmail("test-recipient@test.com")
            .setRecipientData(new RecipientData(new ResourceId.ResourceIdBuilder().setYandexId("123").build()))
            .setPersonalDataStatus(PersonalDataStatus.NO_DATA)
            .build();
    }
}
