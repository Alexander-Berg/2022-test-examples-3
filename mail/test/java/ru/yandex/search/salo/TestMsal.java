package ru.yandex.search.salo;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.Assert;

import ru.yandex.collection.Pattern;
import ru.yandex.dbfields.OracleFields;
import ru.yandex.http.server.sync.BaseHttpServer;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;
import ru.yandex.io.StringBuilderWriter;
import ru.yandex.json.writer.JsonWriter;
import ru.yandex.parser.string.NonEmptyValidator;
import ru.yandex.parser.string.NonNegativeLongValidator;
import ru.yandex.parser.string.PositiveLongValidator;
import ru.yandex.parser.uri.CgiParams;

public class TestMsal
    extends BaseHttpServer<ImmutableBaseServerConfig>
    implements HttpRequestHandler
{
    private static final long DEFAULT_LENGTH = 1L;
    private static final long MILLIS = 1000L;

    private final Map<String, TreeMap<Long, Object>> envelopes =
        new HashMap<>();

    public TestMsal(final ImmutableBaseServerConfig config) throws IOException {
        super(config);
        register(new Pattern<>("/operations-queue-envelopes", false), this);
        register(
            new Pattern<>("/get-min-transaction-date", false),
            new StaticHttpItem(
                "{\"min_transaction_date\":null,"
                + "\"server_timestamp\":1534343444.123}"));
    }

    public Map<String, TreeMap<Long, Object>> envelopes() {
        return envelopes;
    }

    public void envelope(
        final String mdb,
        final long operationId,
        final String text)
    {
        TreeMap<Long, Object> envelopes = this.envelopes.get(mdb);
        if (envelopes == null) {
            envelopes = new TreeMap<>();
            this.envelopes.put(mdb, envelopes);
        }
        Map<String, Object> envelope = new HashMap<>();
        envelope.put(OracleFields.OPERATION_ID, Long.toString(operationId));
        envelope.put("text", text.trim());
        envelope.put(OracleFields.UNAME, Long.toString(operationId));
        envelope.put(
            OracleFields.ACTION_TYPE,
            Collections.singletonList("add"));
        envelope.put(
            OracleFields.OPERATION_DATE,
            System.currentTimeMillis() / MILLIS);
        envelopes.put(operationId, envelope);
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        Logger logger = (Logger) context.getAttribute(Server.LOGGER);
        CgiParams params = new CgiParams(request);
        String mdb = params.get("mdb", NonEmptyValidator.INSTANCE);
        logger.info("mdb: " + mdb);
        long operationId =
            params.get("op-id", NonNegativeLongValidator.INSTANCE);
        logger.info("operationId: " + operationId);
        long length = params.get(
            "length",
            DEFAULT_LENGTH,
            PositiveLongValidator.INSTANCE);
        logger.info("length: " + length);
        TreeMap<Long, Object> envelopes = this.envelopes.get(mdb);
        Assert.assertNotNull(envelopes);
        logger.info("envelopes[mdb].size(): " + envelopes.size());
        Iterator<Object> it =
            envelopes.tailMap(operationId, true).values().iterator();
        StringBuilder sb = new StringBuilder();
        StringBuilderWriter sbw = new StringBuilderWriter(sb);
        try (JsonWriter writer = new JsonWriter(sbw)) {
            writer.startObject();
            writer.key("rows");
            writer.startArray();
            for (int i = 0; i < length && it.hasNext(); ++i) {
                writer.value(it.next());
            }
            writer.endArray();
            writer.endObject();
        }
        StringEntity entity = new StringEntity(new String(sb));
        entity.setChunked(true);
        entity.setContentType(
            ContentType.TEXT_PLAIN.withCharset(
                CharsetUtils.acceptedCharset(request)).toString());
        response.setEntity(entity);
        response.setStatusCode(HttpStatus.SC_OK);
    }
}

