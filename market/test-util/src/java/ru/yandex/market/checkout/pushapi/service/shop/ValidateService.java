package ru.yandex.market.checkout.pushapi.service.shop;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.pushapi.client.entity.shop.Settings;
import ru.yandex.market.checkout.pushapi.error.validate.CartValidator;
import ru.yandex.market.checkout.pushapi.error.validate.OrderValidator;
import ru.yandex.market.checkout.pushapi.error.validate.SettingsValidator;
import ru.yandex.market.checkout.pushapi.error.validate.StatusChangeValidator;
import ru.yandex.market.checkout.pushapi.shop.validate.CartResponseValidator;
import ru.yandex.market.checkout.pushapi.shop.validate.OrderResponseValidator;

@Component
public class ValidateService {
    
    private CartResponseValidator cartResponseValidator;
    private OrderResponseValidator orderResponseValidator;
    private CartValidator cartValidator;
    private OrderValidator orderValidator;
    private StatusChangeValidator statusChangeValidator;
    private SettingsValidator settingsValidator;

    @Autowired
    public void setCartResponseValidator(CartResponseValidator cartResponseValidator) {
        this.cartResponseValidator = cartResponseValidator;
    }

    @Autowired
    public void setOrderResponseValidator(OrderResponseValidator orderResponseValidator) {
        this.orderResponseValidator = orderResponseValidator;
    }

    @Autowired
    public void setCartValidator(CartValidator cartValidator) {
        this.cartValidator = cartValidator;
    }

    @Autowired
    public void setOrderValidator(OrderValidator orderValidator) {
        this.orderValidator = orderValidator;
    }

    @Autowired
    public void setStatusChangeValidator(StatusChangeValidator statusChangeValidator) {
        this.statusChangeValidator = statusChangeValidator;
    }

    @Autowired
    public void setSettingsValidator(SettingsValidator settingsValidator) {
        this.settingsValidator = settingsValidator;
    }

    public void validateCartResponse(Cart cart, CartResponse cartResponse) {
        cartResponseValidator.validate(cart, cartResponse);
    }
    
    public void validateOrderResponse(OrderResponse orderResponse) {
        orderResponseValidator.validate(orderResponse);
    }

    public void validateCart(Cart cart) {
        cartValidator.validate(cart);
    }

    public void validateOrder(Order order) {
        orderValidator.validate(order);
    }

    public void validateStatusChange(Order orderStatusChange) {
        orderValidator.validate(orderStatusChange, false);
        statusChangeValidator.validate(orderStatusChange);
    }

    public void validateSettings(Settings settings) {
        settingsValidator.validate(settings);
    }
}
