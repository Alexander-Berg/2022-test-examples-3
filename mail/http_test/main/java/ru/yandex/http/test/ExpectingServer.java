package ru.yandex.http.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.RequestLine;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

import ru.yandex.collection.Pattern;
import ru.yandex.http.server.sync.BaseHttpServer;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;

public class ExpectingServer
    extends BaseHttpServer<ImmutableBaseServerConfig>
{
    private static final ContentType TEXT_PLAIN =
        ContentType.create("text/plain", StandardCharsets.UTF_8);

    private final Map<String, Deque<String>> expectedData = new HashMap<>();

    public ExpectingServer(final ImmutableBaseServerConfig config)
        throws IOException
    {
        super(config);
        register(new Pattern<>("", true), new Handler());
    }

    public Collection<String> uri(final String uri) {
        return expectedData.computeIfAbsent(uri, x -> new ArrayDeque<>());
    }

    private static int commonPrefixLength(final String lhs, final String rhs) {
        int len = Math.min(lhs.length(), rhs.length());
        for (int i = 0; i < len; ++i) {
            if (lhs.charAt(i) != rhs.charAt(i)) {
                return i;
            }
        }
        return len;
    }

    private static int commonSuffixLength(
        final int off,
        final String lhs,
        final String rhs)
    {
        int len = Math.min(lhs.length(), rhs.length()) - off;
        int lhsLen = lhs.length() - 1;
        int rhsLen = rhs.length() - 1;
        for (int i = 0; i < len; ++i) {
            if (lhs.charAt(lhsLen - i) != rhs.charAt(rhsLen - i)) {
                return i;
            }
        }
        return len;
    }

    private class Handler implements HttpRequestHandler {
        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            RequestLine requestLine = request.getRequestLine();
            Deque<String> data = expectedData.get(requestLine.getUri());
            if (data == null) {
                response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
            } else {
                String expected = data.poll();
                String body;
                if (request instanceof HttpEntityEnclosingRequest) {
                    body = EntityUtils.toString(
                        ((HttpEntityEnclosingRequest) request).getEntity(),
                        StandardCharsets.UTF_8);
                } else {
                    body = "";
                }
                if (!body.equals(expected)) {
                    int prefix = commonPrefixLength(body, expected);
                    int suffix = commonSuffixLength(prefix, body, expected);
                    response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
                    response.setEntity(
                        new StringEntity(
                            "For '" + requestLine.getUri()
                            + "' expected entity is '"
                            + expected.substring(0, prefix) + '['
                            + expected.substring(
                                prefix,
                                expected.length() - suffix)
                            + ']'
                            + expected.substring(expected.length() - suffix)
                            + "', but got '" + body.substring(0, prefix) + '['
                            + body.substring(prefix, body.length() - suffix)
                            + ']' + body.substring(body.length() - suffix)
                            + '\'',
                            TEXT_PLAIN));
                }
            }
        }
    }
}

