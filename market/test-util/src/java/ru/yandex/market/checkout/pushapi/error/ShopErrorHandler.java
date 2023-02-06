package ru.yandex.market.checkout.pushapi.error;

import org.springframework.http.HttpStatus;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.common.web.ExceptionHandler;
import ru.yandex.market.checkout.pushapi.client.error.ShopErrorException;

public class ShopErrorHandler implements ExceptionHandler<ShopErrorException> {
    @Override
    public ErrorCodeException handle(ShopErrorException throwable) {
        return new ErrorCodeException(
            throwable.getCode().toString(),
            throwable.getMessage(),
            HttpStatus.BAD_GATEWAY.value()
        );
    }
}
