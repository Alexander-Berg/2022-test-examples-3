package ru.yandex.market.checkout.pushapi.error.validate;

import org.springframework.stereotype.Component;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.pushapi.shop.validate.ValidationException;

import static ru.yandex.market.checkout.pushapi.shop.validate.Validate.*;

@Component
public class StatusChangeValidator implements Validator<Order> {

    @Override
    public void validate(Order object) throws ValidationException {
        notNull(object, "orderStatusChange is null");
        notNull(object.getStatus(), "status is null");
        if(object.getId() != null) {
            positive(object.getId(), "orderId is not positive");
        }
        if(object.getStatus() == OrderStatus.CANCELLED) {
            notNull(object.getSubstatus(), "substatus is null while status=CANCELLED");
        } else {
            isNull(object.getSubstatus(), "substatus is not null while status!=CANCELLED");
        }
    }
}
