package ru.yandex.market.checkout.pushapi.error.validate;

import ru.yandex.market.checkout.pushapi.shop.validate.ValidationException;

public interface Validator<T> {
    
    void validate(T object) throws ValidationException;
    
}
