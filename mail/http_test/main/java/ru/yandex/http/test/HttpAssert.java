package ru.yandex.http.test;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.stream.EntityState;
import org.apache.james.mime4j.stream.MimeTokenStream;
import org.junit.Assert;

import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexReasonPhraseCatalog;
import ru.yandex.http.util.server.HttpServer;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.json.dom.JsonList;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.parser.JsonException;
import ru.yandex.json.writer.JsonType;
import ru.yandex.mail.mime.DefaultMimeConfig;
import ru.yandex.mail.mime.OverwritingBodyDescriptorBuilder;
import ru.yandex.mail.mime.Utf8FieldBuilder;
import ru.yandex.test.util.Checker;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.YandexAssert;

public final class HttpAssert {
    private static final String LOCALHOST = "localhost";
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final int MAX_UNISTAT_SIGNALS = 1000;
    private static final String STAT = "Stat ";

    private HttpAssert() {
    }

    public static String body(final HttpResponse response) {
        try {
            return CharsetUtils.toString(response.getEntity());
        } catch (HttpException | IOException e) {
            throw new AssertionError("Failed to read response body", e);
        }
    }

    public static void assertStatusCode(
        final int expected,
        final HttpResponse response)
    {
        int status = response.getStatusLine().getStatusCode();
        if (status != expected) {
            StringBuilder sb = new StringBuilder("Expected ");
            sb.append(expected);
            sb.append(' ');
            sb.append(YandexReasonPhraseCatalog.INSTANCE.getReason(
                expected,
                Locale.ENGLISH));
            sb.append(" but received ");
            sb.append(response);
            String body = body(response);
            if (!body.isEmpty()) {
                sb.append(':');
                sb.append(' ');
                sb.append(body);
            }
            Assert.fail(sb.toString());
        }
    }

    public static void assertStatusCode(
        final int expected,
        final CloseableHttpClient client,
        final HttpUriRequest request)
        throws IOException
    {
        try (CloseableHttpResponse response = client.execute(request)) {
            assertStatusCode(expected, response);
            CharsetUtils.consume(response.getEntity());
        }
    }

    public static void assertStatusCode(
        final int expected,
        final HttpUriRequest request)
        throws IOException
    {
        try (CloseableHttpClient client = Configs.createDefaultClient()) {
            assertStatusCode(expected, client, request);
        }
    }

    public static void assertStatusCode(
        final int expected,
        final int port,
        final String uri)
        throws IOException
    {
        assertStatusCode(expected, new HttpGet(HTTP_LOCALHOST + port + uri));
    }

    public static void assertHeader(
        final String name,
        final String value,
        final HttpMessage message)
    {
        Header header = message.getFirstHeader(name);
        if (value == null) {
            Assert.assertNull(header);
        } else {
            if (header == null) {
                throw new AssertionError("Header '" + name + "' not found");
            }
            Assert.assertEquals(value, header.getValue());
        }
    }

    public static String stats(final int port) throws IOException {
        return stats(new HttpHost(LOCALHOST, port));
    }

    public static String stats(final HttpHost host) throws IOException {
        try (CloseableHttpClient client = Configs.createDefaultClient()) {
            return stats(client, host);
        }
    }

    public static String stats(
        final CloseableHttpClient client,
        final int port)
        throws IOException
    {
        return stats(client, new HttpHost(LOCALHOST, port));
    }

    public static String stats(
        final CloseableHttpClient client,
        final HttpServer<?, ?> server)
        throws IOException
    {
        return stats(client, server.host());
    }

    public static String stats(
        final CloseableHttpClient client,
        final HttpHost host)
        throws IOException
    {
        try (CloseableHttpResponse response = client.execute(
                new HttpGet(host + "/stat")))
        {
            assertStatusCode(HttpStatus.SC_OK, response);
            return body(response);
        }
    }

    //CSOFF: ParameterNumber
    public static void assertStat(
        final String name,
        final String value,
        final int port,
        final int maxUnistatSignals)
        throws IOException
    {
        assertStat(
            name,
            value,
            new HttpHost(LOCALHOST, port),
            maxUnistatSignals);
    }

    public static void assertStat(
        final String name,
        final String value,
        final HttpServer<?, ?> server,
        final int maxUnistatSignals)
        throws IOException
    {
        assertStat(name, value, stats(server.host()), maxUnistatSignals);
    }

    public static void assertStat(
        final String name,
        final String value,
        final HttpHost host,
        final int maxUnistatSignals)
        throws IOException
    {
        assertStat(name, value, stats(host), maxUnistatSignals);
    }

    public static void assertStat(
        final String name,
        final String value,
        final HttpResponse response,
        final int maxUnistatSignals)
    {
        assertStat(name, value, body(response), maxUnistatSignals);
    }
    //CSON: ParameterNumber

    public static void assertStat(
        final String name,
        final String value,
        final int port)
        throws IOException
    {
        assertStat(
            name,
            value,
            new HttpHost(LOCALHOST, port),
            MAX_UNISTAT_SIGNALS);
    }

    public static void assertStat(
        final String name,
        final String value,
        final HttpServer<?, ?> server)
        throws IOException
    {
        assertStat(name, value, stats(server.host()), MAX_UNISTAT_SIGNALS);
    }

    public static void assertStat(
        final String name,
        final String value,
        final HttpHost host)
        throws IOException
    {
        assertStat(name, value, stats(host), MAX_UNISTAT_SIGNALS);
    }

    public static void assertStat(
        final String name,
        final String value,
        final HttpResponse response)
    {
        assertStat(name, value, body(response), MAX_UNISTAT_SIGNALS);
    }

    public static void assertStat(
        final String name,
        final String value,
        final String body)
    {
        assertStat(name, value, body, MAX_UNISTAT_SIGNALS);
    }

    public static void assertStat(
        final String name,
        final String value,
        final String body,
        final int maxUnistatSignals)
    {
        boolean found = false;
        try {
            JsonObject root = TypesafeValueContentHandler.parse(body);
            JsonList stats = root.asList();
            YandexAssert.assertNotGreater(
                maxUnistatSignals,
                stats.size());
            Set<String> signals = new HashSet<>(stats.size() << 1);
            for (JsonObject statObject: root.asList()) {
                JsonList stat = statObject.asList();
                // Check than we not messed up with histograms once again
                if (stat.size() != 2) {
                    throw new AssertionError(
                        "Malformed signal: "
                        + JsonType.HUMAN_READABLE.toString(stat));
                }
                String signalName = stat.get(0).asString();
                // It is unpredictable, which signal occurence will be used by
                // YASM agent
                if (!signals.add(signalName)) {
                    throw new AssertionError(
                        "Duplicate signal name: " + signalName);
                }
                JsonObject signalValue = stat.get(1);
                if (name.equals(signalName)) {
                    if (value == null) {
                        Assert.fail(STAT + name + " not expected in " + body);
                    } else {
                        String actual;
                        if (signalValue.type() == JsonObject.Type.STRING) {
                            actual = signalValue.asString();
                        } else {
                            actual = JsonType.NORMAL.toString(signalValue);
                        }
                        Assert.assertEquals(
                            STAT + name + " expected: <[" + value
                            + "]> but was:<[" + actual + "]> in " + body,
                            value,
                            actual);
                        found = true;
                    }
                } else if (signalValue.type() != JsonObject.Type.LIST) {
                    // Signal should be either number or histogram
                    signalValue.asDouble();
                }
            }
        } catch (JsonException e) {
            throw new AssertionError("Failed to parse json from: " + body, e);
        }
        if (!found && value != null) {
            throw new AssertionError(
                STAT + name + " not found in: " + body);
        }
    }

    public static void assertStringResponse(
            final CloseableHttpClient client,
            final String uri,
            final String expected)
            throws HttpException, IOException
    {
        assertCheckerResponse(client, new HttpGet(uri), new StringChecker(expected));
    }

    public static void assertJsonResponse(
        final CloseableHttpClient client,
        final String uri,
        final String expected)
        throws HttpException, IOException
    {
        assertJsonResponse(client, new HttpGet(uri), expected);
    }

    public static void assertJsonResponse(
        final CloseableHttpClient client,
        final HttpUriRequest request,
        final String expected)
        throws HttpException, IOException
    {
        assertCheckerResponse(client, request, new JsonChecker(expected));
    }

    public static void assertCheckerResponse(
        final CloseableHttpClient client,
        final HttpUriRequest request,
        final Checker checker)
        throws HttpException, IOException
    {
        try (CloseableHttpResponse response = client.execute(request)) {
            String responseStr =
                CharsetUtils.toString(response.getEntity());

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            YandexAssert.check(checker, responseStr);
        }
    }

    public static void assertMultipart(
        final HttpEntity entity,
        final Checker... checkers)
    {
        assertMultipart(entity, Arrays.asList(checkers));
    }

    public static void assertMultipart(
        final HttpEntity entity,
        final List<? extends Checker> checkers)
    {
        assertMultipart(entity, checkers, Function.identity());
    }

    public static void assertMultipart(
        final HttpEntity entity,
        final List<? extends Checker> checkers,
        final Function<String, String> bodyPreprocessor)
    {
        MimeTokenStream stream = new MimeTokenStream(
            DefaultMimeConfig.INSTANCE,
            null,
            new Utf8FieldBuilder(),
            new OverwritingBodyDescriptorBuilder());
        List<String> parts = new ArrayList<>();
        try {
            stream.parseHeadless(
                entity.getContent(),
                entity.getContentType().getValue());
            EntityState state = stream.getState();
            while (state != EntityState.T_END_OF_STREAM) {
                if (state == EntityState.T_BODY) {
                    try (Reader reader = stream.getReader()) {
                        StringBuilder sb = IOStreamUtils.consume(reader);
                        String body = bodyPreprocessor.apply(new String(sb));
                        if (body != null) {
                            parts.add(body);
                        }
                    }
                }
                state = stream.next();
            }
        } catch (IOException | MimeException e) {
            throw new AssertionError("Failed to parse multipart input", e);
        }
        YandexAssert.check(checkers, parts);
    }

    public static String golovanPanel(final HttpHost host) {
        try (CloseableHttpClient client = Configs.createDefaultClient();
            CloseableHttpResponse response = client.execute(
                new HttpGet(host + "/generate-golovan-panel")))
        {
            assertStatusCode(HttpStatus.SC_OK, response);
            String body = CharsetUtils.toString(response.getEntity());
            JsonObject root = TypesafeValueContentHandler.parse(body);
            JsonList charts = root.get("charts").asList();
            int size = charts.size();
            Map<String, String> ids = new HashMap<>(size << 1);
            Map<String, String> positions = new HashMap<>(size << 1);
            for (int i = 0; i < size; ++i) {
                JsonMap chart = charts.get(i).asMap();
                try {
                    if (chart.getString("text", null) != null) {
                        continue;
                    }
                    String id = chart.getString("id");
                    String title = chart.getString("title");
                    String oldTitle = ids.putIfAbsent(id, title);
                    if (oldTitle != null) {
                        throw new AssertionError(
                            "Duplicate id " + id
                            + " found. Old title <" + oldTitle
                            + ">, new title <" + title
                            + ">, body:\n" + body);
                    }
                    String row = chart.getString("row");
                    String col = chart.getString("col");
                    String position = row + 'x' + col;
                    oldTitle = positions.putIfAbsent(position, title);
                    if (oldTitle != null) {
                        throw new AssertionError(
                            "Duplicate position " + position
                            + " found. Old title <" + oldTitle
                            + ">, new title <" + title
                            + ">, body:\n" + body);
                    }
                } catch (JsonException e) {
                    e.addSuppressed(
                        new JsonException(
                            "Failed to process chart:\n"
                            + JsonType.HUMAN_READABLE.toString(chart)));
                    throw e;
                }
            }
            return body;
        } catch (HttpException | IOException | JsonException e) {
            throw new AssertionError(e);
        }
    }
}

