package ru.yandex.http.test;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpContext;

import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.NotImplementedException;
import ru.yandex.http.util.ServerException;
import ru.yandex.test.util.Checker;
import ru.yandex.test.util.StringChecker;

public class ExpectingHttpItem extends StaticHttpItem {
    private final Checker expected;

    public ExpectingHttpItem(final String expected) {
        this(new StringChecker(expected));
    }

    public ExpectingHttpItem(final Checker expected) {
        this(expected, HttpStatus.SC_OK);
    }

    public ExpectingHttpItem(final String expected, final int statusCode) {
        this(new StringChecker(expected), statusCode);
    }

    public ExpectingHttpItem(final Checker expected, final int statusCode) {
        super(statusCode);
        this.expected = expected;
    }

    public ExpectingHttpItem(final String expected, final String body) {
        this(new StringChecker(expected), body);
    }

    public ExpectingHttpItem(final Checker expected, final String body) {
        this(expected, HttpStatus.SC_OK, body);
    }

    public ExpectingHttpItem(
        final Checker expected,
        final int statusCode,
        final String body)
    {
        super(statusCode, body);
        this.expected = expected;
    }

    public ExpectingHttpItem(
        final Checker expected,
        final int statusCode,
        final HttpEntity entity)
    {
        super(statusCode, entity);
        this.expected = expected;
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        if (expected == null) {
            if (request instanceof HttpEntityEnclosingRequest) {
                throw new ServerException(
                    HttpStatus.SC_METHOD_NOT_ALLOWED,
                    "GET request expected");
            }
        } else {
            if (request instanceof HttpEntityEnclosingRequest) {
                String body = CharsetUtils.toString(
                    ((HttpEntityEnclosingRequest) request).getEntity());
                String result = expected.check(body);
                if (result != null) {
                    throw new NotImplementedException(
                        "For '" + request.getRequestLine().getUri()
                        + '\'' + ' ' + result);
                }
            } else {
                throw new NotImplementedException(
                    "Entity enclosing request expected");
            }
        }
        super.handle(request, response, context);
    }
}

