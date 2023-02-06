package ru.yandex.market.logistics.lom.service;

import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.delivery.Person;
import ru.yandex.market.logistic.gateway.common.model.delivery.PersonalPhone;
import ru.yandex.market.logistic.gateway.common.model.delivery.PersonalRecipient;
import ru.yandex.market.logistic.gateway.common.model.delivery.Phone;
import ru.yandex.market.logistic.gateway.common.model.delivery.Recipient;
import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.converter.lgw.ResourceIdConverter;
import ru.yandex.market.logistics.lom.exception.ChangeOrderSegmentException;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderSegmentRequestPayload;
import ru.yandex.market.logistics.lom.jobs.processor.UpdateOrderRecipientSegmentProcessor;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.lgw.CreateLgwCommonEntitiesUtils;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@DisplayName("Обновление данных получателя")
class UpdateOrderRecipientSegmentRequestProcessingTest extends AbstractContextualTest {
    private static final ChangeOrderSegmentRequestPayload PAYLOAD = PayloadFactory
        .createChangeOrderSegmentRequestPayload(10L, "1", 1L);

    @Autowired
    private UpdateOrderRecipientSegmentProcessor processor;
    @Autowired
    private DeliveryClient deliveryClient;
    @Autowired
    private ResourceIdConverter resourceIdConverter;

    @Test
    @DisplayName("Успешная отправка")
    @DatabaseSetup("/orders/update_recipient/single_ds_segment_request_created.xml")
    void updateOrderDeliveryDateSuccess() throws GatewayApiException {
        processor.processPayload(PAYLOAD);
        verify(deliveryClient).updateRecipient(
            eq(resourceIdConverter.toExternalDsId("barcode-1", "external-id-1")),
            eq(null),
            eq(createRecipient()),
            eq(createPersonalRecipient()),
            eq(10L),
            eq(CreateLgwCommonEntitiesUtils.createPartner(1L)),
            eq(new ClientRequestMeta("1"))
        );

        softly.assertThat(backLogCaptor.getResults())
            .isNotEmpty()
            .anyMatch(line -> line.contains("level=INFO"
                + "\tformat=plain"
                + "\tpayload=ChangeOrderSegmentRequest 10 was sent"
                + "\trequest_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd"
                + "\tentity_types=order,lom_order,changeOrderSegmentRequest"
                + "\tentity_values=order:1001,lom_order:1,changeOrderSegmentRequest:10"
            ));
    }

    @Test
    @DisplayName("Успешная отправка, но с пустым PersonalRecipient")
    @DatabaseSetup("/orders/update_recipient/single_ds_segment_request_created_empty_personal.xml")
    void updateOrderDeliveryRecipientEmptyPersonalSuccess() throws GatewayApiException {
        processor.processPayload(PAYLOAD);
        verify(deliveryClient).updateRecipient(
            eq(resourceIdConverter.toExternalDsId("barcode-1", "external-id-1")),
            eq(null),
            eq(createRecipient()),
            eq(null),
            eq(10L),
            eq(CreateLgwCommonEntitiesUtils.createPartner(1L)),
            eq(new ClientRequestMeta("1"))
        );

        softly.assertThat(backLogCaptor.getResults())
            .isNotEmpty()
            .anyMatch(line -> line.contains("level=INFO"
                + "\tformat=plain"
                + "\tpayload=ChangeOrderSegmentRequest 10 was sent"
                + "\trequest_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd"
                + "\tentity_types=order,lom_order,changeOrderSegmentRequest"
                + "\tentity_values=order:1001,lom_order:1,changeOrderSegmentRequest:10"
            ));
    }

    @Test
    @DisplayName("Ошибка: отсутствует PAYLOAD")
    @DatabaseSetup("/orders/update_recipient/segment_request_without_payload.xml")
    void updateOrderRecipientWithoutPayload() {
        softly.assertThatThrownBy(() -> processor.processPayload(PAYLOAD))
            .isInstanceOf(ChangeOrderSegmentException.class)
            .hasMessage("No available payload for change request 1 in status INFO_RECEIVED");

        softly.assertThat(backLogCaptor.getResults()).hasSize(1);
    }

    @Test
    @DisplayName("Ошибка: невалидный PAYLOAD")
    @DatabaseSetup("/orders/update_recipient/segment_request_invalid_payload.xml")
    void updateOrderRecipientWithInvalidPayload() {
        softly.assertThatThrownBy(() -> processor.processPayload(PAYLOAD))
            .isInstanceOf(ChangeOrderSegmentException.class)
            .hasMessage("Payload has validation errors");

        softly.assertThat(backLogCaptor.getResults()).hasSize(1);
    }

    @Test
    @DisplayName("Ошибка: невалидный сегмент")
    @DatabaseSetup("/orders/update_recipient/segment_request_invalid_segment.xml")
    void updateOrderDeliveryDateWithInvalidSegment() {
        softly.assertThatThrownBy(() -> processor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Order (1) recipient updating is unsupported for a segment with type FULFILLMENT.");

        softly.assertThat(backLogCaptor.getResults()).hasSize(1);
    }

    @Nonnull
    private Recipient createRecipient() {
        return new Recipient.RecipientBuilder(
            new Person.PersonBuilder("Ivan", "Ivanov").setPatronymic("Ivanovich").build(),
            List.of(
                new Phone.PhoneBuilder("+79231234567").build(),
                new Phone.PhoneBuilder("+79237654321").build()
            )
        )
            .setEmail("test@mail.ru")
            .build();
    }

    @Nonnull
    private PersonalRecipient createPersonalRecipient() {
        return new PersonalRecipient.RecipientBuilder(
            "personal-fullname-id",
            List.of(
                new PersonalPhone("personal-phone-id-1", null),
                new PersonalPhone("+79237654321", null)
            )
        )
            .setPersonalEmailId("personal-email-id")
            .build();
    }
}
