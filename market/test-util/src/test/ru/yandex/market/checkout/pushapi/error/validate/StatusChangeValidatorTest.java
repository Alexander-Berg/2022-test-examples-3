package ru.yandex.market.checkout.pushapi.error.validate;

import org.junit.Test;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.pushapi.shop.validate.ValidationException;

public class StatusChangeValidatorTest {

    private StatusChangeValidator validator = new StatusChangeValidator();

    private Order createOrder(final long id, final OrderStatus status) {
        return new Order() {{
            setId(id);
            setStatus(status);
        }};
    }

    private Order createOrder(final long id, final OrderStatus status, final OrderSubstatus substatus) {
        return new Order() {{
            setId(id);
            setStatus(status);
            setSubstatus(substatus);
        }};
    }

    @Test
    public void testOk() throws Exception {
        validator.validate(createOrder(1234, OrderStatus.DELIVERED));
        validator.validate(createOrder(1234, OrderStatus.DELIVERY));
        validator.validate(createOrder(1234, OrderStatus.PICKUP));
        validator.validate(createOrder(1234, OrderStatus.PROCESSING));
        validator.validate(createOrder(1234, OrderStatus.RESERVED));
        validator.validate(createOrder(1234, OrderStatus.UNPAID));
        validator.validate(createOrder(1234, OrderStatus.CANCELLED, OrderSubstatus.RESERVATION_EXPIRED));
        validator.validate(createOrder(1234, OrderStatus.CANCELLED, OrderSubstatus.SHOP_FAILED));
        validator.validate(createOrder(1234, OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND));
        validator.validate(createOrder(1234, OrderStatus.CANCELLED, OrderSubstatus.USER_NOT_PAID));
        validator.validate(createOrder(1234, OrderStatus.CANCELLED, OrderSubstatus.USER_REFUSED_DELIVERY));
        validator.validate(createOrder(1234, OrderStatus.CANCELLED, OrderSubstatus.USER_REFUSED_PRODUCT));
        validator.validate(createOrder(1234, OrderStatus.CANCELLED, OrderSubstatus.USER_REFUSED_QUALITY));
        validator.validate(createOrder(1234, OrderStatus.CANCELLED, OrderSubstatus.USER_UNREACHABLE));
    }

    @Test(expected = ValidationException.class)
    public void testOrderStatusChangeIsNull() throws Exception {
        validator.validate(null);
    }

    @Test(expected = ValidationException.class)
    public void testOrderIdIsZero() throws Exception {
        validator.validate(createOrder(0, OrderStatus.DELIVERED));
    }

    @Test(expected = ValidationException.class)
    public void testOrderIdIsNegative() throws Exception {
        validator.validate(createOrder(-1, OrderStatus.CANCELLED));
    }

    @Test(expected = ValidationException.class)
    public void testCancelledStatusDoesntHaveSubstatus() throws Exception {
        validator.validate(createOrder(1234, OrderStatus.CANCELLED));
    }
}
