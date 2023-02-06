package ru.yandex.market.logistic.api.model.fulfillment.request;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OutboundStatusHistory;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.Status;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.StatusCode;
import ru.yandex.market.logistic.api.utils.DateTime;

import static ru.yandex.market.logistic.api.TestUtils.VALIDATOR;

public class PushOutboundStatusHistoryRequestTest {

    private static final int HISTORY_MAX_SIZE = 100;

    private final SoftAssertions assertions = new SoftAssertions();

    @Test
    void testEmptyOutboundStatusHistoriesValidation() {
        PushOutboundStatusHistoryRequest pushOutboundStatusHistoryRequest =
            new PushOutboundStatusHistoryRequest(Collections.emptyList());

        Set<ConstraintViolation<PushOutboundStatusHistoryRequest>> constraintViolations =
            VALIDATOR.validate(pushOutboundStatusHistoryRequest);

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
    void testOutboundStatusHistoriesMaxSizeValidation() {
        PushOutboundStatusHistoryRequest pushOutboundStatusHistoryRequest = new PushOutboundStatusHistoryRequest(
            Collections.nCopies(HISTORY_MAX_SIZE + 1, new OutboundStatusHistory(
                new ResourceId.ResourceIdBuilder().build(),
                Collections.nCopies(5, new Status(StatusCode.ERROR, DateTime.fromLocalDateTime(LocalDateTime.MIN)))
            ))
        );

        Set<ConstraintViolation<PushOutboundStatusHistoryRequest>> constraintViolations =
            VALIDATOR.validate(pushOutboundStatusHistoryRequest);

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
    void testValidOutboundStatusHistoriesValidation() {
        PushOutboundStatusHistoryRequest pushOutboundStatusHistoryRequest = new PushOutboundStatusHistoryRequest(
            Collections.nCopies(HISTORY_MAX_SIZE - 1, new OutboundStatusHistory(
                new ResourceId.ResourceIdBuilder().build(),
                Collections.nCopies(5, new Status(StatusCode.ERROR, DateTime.fromLocalDateTime(LocalDateTime.MIN)))
            ))
        );

        Set<ConstraintViolation<PushOutboundStatusHistoryRequest>> constraintViolations =
            VALIDATOR.validate(pushOutboundStatusHistoryRequest);

        assertions.assertThat(constraintViolations)
            .as("Asserting constraintViolations is empty")
            .isEmpty();

        assertions.assertAll();
    }
}
