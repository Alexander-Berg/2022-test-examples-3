package ru.yandex.market.sdk.userinfo.matcher.dsl;

import org.hamcrest.Matchers;

import ru.yandex.market.sdk.userinfo.domain.PassportResponse;

/**
 * @authror dimkarp93
 */
public class ExceptionDsl extends MatcherDsl<PassportResponse.Exception> {
    public ExceptionDsl() {
        super(PassportResponse.Exception.class);
    }

    public ExceptionDsl setId(long id) {
        add("id", Matchers.is(id), PassportResponse.Exception::getId);
        return this;
    }

    public ExceptionDsl setValue(String value) {
        add("value", Matchers.is(value), PassportResponse.Exception::getValue);
        return this;
    }
}
