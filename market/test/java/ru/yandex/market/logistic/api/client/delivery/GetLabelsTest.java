package ru.yandex.market.logistic.api.client.delivery;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.delivery.response.GetLabelsResponse;
import ru.yandex.market.logistic.api.utils.delivery.DtoFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetLabelsTest extends CommonServiceClientTest {

    @Autowired
    private DeliveryServiceClient deliveryServiceClient;

    @Test
    void testGetLabelsSucceeded() throws Exception {
        prepareMockServiceNormalized("ds_get_labels", PARTNER_URL);

        GetLabelsResponse response = deliveryServiceClient.getLabels(
            Collections.singletonList(DtoFactory.createOrderId()),
            getPartnerProperties()
        );
        assertEquals("test html", response.getHtml());
    }

    @Test
    void testGetLabelsWithErrors() throws Exception {
        prepareMockServiceNormalized("ds_get_labels_with_errors", PARTNER_URL);

        assertThrows(RequestStateErrorException.class, () ->
            deliveryServiceClient.getLabels(
                Collections.singletonList(DtoFactory.createOrderId()),
                getPartnerProperties()
            ));
    }

    @Test
    void testGetLabelsValidationFailed() {
        assertThrows(ValidationException.class, () -> deliveryServiceClient.getLabels(null, getPartnerProperties()));
    }
}
