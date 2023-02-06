package ru.yandex.search.salo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.Assert;

import ru.yandex.collection.Pattern;
import ru.yandex.http.server.sync.BaseHttpServer;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.http.util.request.RequestHandlerMapper;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;
import ru.yandex.parser.string.NonEmptyValidator;
import ru.yandex.parser.uri.CgiParams;

public class TestZoolooser
    extends BaseHttpServer<ImmutableBaseServerConfig>
    implements HttpRequestHandler
{
    private static final String METHOD_LOCK = "/_producer_lock";
    private static final String METHOD_GET = "/_producer_position";
    private static final String METHOD_CAS = "/notify";
    private static final String PRODUCER_NAME = "producer-name";
    private static final long MINUTE = 60000L;

    private final Map<String, Long> initOperationId = new HashMap<>();
    private final ConcurrentHashMap<String, TestLock> mdbs =
        new ConcurrentHashMap<>();

    public TestZoolooser(final ImmutableBaseServerConfig config)
        throws IOException
    {
        super(config);
        register(
            new Pattern<>(METHOD_LOCK, false),
            this,
            RequestHandlerMapper.GET);
        register(
            new Pattern<>(METHOD_GET, false),
            this,
            RequestHandlerMapper.GET);
        register(
            new Pattern<>(METHOD_CAS, false),
            this,
            RequestHandlerMapper.POST);
    }

    public void operationId(final String mdb, final long operationId) {
        initOperationId.put(mdb, operationId);
    }

    void assertEquals(final TestMsal msal) {
        Map<String, TreeMap<Long, Object>> envelopes = msal.envelopes();
        Assert.assertEquals(envelopes.keySet(), mdbs.keySet());
        for (Map.Entry<String, TreeMap<Long, Object>> mdb
                : envelopes.entrySet())
        {
            Assert.assertEquals(
                new ArrayList<>(mdb.getValue().values()),
                new ArrayList<>(mdbs.get(mdb.getKey()).envelopes()));
        }
    }

    private TestLock getStorage(final String mdb) {
        Long initOperationId = this.initOperationId.get(mdb);
        if (initOperationId == null) {
            initOperationId = -1L;
        }
        TestLock lock = new TestLock(mdb, initOperationId);
        TestLock old = mdbs.putIfAbsent(mdb, lock);
        if (old != null) {
            lock = old;
        }
        return lock;
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
        String uri = request.getRequestLine().getUri();
        int end = uri.indexOf('?');
        if (end == -1) {
            end = uri.length();
        }
        uri = uri.substring(0, end);
        if (params.getBoolean("optional", false)) {
            response.setStatusCode(HttpStatus.SC_OK);
        } else if (uri.equals(METHOD_LOCK)) {
            String mdb = params.get(
                PRODUCER_NAME,
                NonEmptyValidator.INSTANCE);
            TestLock storage = getStorage(mdb);
            String token = storage.tryGetLock(MINUTE);
            if (token == null) {
                response.setStatusCode(HttpStatus.SC_FORBIDDEN);
            } else {
                response.setEntity(new StringEntity(token));
                response.setStatusCode(HttpStatus.SC_OK);
            }
        } else if (uri.equals(METHOD_GET)) {
            String mdb = params.get(
                PRODUCER_NAME,
                NonEmptyValidator.INSTANCE);
            TestLock storage = getStorage(mdb.substring(0, mdb.indexOf(':')));
            long operationId;
            if (mdb.charAt(mdb.indexOf(':') + 1) == '0') {
                operationId = storage.operationId();
            } else {
                operationId = -1L;
            }
            response.setEntity(new StringEntity(Long.toString(operationId)));
            response.setStatusCode(HttpStatus.SC_OK);
        } else if (uri.equals(METHOD_CAS)) {
            String mdb = request.getFirstHeader(PRODUCER_NAME).getValue();
            TestLock storage = getStorage(mdb.substring(0, mdb.indexOf(':')));
            long update = Long.parseLong(
                request.getFirstHeader(YandexHeaders.PRODUCER_POSITION)
                    .getValue());
            String token = request.getFirstHeader("lockid").getValue();
            if (storage.compareAndSet(
                    update,
                    token,
                    CharsetUtils.content(
                        ((HttpEntityEnclosingRequest) request).getEntity()),
                    logger))
            {
                logger.info("token accepted");
                response.setStatusCode(HttpStatus.SC_OK);
            } else {
                response.setStatusCode(HttpStatus.SC_FORBIDDEN);
            }
        } else {
            response.setStatusCode(HttpStatus.SC_NOT_FOUND);
        }
    }
}

