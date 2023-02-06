package ru.yandex.http.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

import ru.yandex.collection.Pattern;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericSupplier;
import ru.yandex.http.server.sync.BaseHttpServer;
import ru.yandex.http.server.sync.ContentProducerWriter;
import ru.yandex.http.server.sync.ContentWriter;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.NotImplementedException;
import ru.yandex.http.util.server.AbstractHttpServer;
import ru.yandex.http.util.server.DefaultHttpServerFactory;
import ru.yandex.http.util.server.HttpServer;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.uri.UriParser;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.TestBase;

public class StaticServer extends BaseHttpServer<ImmutableBaseServerConfig> {
    private static final ContentType TEXT_PLAIN =
        ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8);
    private static final UnaryOperator<String> URI_DECODER =
        uri -> new UriParser(uri).toString();

    private final AtomicInteger accessCount = new AtomicInteger();
    private final Map<String, HttpResource> data = new HashMap<>();
    private final UnaryOperator<String> uriPreprocessor;

    public StaticServer(final ImmutableBaseServerConfig config)
        throws IOException
    {
        this(config, URI_DECODER);
    }

    public StaticServer(
        final ImmutableBaseServerConfig config,
        final UnaryOperator<String> uriPreprocessor)
        throws IOException
    {
        super(config);
        this.uriPreprocessor = uriPreprocessor;
        register(new Pattern<>("", true), new Handler());
    }

    public static StaticServer fromContext(
        final TestBase testBase,
        final String shortName,
        final String hostEnvVar,
        final GenericAutoCloseableChain<IOException> chain)
        throws IOException
    {
        return testBase.contextResource(
            hostEnvVar,
            new StaticServerSupplier(shortName, hostEnvVar, chain));
    }

    public void add(final String uri, final int status) {
        add(uri, status, (HttpEntity) null);
    }

    public void add(final String uri, final String body) {
        add(uri, HttpStatus.SC_OK, body);
    }

    public void add(
        final String uri,
        final int statusCode,
        final String body)
    {
        add(uri, statusCode, new StringEntity(body, TEXT_PLAIN));
    }

    public void add(
        final String uri,
        final String body,
        final ContentType contentType)
    {
        add(uri, new StringEntity(body, contentType));
    }

    public void add(final String uri, final File file) {
        add(uri, file, TEXT_PLAIN);
    }

    public void add(
        final String uri,
        final File file,
        final ContentType contentType)
    {
        add(uri, new FileEntity(file, contentType));
    }

    @SuppressWarnings("overloads")
    public void add(final String uri, final ContentWriter writer) {
        add(uri, HttpStatus.SC_OK, writer);
    }

    @SuppressWarnings("overloads")
    public void add(
        final String uri,
        final int statusCode,
        final ContentWriter writer)
    {
        add(
            uri,
            statusCode,
            new ContentProducerWriter(writer, StandardCharsets.UTF_8));
    }

    @SuppressWarnings("overloads")
    public void add(final String uri, final ContentProducer producer) {
        add(uri, HttpStatus.SC_OK, producer);
    }

    @SuppressWarnings("overloads")
    public void add(
        final String uri,
        final int statusCode,
        final ContentProducer producer)
    {
        EntityTemplate entity = new EntityTemplate(producer);
        entity.setChunked(true);
        add(uri, statusCode, entity);
    }

    public void add(final String uri, final HttpEntity entity) {
        add(uri, HttpStatus.SC_OK, entity);
    }

    public void add(
        final String uri,
        final int statusCode,
        final HttpEntity entity)
    {
        add(uri, new StaticHttpResource(statusCode, entity));
    }

    public void add(final String uri, final HttpRequestHandler... handlers) {
        add(uri, new ChainedHttpResource(handlers));
    }

    public void add(final String uri, final HttpResource resource) {
        data.put(URI_DECODER.apply(uri), resource);
    }

    private String findNearestUri(final String inputUri) {
        String uri = uriPreprocessor.apply(inputUri);
        String minDiff = null;

        if (data.size() <= 0) {
            return null;
        }

        for (String registered: data.keySet()) {
            String diff = StringChecker.compare(registered, uri);
            if (diff == null) {
                continue;
            }

            if (minDiff == null || diff.length() < minDiff.length()) {
                minDiff = diff;
            }
        }

        return minDiff;
    }

    private HttpResource findResource(final String inputUri) {
        String uri = uriPreprocessor.apply(inputUri);
        HttpResource res = data.get(uri);
        if (res == null) {
            int maxPrefixLen = -1;
            for (Map.Entry<String, HttpResource> entry: data.entrySet()) {
                String key = entry.getKey();
                int len = key.length() - 1;
                if (key.charAt(len) == '*') {
                    key = key.substring(0, len);
                    if (uri.startsWith(key) && len > maxPrefixLen) {
                        res = entry.getValue();
                        maxPrefixLen = len;
                    }
                }
            }
        }
        return res;
    }

    public int accessCount(final String uri) {
        HttpResource res = findResource(uri);
        if (res == null) {
            return 0;
        } else {
            return res.accessCount();
        }
    }

    public int accessCount() {
        return accessCount.get();
    }

    public List<Throwable> exceptions(final String uri) {
        HttpResource res = findResource(uri);
        if (res == null) {
            return Collections.emptyList();
        } else {
            return res.exceptions();
        }
    }

    private class Handler implements HttpRequestHandler {
        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            accessCount.incrementAndGet();
            HttpResource res = findResource(request.getRequestLine().getUri());
            String message;
            if (res == null) {
                message = "Resource " + request + " not found";

                String nearestDiff =
                    findNearestUri(request.getRequestLine().getUri());
                if (nearestDiff != null) {
                    message += '\n' + nearestDiff + '\n';
                }
            } else {
                try {
                    HttpRequestHandler handler = res.next();
                    if (handler == null) {
                        message = "No items left for resource: " + request;
                    } else {
                        message = null;
                        handler.handle(request, response, context);
                    }
                } catch (Throwable t) {
                    res.exception(t);
                    throw t;
                }
            }

            if (message == null) {
                if (request instanceof HttpEntityEnclosingRequest) {
                    EntityUtils.consume(
                        ((HttpEntityEnclosingRequest) request).getEntity());
                }
            } else {
                if (request instanceof HttpEntityEnclosingRequest) {
                    message =
                        message + " with body: '"
                        + CharsetUtils.toString(
                            ((HttpEntityEnclosingRequest) request).getEntity())
                        + '\'';
                }
                throw new NotImplementedException(message);
            }
        }
    }

    private static class StaticServerSupplier
        implements GenericSupplier<StaticServer, IOException>
    {
        private final String shortName;
        private final String hostEnvVar;
        private final GenericAutoCloseableChain<IOException> chain;

        StaticServerSupplier(
            final String shortName,
            final String hostEnvVar,
            final GenericAutoCloseableChain<IOException> chain)
        {
            this.shortName = shortName;
            this.hostEnvVar = hostEnvVar;
            this.chain = chain;
        }

        @Override
        public StaticServer get() throws IOException {
            ImmutableBaseServerConfig config;
            try {
                config = Configs.baseConfig(shortName);
            } catch (ConfigException e) {
                throw new IOException(e);
            }

            StaticServer server = new StaticServer(config);
            chain.add(server);

            server.start();
            System.setProperty(hostEnvVar, server.host().toString());
            return server;
        }
    }

    public static void main(final String... args)
        throws ConfigException, IOException
    {
        DefaultHttpServerFactory<ImmutableBaseServerConfig> serverFactory =
            new DefaultHttpServerFactory<>();
        switch (args.length) {
            case 1:
                AbstractHttpServer.main(args);
                break;
            case 2:
                HttpServer<ImmutableBaseServerConfig, Object> server =
                    main(serverFactory, new String[]{args[0]});
                if (args[1].equals("sleep")) {
                    server.register(
                        new Pattern<>("", true),
                        new SlowpokeHttpItem(StaticHttpItem.OK, 120000L));
                } else {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    try (FileInputStream in = new FileInputStream(args[1])) {
                        IOStreamUtils.copy(in, out);
                    }
                    server.register(
                        new Pattern<>("", true),
                        new StaticHttpItem(
                            HttpStatus.SC_OK,
                            new ByteArrayEntity(
                                out.toByteArray(),
                                ContentType.APPLICATION_OCTET_STREAM)));
                }
                break;
            default:
                System.err.println(
                    "Usage: " + serverFactory.name()
                    + " <config file> [static file]");
                System.exit(1);
                break;
        }
    }
}

