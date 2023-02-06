package ru.yandex.market.checkout.pushapi.shop.validate;

import org.junit.Test;
import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeclineReason;

public class OrderResponseValidatorTest {
    
    private OrderResponseValidator validator = new OrderResponseValidator();

    @Test
    public void testValidateOk() throws Exception {
        validator.validate(new OrderResponse(false, DeclineReason.OTHER));
        validator.validate(new OrderResponse(false, DeclineReason.OUT_OF_DATE));
        validator.validate(new OrderResponse(true, null));
    }

    @Test(expected = ValidationException.class)
    public void testShouldNotBeNull() throws Exception {
        validator.validate(null);
    }

    @Test(expected = ValidationException.class)
    public void testNullAccepted() throws Exception {
        validator.validate(new OrderResponse(null, DeclineReason.OTHER));
    }

    @Test(expected = ValidationException.class)
    public void testNullDeclineReasonIfDeclined() throws Exception {
        validator.validate(new OrderResponse(false, null));
    }

    @Test(expected = ValidationException.class)
    public void testNotNullDeclineReasonIfAccepted() throws Exception {
        validator.validate(new OrderResponse(true, DeclineReason.OUT_OF_DATE));
    }
}
