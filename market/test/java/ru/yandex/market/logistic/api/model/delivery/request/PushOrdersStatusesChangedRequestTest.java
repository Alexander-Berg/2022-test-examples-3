package ru.yandex.market.logistic.api.model.delivery.request;

import java.util.Collections;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.request.PushOrdersStatusesChangedRequest;

import static ru.yandex.market.logistic.api.TestUtils.VALIDATOR;

class PushOrdersStatusesChangedRequestTest {

    protected SoftAssertions assertions = new SoftAssertions();

    @Test
    void testValidation() {

        PushOrdersStatusesChangedRequest pushOrdersStatusesChangedRequest =
            new PushOrdersStatusesChangedRequest(Collections.singletonList(new ResourceId.ResourceIdBuilder().build()));

        Set<ConstraintViolation<PushOrdersStatusesChangedRequest>> constraintViolations =
            VALIDATOR.validate(pushOrdersStatusesChangedRequest);

        assertions.assertThat(constraintViolations)
            .as("have to contain violation")
            .isNotEmpty();

        assertions.assertThat(constraintViolations)
            .as("violation message must be specific")
            .extracting(ConstraintViolation::getMessage)
            .contains("At least one of these fields must be passed");
    }
}
