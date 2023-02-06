package ru.yandex.market.logistic.api.model.fulfillment.request;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatus;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory;
import ru.yandex.market.logistic.api.utils.DateTime;

import static ru.yandex.market.logistic.api.TestUtils.VALIDATOR;

class PushOrdersStatusHistoryRequestTest {

    private static final int HISTORY_MAX_SIZE = 100;

    private final SoftAssertions assertions = new SoftAssertions();

    @Test
    void testEmptyOrderStatusHistoriesValidation() {
        PushOrdersStatusHistoryRequest pushOrdersStatusHistoryRequest =
                new PushOrdersStatusHistoryRequest(Collections.emptyList());

        Set<ConstraintViolation<PushOrdersStatusHistoryRequest>> constraintViolations =
                VALIDATOR.validate(pushOrdersStatusHistoryRequest);

        assertions.assertThat(constraintViolations)
                .as("Asserting constraintViolations is not empty")
                .isNotEmpty();

        assertions.assertThat(constraintViolations)
                .as("Asserting constraintViolations message must be specific")
                .extracting(ConstraintViolation::getMessage)
                .contains("must not be empty")
                .contains("size must be between 1 and 100");

        assertions.assertAll();
    }

    @Test
    void testOrderStatusHistoriesMaxSizeValidation() {
        PushOrdersStatusHistoryRequest pushOrdersStatusHistoryRequest = new PushOrdersStatusHistoryRequest(
            Collections.nCopies(HISTORY_MAX_SIZE + 1, new OrderStatusHistory(
                Collections.nCopies(5, new OrderStatus(
                    OrderStatusType.ORDER_CANCELLED_FF,
                    DateTime.fromLocalDateTime(LocalDateTime.MIN),
                    "Test message"
                )),
                new ResourceId.ResourceIdBuilder().build()
            ))
        );

        Set<ConstraintViolation<PushOrdersStatusHistoryRequest>> constraintViolations =
                VALIDATOR.validate(pushOrdersStatusHistoryRequest);

        assertions.assertThat(constraintViolations)
                .as("Asserting constraintViolations is not empty")
                .isNotEmpty();

        assertions.assertThat(constraintViolations)
                .as("Asserting constraintViolations message must be specific")
                .extracting(ConstraintViolation::getMessage)
                .contains("size must be between 1 and 100");

        assertions.assertAll();
    }

    @Test
    void testValidOrderStatusHistoriesValidation() {
        PushOrdersStatusHistoryRequest pushOrdersStatusHistoryRequest = new PushOrdersStatusHistoryRequest(
            Collections.nCopies(HISTORY_MAX_SIZE - 1, new OrderStatusHistory(
                Collections.nCopies(5, new OrderStatus(
                    OrderStatusType.ORDER_CANCELLED_FF,
                    DateTime.fromLocalDateTime(LocalDateTime.MIN),
                    "Test message"
                )),
                new ResourceId.ResourceIdBuilder().build()
            ))
        );

        Set<ConstraintViolation<PushOrdersStatusHistoryRequest>> constraintViolations =
                VALIDATOR.validate(pushOrdersStatusHistoryRequest);

        assertions.assertThat(constraintViolations)
                .as("Asserting constraintViolations is empty")
                .isEmpty();

        assertions.assertAll();
    }
}
