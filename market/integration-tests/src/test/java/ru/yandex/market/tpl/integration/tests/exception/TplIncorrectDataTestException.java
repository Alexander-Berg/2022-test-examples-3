package ru.yandex.market.tpl.integration.tests.exception;

import org.springframework.http.HttpStatus;

import ru.yandex.market.tpl.common.util.exception.TplErrorCode;
import ru.yandex.market.tpl.common.util.exception.TplException;

public class TplIncorrectDataTestException extends TplException {
    public TplIncorrectDataTestException(String message, Object... args) {
        super(HttpStatus.BAD_REQUEST.value(),
                TplErrorCode.INVALID_PARAM.name(),
                interpolate(message, args),
                null);
    }
}
