package ru.yandex.market.logistic.api.client.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.delivery.PersonalDataStatus;
import ru.yandex.market.logistic.api.model.delivery.Recipient;
import ru.yandex.market.logistic.api.model.delivery.response.UpdateRecipientResponse;
import ru.yandex.market.logistic.api.utils.delivery.DtoFactory;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.utils.delivery.DtoFactory.createOrderId;

class UpdateRecipientTest extends CommonServiceClientTest {

    @Autowired
    private DeliveryServiceClient deliveryServiceClient;

    @Test
    void testUpdateRecipientSucceeded() throws Exception {
        prepareMockServiceNormalized("ds_update_recipient", PARTNER_URL);

        UpdateRecipientResponse response = deliveryServiceClient.updateRecipient(
            createOrderId(),
            getRecipientWithPersonalDataStatus(),
            DtoFactory.createPersonalRecipient(),
            getPartnerProperties());
        assertions.assertThat(response)
            .as("Asserting that response is not null")
            .isNotNull();
    }

    @Test
    void testUpdateRecipientFailed() throws Exception {
        prepareMockServiceNormalized("ds_update_recipient_with_errors", PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.updateRecipient(
                createOrderId(),
                getRecipientWithPersonalDataStatus(),
                null,
                getPartnerProperties())
        );
    }

    private Recipient getRecipientWithPersonalDataStatus() {
        return DtoFactory.createRecipient(PersonalDataStatus.GATHERED);
    }
}
