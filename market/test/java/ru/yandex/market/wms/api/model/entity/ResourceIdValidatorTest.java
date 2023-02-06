package ru.yandex.market.wms.api.model.entity;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.exception.FulfillmentApiException;
import ru.yandex.market.wms.common.spring.utils.ResourceIdValidator;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResourceIdValidatorTest {

    @Test
    public void validate() {
        ResourceId resourceId = ResourceId.builder()
                .setPartnerId("partnerId")
                .setYandexId("yandexId")
                .build();

        ResourceIdValidator.validate(resourceId);
        assertTrue(true);
    }

    @Test
    public void validateNullPartnerId() {
        ResourceId resourceId = ResourceId.builder()
                .setPartnerId(null)
                .setYandexId("yandexId")
                .build();

        Exception exception = assertThrows(FulfillmentApiException.class, () -> {
            ResourceIdValidator.validate(resourceId);
        });
        assertTrue(exception.getMessage().contains("'partnerId' of the given ResourceId must not be blank'"));
    }

    @Test
    public void validateNullYandexId() {
        ResourceId resourceId = ResourceId.builder()
                .setPartnerId("partnerId")
                .setYandexId(null)
                .build();

        Exception exception = assertThrows(FulfillmentApiException.class, () -> {
            ResourceIdValidator.validate(resourceId);
        });
        assertTrue(exception.getMessage().contains("'yandexId' of the given ResourceId must not be blank'"));
    }

    @Test
    public void validateNull() {
        Exception exception = assertThrows(FulfillmentApiException.class, () -> {
            ResourceIdValidator.validate(null);
        });
        assertTrue(exception.getMessage().contains("The given ResourceId must not be null"));
    }
}
