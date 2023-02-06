package ru.yandex.http.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import ru.yandex.collection.Pattern;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericSupplier;
import ru.yandex.http.util.BadRequestException;
import ru.yandex.http.util.ByteArrayEntityFactory;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.NotFoundException;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.io.StringBuilderWriter;
import ru.yandex.json.writer.JsonType;
import ru.yandex.json.writer.JsonWriter;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.string.CollectionParser;
import ru.yandex.parser.uri.CgiParams;
import ru.yandex.parser.uri.QueryParser;
import ru.yandex.passport.tvmauth.Version;
import ru.yandex.test.util.TestBase;

public class FakeTvmServer
    extends StaticServer
    implements HttpRequestHandler
{
    private final Map<Integer, String> tickets = new ConcurrentHashMap<>();

    public FakeTvmServer(final ImmutableBaseServerConfig config)
        throws IOException
    {
        super(config);
    }

    public static FakeTvmServer fromContext(
        final TestBase testBase,
        final GenericAutoCloseableChain<IOException> chain)
        throws IOException
    {
        return testBase.contextResource(
            FakeTvmServer.class.getName(),
            new FakeTvmServerSupplier(chain));
    }

    public void addTicket(final String tvmId, final String ticket) {
        addTicket(Integer.parseInt(tvmId), ticket);
    }

    public void addTicket(final int tvmId, final String ticket) {
        tickets.put(tvmId, ticket);
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        if (!(request instanceof HttpEntityEnclosingRequest)) {
            throw new BadRequestException("Payload expected");
        }
        CgiParams params =
            new CgiParams(
                new QueryParser(
                    CharsetUtils.toString(
                        ((HttpEntityEnclosingRequest) request).getEntity())));
        List<Integer> dsts =
            params.get(
                "dst",
                new CollectionParser<>(Integer::valueOf, ArrayList::new));
        StringBuilderWriter sbw = new StringBuilderWriter();
        try (JsonWriter writer = JsonType.NORMAL.create(sbw)) {
            writer.startObject();
            for (Integer dst: dsts) {
                String ticket = tickets.get(dst);
                if (ticket == null) {
                    throw new NotFoundException(
                        "No ticket found for dst: " + dst);
                }
                writer.key(dst.toString());
                writer.startObject();
                writer.key("ticket");
                writer.value(ticket);
                writer.endObject();
            }
            writer.endObject();
        }
        response.setEntity(
            new StringEntity(
                sbw.toString(),
                ContentType.APPLICATION_JSON));
    }

    private static class FakeTvmServerSupplier
        implements GenericSupplier<FakeTvmServer, IOException>
    {
        private final GenericAutoCloseableChain<IOException> chain;

        FakeTvmServerSupplier(
            final GenericAutoCloseableChain<IOException> chain)
        {
            this.chain = chain;
        }

        @Override
        public FakeTvmServer get() throws IOException {
            ImmutableBaseServerConfig config;
            try {
                config = Configs.baseConfig("TVM2");
            } catch (ConfigException e) {
                throw new IOException(e);
            }

            FakeTvmServer server = new FakeTvmServer(config);
            chain.add(server);

            server.add(
                "/2/keys/?lib_version=" + Version.get(),
                new StaticHttpResource(
                    new StaticHttpItem(
                        HttpStatus.SC_OK,
                        IOStreamUtils.consume(
                            getClass().getResourceAsStream("tvm-keys.txt"))
                            .processWith(ByteArrayEntityFactory.INSTANCE))));
            server.register(new Pattern<>("/2/ticket/", false), server);
            server.start();
            System.setProperty("TVM_API_HOST", server.host().toString());
            return server;
        }
    }
}

