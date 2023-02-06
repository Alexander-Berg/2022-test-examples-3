package ru.yandex.market.sdk.userinfo.matcher.dsl;

import org.hamcrest.Matchers;

import ru.yandex.market.sdk.userinfo.domain.SberlogError;

/**
 * @authror dimkarp93
 */
public class SberlogErrorDsl extends ErrorDsl<SberlogError> {
    public SberlogErrorDsl() {
        super(SberlogError.class);
    }

    public SberlogErrorDsl setCode(long code) {
        add("code", Matchers.is(code), SberlogError::getCode);
        return this;
    }

}
