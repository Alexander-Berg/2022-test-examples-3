package ru.yandex.market.checkout.pushapi.error.validate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.pushapi.shop.validate.ValidationException;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

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
        validator.validate(createOrder(1234, OrderStatus.UNPAID, OrderSubstatus.WAITING_USER_INPUT));
        validator.validate(createOrder(1234, OrderStatus.CANCELLED, OrderSubstatus.RESERVATION_EXPIRED));
        validator.validate(createOrder(1234, OrderStatus.CANCELLED, OrderSubstatus.SHOP_FAILED));
        validator.validate(createOrder(1234, OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND));
        validator.validate(createOrder(1234, OrderStatus.CANCELLED, OrderSubstatus.USER_NOT_PAID));
        validator.validate(createOrder(1234, OrderStatus.CANCELLED, OrderSubstatus.USER_REFUSED_DELIVERY));
        validator.validate(createOrder(1234, OrderStatus.CANCELLED, OrderSubstatus.USER_REFUSED_PRODUCT));
        validator.validate(createOrder(1234, OrderStatus.CANCELLED, OrderSubstatus.USER_REFUSED_QUALITY));
        validator.validate(createOrder(1234, OrderStatus.CANCELLED, OrderSubstatus.USER_UNREACHABLE));
        validator.validate(createOrder(1234, OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION));
        validator.validate(createOrder(1234, OrderStatus.PENDING, null));
        validator.validate(createOrder(1234, OrderStatus.PROCESSING, OrderSubstatus.PACKAGING));
        validator.validate(createOrder(1234, OrderStatus.PROCESSING, null));
    }

    @Test
    public void testOrderStatusChangeIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(null);
        });
    }

    @Test
    public void testOrderIdIsZero() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(createOrder(0, OrderStatus.DELIVERED));
        });
    }

    @Test
    public void testOrderIdIsNegative() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(createOrder(-1, OrderStatus.CANCELLED));
        });
    }

    @Test
    public void testCancelledStatusDoesntHaveSubstatus() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(createOrder(1234, OrderStatus.CANCELLED));
        });
    }

    @Test
    public void shouldNotFailedOnDeliverySubstatus() {
        validator.validate(createOrder(1234, OrderStatus.DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED));
    }

    @Test
    public void shouldSetNullSubstatus() {
        Order order = createOrder(1234, OrderStatus.DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);
        validator.validate(order);

        assertThat(order.getSubstatus(), nullValue());
    }
}
