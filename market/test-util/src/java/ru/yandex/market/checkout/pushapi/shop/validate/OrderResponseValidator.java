package ru.yandex.market.checkout.pushapi.shop.validate;

import org.springframework.stereotype.Component;
import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.pushapi.error.validate.Validator;

import static ru.yandex.market.checkout.pushapi.shop.validate.Validate.isNull;
import static ru.yandex.market.checkout.pushapi.shop.validate.Validate.isTrue;
import static ru.yandex.market.checkout.pushapi.shop.validate.Validate.notNull;

@Component
public class OrderResponseValidator implements Validator<OrderResponse> {

    public static final int MAX_ORDER_ID_LENGTH = 50;

    @Override
    public void validate(OrderResponse object) throws ValidationException {
        notNull(object, "orderResponse is null");
        notNull(object.isAccepted(), "accepted is null");
        if(object.isAccepted()) {
            final String orderId = object.getId();
            notNull(orderId, "orderResponse.id is null");
            isTrue(
                orderId.length() <= MAX_ORDER_ID_LENGTH,
                "length of orderResponse.id is greater than " + MAX_ORDER_ID_LENGTH
            );
            isNull(object.getReason(), "accepted=true and declineReason is not null: " + object.getReason());
        } else {
            notNull(object.getReason(), "accepted=false and declineReason is null");
        }
    }
    
}
