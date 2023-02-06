package ru.yandex.market.delivery.mdbapp.integration.converter;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import steps.UpdateRecipientSteps;

import ru.yandex.market.delivery.mdbapp.configuration.FeatureProperties;
import ru.yandex.market.delivery.mdbapp.integration.service.PersonalDataService;
import ru.yandex.market.logistics.lom.model.dto.OrderContactDto;
import ru.yandex.market.logistics.lom.model.dto.UpdateOrderRecipientRequestDto;
import ru.yandex.market.logistics.lom.model.enums.ContactType;
import ru.yandex.market.logistics.personal.converter.AddressConverter;
import ru.yandex.market.personal.PersonalClient;

class UpdateRecipientPayloadConverterTest {

    @Rule
    private final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private final UpdateRecipientPayloadConverter converter = new UpdateRecipientPayloadConverter(
        new PersonalDataService(
            new FeatureProperties().setFillPersonalDataValuesWithDefaultData(true),
            Mockito.mock(PersonalClient.class),
            new AddressConverter()
        )
    );

    @Test
    void shouldSuccessfullyConvert() {

        final var actual = converter.toRecipientRequestDto(
            UpdateRecipientSteps.createUpdateRecipientDto()
        );

        softly.assertThat(actual).isEqualToComparingFieldByField(getUpdateOrderRecipientRequestDto());
    }

    private UpdateOrderRecipientRequestDto getUpdateOrderRecipientRequestDto() {
        return UpdateOrderRecipientRequestDto.builder()
            .email(UpdateRecipientSteps.EMAIL)
            .contact(
                OrderContactDto.builder()
                    .firstName(UpdateRecipientSteps.FIRST_NAME)
                    .lastName(UpdateRecipientSteps.LAST_NAME)
                    .middleName(UpdateRecipientSteps.MIDDLE_NAME)
                    .phone(UpdateRecipientSteps.PHONE)
                    .contactType(ContactType.RECIPIENT)
                    .build()
            )
            .checkouterRequestId(UpdateRecipientSteps.UPDATE_REQUEST_ID)
            .barcode(String.valueOf(UpdateRecipientSteps.ORDER_ID))
            .build();
    }
}
