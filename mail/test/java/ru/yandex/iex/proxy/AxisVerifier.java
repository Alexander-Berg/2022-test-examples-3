package ru.yandex.iex.proxy;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;

import ru.yandex.http.util.BadRequestException;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.parser.JsonException;
import ru.yandex.json.writer.JsonType;

public class AxisVerifier implements HttpRequestHandler {
    private static final long SHARDS = 65534L;
    private final long uid;
    private final String source;
    private boolean reindex = false;
    private final Map<String, Map<String, Object>> solutions;

    AxisVerifier(final long uid, final String source) {
        this.uid = uid;
        this.source = source;
        this.solutions = new LinkedHashMap<>();
    }

    AxisVerifier(
            final long uid,
            final String source,
            final boolean reindex)
    {
        this.uid = uid;
        this.source = source;
        this.reindex = reindex;
        this.solutions = new LinkedHashMap<>();
    }

    public AxisVerifier add(
        final String name,
        final LinkedHashMap<String, Object> solution)
    {
        solutions.put(name, solution);
        return this;
    }

    private void checkSolution(
        final JsonMap json,
        final Map<String, Object> solution)
        throws JsonException
    {
        for (String key : solution.keySet()) {
            System.out.println("key = " + key);
        }
        try {
            if (json.get("mid") == null) {
                throw new BadRequestException(
                    "\"mid\" field is absent in data structure");
            }
            if (json.get("stid") == null) {
                throw new BadRequestException(
                    "\"stid\" field is absent in data structure");
            }
            if (json.get("from") == null) {
                throw new BadRequestException(
                    "\"from\" field is absent in data structure");
            }
            if (json.get("domain") == null) {
                throw new BadRequestException(
                    "\"domain\" field is absent in data structure");
            }
            if (json.get("subject") == null) {
                throw new BadRequestException(
                    "\"subject\" field is absent in data structure");
            }
            if (reindex) {
                String reindex = "reindex";
                System.out.println("CHECKING reindexing");
                System.out.println(
                    "json = " + JsonType.NORMAL.toString(json));
                final String reindexEntity =
                    json.getMapOrNull("data").getOrNull(reindex);
                System.out.println("reindexEntity = " + reindexEntity);
                if (!reindexEntity.equals("true")) {
                    throw new BadRequestException(
                        "reindex param is missed");
                }
            }
        } catch (BadRequestException e) {
            throw new AssertionError("AssertionError:"
                    + " BadRequestException: " + e);
        }
    }

    @SuppressWarnings("StringSplitter")
    private void verifyJson(final String jsonString) throws HttpException {
        try {
            System.out.println("in verifyJson, jsonString = " + jsonString);
            String[] lines = jsonString.split("\n");
            for (String jsonLine : lines) {
                final JsonMap root =
                    TypesafeValueContentHandler.parse(jsonLine).asMap();
                final String factType = root.getString("type");
                final long uid = root.getLong("uid");
                final String source = root.getString("source");
                if (uid != this.uid) {
                    throw new BadRequestException(
                        "uid's missmatch: expected " + this.uid
                        + ", but received: " + uid);
                }
                if (!source.equals(this.source)) {
                    throw new BadRequestException(
                        "source missmatch: expected " + this.source
                        + ", received: " + source);
                }
                final Map<String, Object> solution =
                    solutions.get(factType);
                // if we don't want to include all entitites to test,
                // include just entities to compare
                if (solution != null) {
                    checkSolution(root, solution);
                }
                solutions.remove(factType);
            }
            if (solutions.size() > 0) {
                throw new BadRequestException(
                    "Incomplete solutions list received, "
                    + "unreceived solutions: " + solutions.keySet());
            }
        } catch (JsonException e) {
        //    throw new BadRequestException("Json parse error", e);
            throw new AssertionError("Json parse error", e);
        }
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws IOException, HttpException
    {
        if (request instanceof HttpEntityEnclosingRequest) {
            final String requestBody =
                EntityUtils.toString(
                    ((HttpEntityEnclosingRequest) request).getEntity());
            Assert.assertEquals(Long.parseLong(request.
                    getFirstHeader(YandexHeaders.ZOO_SHARD_ID).getValue()),
                    this.uid % SHARDS);
            verifyJson(requestBody);
        } else {
            throw new BadRequestException("Expected POST request");
        }
    }
}
