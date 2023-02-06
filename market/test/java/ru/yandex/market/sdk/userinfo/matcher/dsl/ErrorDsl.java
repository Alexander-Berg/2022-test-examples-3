package ru.yandex.market.sdk.userinfo.matcher.dsl;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.sdk.userinfo.domain.Error;

/**
 * @authror dimkarp93
 */
public class ErrorDsl<T extends Error> extends MatcherDsl<T> {
    public ErrorDsl() {
        this((Class<T>) Error.class);
    }

    public ErrorDsl(Class<T> clazz) {
        super(clazz);
    }

    public ErrorDsl setType(Error.Type type) {
        add("type", Matchers.is(type), Error::getType);
        return this;
    }

    public ErrorDsl setMessage(String message) {
        return setMessage(Matchers.is(message));
    }

    public ErrorDsl setMessage(Matcher<String> matcher) {
        add("message",  matcher, Error::getMessage);
        return this;
    }
}
