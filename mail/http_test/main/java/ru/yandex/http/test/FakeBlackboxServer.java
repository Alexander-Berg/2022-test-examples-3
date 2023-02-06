package ru.yandex.http.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericSupplier;
import ru.yandex.http.util.BadRequestException;
import ru.yandex.http.util.request.RequestPatternParser;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;
import ru.yandex.io.StringBuilderWriter;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.writer.JsonType;
import ru.yandex.json.writer.JsonWriter;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.string.CollectionParser;
import ru.yandex.parser.uri.CgiParams;
import ru.yandex.test.util.TestBase;

public class FakeBlackboxServer
    extends StaticServer
    implements HttpRequestHandler
{
    private static final String NOT_FOUND_TEMPLATE =
        "{\"id\":\"_uid_\",\"uid\":{},\"karma\":{\"value\":0},"
        + "\"karma_status\":{\"value\":0}}";

    private final Map<Long, JsonObject> userinfos = new ConcurrentHashMap<>();

    public FakeBlackboxServer(final ImmutableBaseServerConfig config)
        throws IOException
    {
        super(config);
    }

    public static FakeBlackboxServer fromContext(
        final TestBase testBase,
        final GenericAutoCloseableChain<IOException> chain)
        throws IOException
    {
        return testBase.contextResource(
            FakeBlackboxServer.class.getName(),
            new FakeBlackboxServerSupplier(chain, false));
    }

    public static FakeBlackboxServer corpFromContext(
        final TestBase testBase,
        final GenericAutoCloseableChain<IOException> chain)
        throws IOException
    {
        return testBase.contextResource(
            FakeBlackboxServer.class.getName() + "-corp",
            new FakeBlackboxServerSupplier(chain, true));
    }

    public void addUserinfo(final long uid, final String userinfo) {
        try {
            addUserinfo(uid, TypesafeValueContentHandler.parse(userinfo));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addUserinfo(final long uid, final JsonObject userinfo) {
        userinfos.put(uid, userinfo);
    }

    public void removeUserinfo(final long uid) {
        userinfos.remove(uid);
    }

    @Override
    public void handle(
        final HttpRequest request,
        final HttpResponse response,
        final HttpContext context)
        throws HttpException, IOException
    {
        CgiParams params = new CgiParams(request);
        List<Long> uids =
            params.get(
                "uid",
                new CollectionParser<>(Long::valueOf, ArrayList::new));
        StringBuilderWriter sbw = new StringBuilderWriter();
        try (JsonWriter writer = JsonType.NORMAL.create(sbw)) {
            writer.startObject();
            writer.key("users");
            writer.startArray();
            for (Long uid: uids) {
                JsonObject userinfo = userinfos.get(uid);
                if (userinfo == null) {
                    userinfo =
                        TypesafeValueContentHandler.parse(
                            NOT_FOUND_TEMPLATE.replace(
                                "_uid_",
                                uid.toString()));
                }
                userinfo.writeValue(writer);
            }
            writer.endArray();
            writer.endObject();
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
        response.setEntity(
            new StringEntity(
                sbw.toString(),
                ContentType.APPLICATION_JSON));
    }

    private static class FakeBlackboxServerSupplier
        implements GenericSupplier<FakeBlackboxServer, IOException>
    {
        private final GenericAutoCloseableChain<IOException> chain;
        private final boolean corp;

        FakeBlackboxServerSupplier(
            final GenericAutoCloseableChain<IOException> chain,
            final boolean corp)
        {
            this.chain = chain;
            this.corp = corp;
        }

        @Override
        public FakeBlackboxServer get() throws IOException {
            String serverName;
            String envVar;
            if (corp) {
                serverName = "CorpBlackbox";
                envVar = "CORP_BLACKBOX_HOST";
            } else {
                serverName = "Blackbox";
                envVar = "BLACKBOX_HOST";
            }
            ImmutableBaseServerConfig config;
            try {
                config = Configs.baseConfig(serverName);
            } catch (ConfigException e) {
                throw new IOException(e);
            }

            FakeBlackboxServer server = new FakeBlackboxServer(config);
            chain.add(server);

            try {
                server.register(
                    RequestPatternParser.INSTANCE.apply(
                        "/blackbox/{arg_method:userinfo}"),
                    server);
            } catch (ConfigException e) {
                throw new IOException(e);
            }
            server.start();
            System.setProperty(envVar, server.host().toString());
            return server;
        }
    }
}

