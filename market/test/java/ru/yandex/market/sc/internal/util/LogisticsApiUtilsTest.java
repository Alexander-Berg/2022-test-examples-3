package ru.yandex.market.sc.internal.util;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.exception.FulfillmentApiException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogisticsApiUtilsTest {

    @Test
    void extractValidOrderId() {
        ResourceId resourceId = new ResourceId("123456", "654321");
        long actual = LogisticsApiUtils.extractOrderIdOrThrow(resourceId);
        assertThat(actual).isEqualTo(654321L);
    }

    @Test
    void tryToExtractNullOrderId() {
        ResourceId resourceId = new ResourceId("123456", "null");
        assertThatThrownBy(() -> LogisticsApiUtils.extractOrderIdOrThrow(resourceId))
                .isExactlyInstanceOf(FulfillmentApiException.class);
    }

    @Test
    void tryToExtractNotNumericOrderId() {
        ResourceId resourceId = new ResourceId("123456", "65z321");
        assertThatThrownBy(() -> LogisticsApiUtils.extractOrderIdOrThrow(resourceId))
                .isExactlyInstanceOf(FulfillmentApiException.class);
    }

    @Test
    void tryToExtractVeryBigNumberOrderId() {
        ResourceId resourceId = new ResourceId("123456", "65321653216532165321653216532165321");
        assertThatThrownBy(() -> LogisticsApiUtils.extractOrderIdOrThrow(resourceId))
                .isExactlyInstanceOf(FulfillmentApiException.class);
    }

}
