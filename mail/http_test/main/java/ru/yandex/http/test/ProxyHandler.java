package ru.yandex.http.test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.UnaryOperator;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.RequestLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicRequestLine;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import ru.yandex.http.util.BadRequestException;
import ru.yandex.http.util.ByteArrayEntityFactory;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.server.AbstractHttpServer;
import ru.yandex.logger.PrefixedLogger;

public class ProxyHandler implements HttpRequestHandler {
    private final HttpHost targetHost;
    private final UnaryOperator<String> uriPreprocessor;
    protected final String[] proxyHeaders;

    public ProxyHandler(final int port, final String... proxyHeaders) {
        this(port, UnaryOperator.identity(), proxyHeaders);
    }

    public ProxyHandler(
        final int port,
        final UnaryOperator<String> uriPreprocessor,
        final String... proxyHeaders)
    {
        this(new HttpHost("localhost", port), uriPreprocessor, proxyHeaders);
    }

    public ProxyHandler(
        final HttpHost targetHost,
        final String... proxyHeaders)
    {
        this(targetHost, UnaryOperator.identity(), proxyHeaders);
    }

    public ProxyHandler(
        final HttpHost targetHost,
        final UnaryOperator<String> uriPreprocessor,
        final String... proxyHeaders)
    {
        this.targetHost = targetHost;
        this.uriPreprocessor = uriPreprocessor;
        this.proxyHeaders = proxyHeaders;
    }

    protected void copyResponse(final HttpResponse from, final HttpResponse to)
        throws IOException
    {
        to.setStatusLine(from.getStatusLine());
        HttpEntity fromEntity = from.getEntity();
        if (fromEntity != null) {
            ByteArrayEntity toEntity =
                CharsetUtils.toDecodable(fromEntity)
                    .processWith(ByteArrayEntityFactory.INSTANCE);
            toEntity.setContentType(fromEntity.getContentType());
            toEntity.setContentEncoding(fromEntity.getContentEncoding());
            to.setEntity(toEntity);
        }
        for (String headerName: proxyHeaders) {
            Header header = from.getFirstHeader(headerName);
            if (header != null) {
                to.addHeader(header);
            }
        }
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        PrefixedLogger logger =
            (PrefixedLogger) context.getAttribute(AbstractHttpServer.LOGGER);
        HttpRequest next;
        RequestLine initialRequestLine = request.getRequestLine();
        String initialUri = initialRequestLine.getUri();
        HttpHost proxy;
        HttpHost targetHost;
        if (initialUri.startsWith("/")) {
            proxy = null;
            targetHost = this.targetHost;
        } else {
            proxy = this.targetHost;
            try {
                URI uri = new URI(initialUri);
                targetHost = new HttpHost(uri.getHost(), uri.getPort());
            } catch (URISyntaxException e) {
                throw new BadRequestException(e);
            }
        }

        String uri = uriPreprocessor.apply(initialUri);
        logger.info("Proxying " + initialUri + " to " + uri);
        RequestLine requestLine = new BasicRequestLine(
            initialRequestLine.getMethod(),
            uri,
            initialRequestLine.getProtocolVersion());
        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntity entity =
                ((HttpEntityEnclosingRequest) request).getEntity();
            ByteArrayEntity nextEntity = CharsetUtils.toDecodable(entity)
                .processWith(ByteArrayEntityFactory.INSTANCE);
            nextEntity.setContentType(entity.getContentType());
            nextEntity.setContentEncoding(entity.getContentEncoding());
            BasicHttpEntityEnclosingRequest nextRequest =
                new BasicHttpEntityEnclosingRequest(requestLine);
            nextRequest.setEntity(nextEntity);
            next = nextRequest;
        } else {
            next = new BasicHttpRequest(requestLine);
        }

        for (String headerName: proxyHeaders) {
            Header header = request.getFirstHeader(headerName);
            if (header != null) {
                next.addHeader(header);
            }
        }

        try (CloseableHttpClient client =
                HttpClients.custom().setProxy(proxy).build();
            CloseableHttpResponse nextResponse =
                client.execute(targetHost, next))
        {
            copyResponse(nextResponse, response);
        }
    }
}

