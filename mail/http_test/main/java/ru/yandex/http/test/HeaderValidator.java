package ru.yandex.http.test;

import java.util.Objects;

import org.apache.http.Header;
import org.apache.http.HttpRequest;

import ru.yandex.function.GenericConsumer;
import ru.yandex.http.util.NotImplementedException;

public class HeaderValidator
    implements GenericConsumer<HttpRequest, NotImplementedException>
{
    private final Header expected;

    public HeaderValidator(final Header expected) {
        this.expected = expected;
    }

    @Override
    public void accept(final HttpRequest request)
        throws NotImplementedException
    {
        Header actual = request.getFirstHeader(expected.getName());
        String value;
        if (actual == null) {
            value = null;
        } else {
            value = actual.getValue();
        }
        if (!Objects.equals(expected.getValue(), value)) {
            throw new NotImplementedException(
                "Expected '" + expected + "', got '" + actual + '\'');
        }
    }
}

