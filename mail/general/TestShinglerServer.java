package ru.yandex.client.so.shingler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

import ru.yandex.client.so.shingler.config.ShinglerDataType;
import ru.yandex.collection.Pattern;
import ru.yandex.http.server.sync.BaseHttpServer;
import ru.yandex.http.server.sync.ContentProducerWriter;
import ru.yandex.http.server.sync.ContentWriter;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpResource;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.NotImplementedException;
import ru.yandex.http.util.server.AbstractHttpServer;
import ru.yandex.http.util.server.DefaultHttpServerFactory;
import ru.yandex.http.util.server.HttpServer;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.uri.UriParser;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.StringChecker;

public class TestShinglerServer
    extends BaseHttpServer<ImmutableBaseServerConfig>
{
    private static final ContentType TEXT_PLAIN = ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8);
    private static final ContentType TEXT_JSON = ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8);
    @SuppressWarnings("UnnecessaryLambda")
    private static final UnaryOperator<String> URI_DECODER = uri -> new UriParser(uri).toString();

    private final Map<String, Map<String, HttpResource>> data = new HashMap<>();
    private final Map<String, HttpResource> emptyBodyData = new HashMap<>();
    private final UnaryOperator<String> uriPreprocessor;
    private ShinglerDataType dataType;
    private boolean verbose = false;

    public TestShinglerServer(final ImmutableBaseServerConfig config)
        throws IOException
    {
        this(config, URI_DECODER);
        dataType = ShinglerDataType.TEXT;
    }

    @SuppressWarnings("unused")
    public TestShinglerServer(final ImmutableBaseServerConfig config, final ShinglerDataType dataType)
        throws IOException
    {
        this(config, URI_DECODER);
        this.dataType = dataType;
    }

    public TestShinglerServer(final ImmutableBaseServerConfig config, final UnaryOperator<String> uriPreprocessor)
        throws IOException
    {
        super(config);
        dataType = ShinglerDataType.TEXT;
        this.uriPreprocessor = uriPreprocessor;
        register(new Pattern<>("", true), new TestShinglerServer.Handler());
    }

    @SuppressWarnings("unused")
    public TestShinglerServer(
        final ImmutableBaseServerConfig config,
        final UnaryOperator<String> uriPreprocessor,
        final ShinglerDataType dataType)
        throws IOException
    {
        super(config);
        this.dataType = dataType;
        this.uriPreprocessor = uriPreprocessor;
        register(new Pattern<>("", true), new TestShinglerServer.Handler());
    }

    public ContentType contentType() {
        return dataType == ShinglerDataType.JSON ? TEXT_JSON : TEXT_PLAIN;
    }

    @SuppressWarnings("unused")
    public ShinglerDataType dataType() {
        return dataType;
    }

    @SuppressWarnings("unused")
    public void setDataType(final ShinglerDataType dataType) {
        this.dataType = dataType;
    }

    @SuppressWarnings("unused")
    public void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }

    public void add(final String uri, final int status) {
        add(uri, null, status, (HttpEntity) null);
    }

    public void add(final String uri, final String putBody, final int status) {
        add(uri, putBody, status, (HttpEntity) null);
    }

    public void add(final String uri, final String putBody, final String getBody) {
        add(uri, putBody, HttpStatus.SC_OK, getBody);
    }

    public void add(final String uri, final String putBody, final int statusCode, final String getBody) {
        add(uri, putBody, statusCode, new StringEntity(getBody, contentType()));
    }

    public void add(final String uri, final String putBody, final String getBody, final ContentType contentType) {
        add(uri, putBody, new StringEntity(getBody, contentType));
    }

    public void add(final String uri, final String body, final File file) {
        add(uri, body, file, contentType());
    }

    public void add(final String uri, final String body, final File file, final ContentType contentType) {
        add(uri, body, new FileEntity(file, contentType));
    }

    @SuppressWarnings("overloads")
    public void add(final String uri, final String body, final ContentWriter writer) {
        add(uri, body, HttpStatus.SC_OK, writer);
    }

    @SuppressWarnings("overloads")
    public void add(final String uri, final String body, final int statusCode, final ContentWriter writer) {
        add(uri, body, statusCode, new ContentProducerWriter(writer, StandardCharsets.UTF_8));
    }

    @SuppressWarnings("overloads")
    public void add(final String uri, final String body, final ContentProducer producer) {
        add(uri, body, HttpStatus.SC_OK, producer);
    }

    @SuppressWarnings("overloads")
    public void add(final String uri, final String body, final int statusCode, final ContentProducer producer) {
        EntityTemplate getEntity = new EntityTemplate(producer);
        getEntity.setChunked(true);
        add(uri, body, statusCode, getEntity);
    }

    public void add(final String uri, final String body, final HttpEntity getEntity) {
        add(uri, body, HttpStatus.SC_OK, getEntity);
    }

    public void add(final String uri, final String body, final int statusCode, final HttpEntity getEntity)
    {
        add(uri, body, new StaticHttpResource(statusCode, getEntity));
    }

    public void add(final String uri, final String body, final HttpResource resource) {
        if (body == null) {
            emptyBodyData.put(URI_DECODER.apply(uri), resource);
        } else {
            data.computeIfAbsent(URI_DECODER.apply(uri), x -> new HashMap<>()).put(body, resource);
        }
    }

    private static String findUri(final String uri, final Map<String, ?> data) {
        String minDiff = null;
        for (String registered : data.keySet()) {
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

    private String findNearestUri(final String inputUri) {
        if (data.size() <= 0) {
            return null;
        }
        String uri = uriPreprocessor.apply(inputUri);
        String minDiff = findUri(uri, data);
        if (minDiff == null) {
            if (emptyBodyData.size() <= 0) {
                return null;
            }
            minDiff = findUri(uri, emptyBodyData);
        }
        return minDiff;
    }

    private static Object findResource(final String uri, final Map<String, ?> data) {
        Object res = data.get(uri);
        if (res == null) {
            for (Map.Entry<String, ?> entry : data.entrySet()) {
                String key = entry.getKey();
                if (key.charAt(key.length() - 1) == '*') {
                    key = key.substring(0, key.length() - 1);
                    if (uri.startsWith(key)) {
                        res = entry.getValue();
                        break;
                    }
                }
            }
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    private HttpResource findResource(final String inputUri, final String body) {
        String uri = uriPreprocessor.apply(inputUri);
        if (body == null) {
            return (HttpResource) findResource(uri, emptyBodyData);
        } else {
            Map<String, HttpResource> res = (Map<String, HttpResource>) findResource(uri, data);
            if (res != null) {
                if (verbose) {
                    logger.info(getName() + ": found body(ies) for uri=" + inputUri + ", res=" + res);
                }
                if (dataType == ShinglerDataType.JSON) {
                    JsonChecker checker = new JsonChecker(body);
                    for (final Map.Entry<String, HttpResource> entry : res.entrySet()) {
                        String checkResult = checker.check(entry.getKey());
                        if (verbose) {
                            logger.info(getName() + ": result of comparing bodies=" + checkResult);
                        }
                        if (checkResult == null) {
                            return entry.getValue();
                        }
                    }
                } else {
                    if (verbose) {
                        logger.info(getName() + ": found resource=" + res.get(body) + " for body=" + body
                            + " and res=" + res);
                    }
                    return res.get(body);
                }
            } else {
                logger.info("TestShinglerServer.findResource failed for body=" + body);
            }
        }
        return null;
    }

    public int accessCount(final String uri, final String body) {
        HttpResource res = findResource(uri, body);
        if (res == null) {
            return 0;
        } else {
            return res.accessCount();
        }
    }

    @SuppressWarnings("unused")
    public List<Throwable> exceptions(final String uri, final String body) {
        HttpResource res = findResource(uri, body);
        if (res == null) {
            return Collections.emptyList();
        } else {
            return res.exceptions();
        }
    }

    private class Handler implements HttpRequestHandler {
        @Override
        public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context)
            throws HttpException, IOException
        {
            String message;
            if (request instanceof HttpEntityEnclosingRequest) {
                final HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                ByteArrayOutputStream body = new ByteArrayOutputStream();
                entity.writeTo(body);
                try {
                    message = handle(request, response, context, body.toString());
                } catch (Throwable t) {
                    throw t;
                }
                if (message == null) {
                    EntityUtils.consume(entity);
                } else {
                    message = message + " with body: '" + body + '\'';
                    throw new NotImplementedException(message);
                }
            } else {
                //throw new HttpException("Non POST requests is not supported");
                try {
                    message = handle(request, response, context, null);
                } catch (Throwable t) {
                    throw t;
                }
                if (message != null) {
                    throw new NotImplementedException(message);
                }
            }
        }

        private String handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context,
            final String body)
            throws HttpException, IOException
        {
            String message = null;
            HttpResource res = findResource(request.getRequestLine().getUri(), body);
            if (res == null) {
                message = "Resource " + request + " not found";
                String nearestDiff = findNearestUri(request.getRequestLine().getUri());
                if (nearestDiff != null) {
                    message += '\n' + nearestDiff + '\n';
                }
            } else {
                try {
                    HttpRequestHandler handler = res.next();
                    if (handler == null) {
                        message = "No items left for resource: " + request;
                    } else {
                        handler.handle(request, response, context);
                    }
                } catch (Throwable t) {
                    res.exception(t);
                    throw t;
                }
            }
            return message;
        }
    }

    private static ByteArrayOutputStream loadFile(final String filePath)
        throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (FileInputStream in = new FileInputStream(filePath)) {
            IOStreamUtils.copy(in, out);
        }
        return out;
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
                    main(serverFactory, args[0]);
                final ByteArrayOutputStream inputData = loadFile(args[1]);
                final ByteArrayOutputStream outputData = loadFile(args[2]);
                server.register(
                    new Pattern<>("", true),
                    new ExpectingHttpItem(inputData.toString(), outputData.toString()));
                break;
            default:
                System.err.println("Usage: " + serverFactory.name() + " <config file> [input static file] "
                    + "[output static file]");
                System.exit(1);
                break;
        }
    }
}
