package ru.yandex.http.test;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpRequestHandler;

import ru.yandex.function.GenericConsumer;
import ru.yandex.function.NullConsumer;
import ru.yandex.http.util.NotImplementedException;

public class ExpectingHeaderHttpItem extends ValidatingHttpItem {
    public ExpectingHeaderHttpItem(
        final HttpRequestHandler handler,
        final String headerName,
        final String headerValue)
    {
        this(handler, new BasicHeader(headerName, headerValue));
    }

    public ExpectingHeaderHttpItem(
        final HttpRequestHandler handler,
        final Header... headers)
    {
        super(handler, join(headers));
    }

    public static GenericConsumer<HttpRequest, NotImplementedException> join(
        final Header... headers)
    {
        GenericConsumer<HttpRequest, NotImplementedException> validator =
            NullConsumer.genericInstance();
        for (Header header: headers) {
            validator = validator.andThen(new HeaderValidator(header));
        }
        return validator;
    }
}

