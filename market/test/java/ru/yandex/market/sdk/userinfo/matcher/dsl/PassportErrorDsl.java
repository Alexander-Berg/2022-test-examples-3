package ru.yandex.market.sdk.userinfo.matcher.dsl;

import org.hamcrest.Matchers;

import ru.yandex.market.sdk.userinfo.domain.Error;
import ru.yandex.market.sdk.userinfo.domain.PassportError;

/**
 * @authror dimkarp93
 */
public class PassportErrorDsl extends ErrorDsl<PassportError> {
    public PassportErrorDsl() {
        super(PassportError.class);
        setType(Error.Type.PASSPORT_EXCEPTION);
    }

    public PassportErrorDsl setId(long id) {
        add("id", Matchers.is(id), PassportError::getId);
        return this;
    }

    public PassportErrorDsl setValue(String value) {
        add("value", Matchers.is(value), PassportError::getValue);
        return this;
    }
}
