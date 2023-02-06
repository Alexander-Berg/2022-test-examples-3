package ru.yandex.http.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.stream.EntityState;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.MimeTokenStream;

import ru.yandex.http.util.BadRequestException;
import ru.yandex.http.util.ByteArrayEntityFactory;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.HeaderUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.http.util.server.AbstractHttpServer;
import ru.yandex.io.DecodableByteArrayOutputStream;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.logger.PrefixedLogger;
import ru.yandex.mail.mime.DefaultMimeConfig;
import ru.yandex.mail.mime.OverwritingBodyDescriptorBuilder;
import ru.yandex.mail.mime.Utf8FieldBuilder;

public class ProxyMultipartHandler extends ProxyHandler {
    private final Set<String> proxyHeadersSet;

    public ProxyMultipartHandler(
        final int port,
        final String... proxyHeaders)
    {
        super(port, proxyHeaders);
        proxyHeadersSet = new HashSet<>(Arrays.asList(proxyHeaders));
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntityEnclosingRequest httpEntityEnclosingRequest =
                (HttpEntityEnclosingRequest) request;
            HttpEntity entity = httpEntityEnclosingRequest.getEntity();
            ContentType contentType = CharsetUtils.contentType(entity);
            if ("multipart/mixed".equals(contentType.getMimeType())) {
                handleMultipart(httpEntityEnclosingRequest, response, context);
                return;
            }
        }
        super.handle(request, response, context);
    }

    private void handleMultipart(
        final HttpEntityEnclosingRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        MimeTokenStream stream = new MimeTokenStream(
            DefaultMimeConfig.INSTANCE,
            null,
            new Utf8FieldBuilder(),
            new OverwritingBodyDescriptorBuilder());
        PrefixedLogger logger =
            (PrefixedLogger) context.getAttribute(AbstractHttpServer.LOGGER);
        HttpEntity entity = request.getEntity();
//        byte[] content = EntityUtils.toByteArray(entity);
//        logger.info(
//            "Processing Multipart\n" + new String(content, StandardCharsets.UTF_8));
        stream.parseHeadless(
            entity.getContent(),
            //new ByteArrayInputStream(content),
            entity.getContentType().getValue());
        EntityState state = stream.getState();
        String uri = null;
        String method = HttpPost.METHOD_NAME;
        List<Header> headers = new ArrayList<>();
        try {
            while (state != EntityState.T_END_OF_STREAM) {
                switch (state) {
                    case T_FIELD:
                        Field field = stream.getField();
                        String name = field.getName();

                        if (YandexHeaders.URI.equals(name)) {
                            uri = field.getBody();
                        } else if (YandexHeaders.ZOO_HTTP_METHOD.equalsIgnoreCase(name)) {
                            method = field.getBody();
                        } else if (proxyHeadersSet.contains(name)) {
                            headers.add(
                                HeaderUtils.createHeader(
                                    name,
                                    field.getBody()));
                        } else {
                            logger.fine("Dropping header " + name);
                        }
                        break;
                    case T_BODY:
                        DecodableByteArrayOutputStream data =
                            IOStreamUtils.consume(
                                stream.getDecodedInputStream());
                        if (uri == null) {
                            uri = request.getRequestLine().getUri();
                        }

                        HttpRequest upstreamRequest;
                        if (HttpPost.METHOD_NAME.equalsIgnoreCase(method)) {
                            BasicHttpEntityEnclosingRequest nextRequest =
                                new BasicHttpEntityEnclosingRequest(
                                    request.getRequestLine().getMethod(),
                                    uri);
                            nextRequest.setEntity(
                                data.processWith(ByteArrayEntityFactory.INSTANCE));
                            upstreamRequest = nextRequest;
                        } else {
                            upstreamRequest = new HttpGet(uri);
                        }

                        for (Header header: headers) {
                            upstreamRequest.addHeader(header);
                        }
                        headers.clear();

                        BasicHttpResponse nextResponse =
                            new BasicHttpResponse(response.getStatusLine());
                        logger.info(
                            "Multipart, sending request to " + uri + " body "
                                + new String(data.toByteArray(), StandardCharsets.UTF_8));
                        super.handle(upstreamRequest, nextResponse, context);
                        StatusLine statusLine = nextResponse.getStatusLine();
                        if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                            copyResponse(nextResponse, response);
                            //return;
                        }
                        break;
                    default:
                        break;
                }
                state = stream.next();
            }
        } catch (MimeException e) {
            throw new BadRequestException(e);
        }
    }
}

