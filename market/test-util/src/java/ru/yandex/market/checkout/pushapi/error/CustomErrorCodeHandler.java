package ru.yandex.market.checkout.pushapi.error;

import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.common.web.ExceptionHandler;

public class CustomErrorCodeHandler implements ExceptionHandler<ErrorCodeException> {
    @Override
    public ErrorCodeException handle(ErrorCodeException e) {
        return e;
    }
}
