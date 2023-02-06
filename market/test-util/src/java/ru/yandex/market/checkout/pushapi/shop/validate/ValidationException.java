package ru.yandex.market.checkout.pushapi.shop.validate;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
