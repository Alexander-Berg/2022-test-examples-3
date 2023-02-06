package ru.yandex.market.checkout.pushapi.shop.validate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeclineReason;

public class OrderResponseValidatorTest {

    private OrderResponseValidator validator = new OrderResponseValidator();

    @Test
    public void testValidateOk() throws Exception {
        validator.validate(new OrderResponse(false, DeclineReason.OTHER));
        validator.validate(new OrderResponse(false, DeclineReason.OUT_OF_DATE));
        validator.validate(new OrderResponse("1", true, null));
    }

    @Test
    public void testShouldNotBeNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(null);
        });
    }

    @Test
    public void testNullAccepted() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(new OrderResponse(null, DeclineReason.OTHER));
        });
    }

    @Test
    public void testNullDeclineReasonIfDeclined() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(new OrderResponse(false, null));
        });
    }

    @Test
    public void testNotNullDeclineReasonIfAccepted() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(new OrderResponse(true, DeclineReason.OUT_OF_DATE));
        });
    }
}
