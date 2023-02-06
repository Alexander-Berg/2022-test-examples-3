package ru.yandex.mail.so2.skeleton;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;

import ru.yandex.function.ByteArrayProcessable;
import ru.yandex.http.proxy.AbstractProxySessionCallback;
import ru.yandex.http.proxy.BasicProxySession;
import ru.yandex.http.proxy.ProxySession;
import ru.yandex.http.util.nio.ByteArrayProcessableAsyncConsumer;
import ru.yandex.json.writer.JsonTypeExtractor;
import ru.yandex.mail.so.factors.BasicSoFunctionInputs;
import ru.yandex.mail.so.factors.LoggingFactorsAccessViolationHandler;
import ru.yandex.mail.so.factors.http.HttpSoFactorsExtractorContext;
import ru.yandex.mail.so.factors.types.BooleanSoFactorType;
import ru.yandex.mail.so.factors.types.LongSoFactorType;
import ru.yandex.parser.mail.errors.ErrorsLogger;

public class So2SkeletonHandler
    implements HttpAsyncRequestHandler<ByteArrayProcessable>
{
    private final So2SkeletonServer<?> server;

    public So2SkeletonHandler(final So2SkeletonServer<?> server) {
        this.server = server;
    }

    @Override
    public HttpAsyncRequestConsumer<ByteArrayProcessable> processRequest(
        final HttpRequest request,
        final HttpContext context)
    {
        return new ByteArrayProcessableAsyncConsumer();
    }

    @Override
    public void handle(
        final ByteArrayProcessable body,
        final HttpAsyncExchange exchange,
        final HttpContext context)
        throws HttpException, IOException
    {
        ProxySession session =
            new BasicProxySession(server, exchange, context);
        String extractorName =
            session.params().getString("extractor-name", "main");
        ErrorsLogger errorsLogger = new ErrorsLogger(session.logger());
        long uid = session.params().getLong("uid");
        boolean resolveFamilyMembers =
            session.params().getBoolean("resolve-family-members", false);
        LoggingFactorsAccessViolationHandler accessViolationHandler =
            new LoggingFactorsAccessViolationHandler(
                server.extractors().violationsCounter(),
                session.logger());
        HttpSoFactorsExtractorContext extractorContext =
            new HttpSoFactorsExtractorContext(
                session,
                accessViolationHandler,
                errorsLogger,
                server.threadPool());
        server.extractors().extract(
            extractorName,
            extractorContext,
            new BasicSoFunctionInputs(
                accessViolationHandler,
                LongSoFactorType.LONG.createFactor(uid),
                BooleanSoFactorType.BOOLEAN.createFactor(
                    resolveFamilyMembers)),
            JsonTypeExtractor.NORMAL.extract(session.params()),
            new Callback(session));
    }

    private static class Callback
        extends AbstractProxySessionCallback<String>
    {
        Callback(final ProxySession session)
            throws HttpException
        {
            super(session);
        }

        @Override
        public void completed(final String result) {
            session.response(
                HttpStatus.SC_OK,
                new NStringEntity(
                    result,
                    ContentType.APPLICATION_JSON.withCharset(
                        session.acceptedCharset())));
        }
    }
}

