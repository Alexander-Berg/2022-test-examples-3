package ru.yandex.search.sharpei;

import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.BadRequestException;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.http.util.IOHttpException;
import ru.yandex.http.util.nio.client.SharedConnectingIOReactor;
import ru.yandex.json.parser.JsonException;
import ru.yandex.json.xpath.JsonUnexpectedTokenException;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class ShardInfoTest extends TestBase {
    private static final String STAT = "/v2/stat?shard_id=";
    private static final String DATA =
        "{\"1\":{\"name\":\"xdb2001\",\"id\":\"1\",\"databases\":"
        + "[{\"address\":{\"host\":\"xdb2001g.mail.yandex.net\",\"port\":"
        + "\"6432\",\"dbname\":\"maildb\",\"dataCenter\":\"FOL\"},\"role\":"
        + "\"master\"},{\"address\":{\"host\":\"xdb2001d.mail.yandex.net\","
        + "\"port\":\"6432\",\"dbname\":\"maildb\",\"dataCenter\":\"UGR\"},"
        + "\"role\":\"replica\"},{\"address\":{\"host\":\"xdb2001e.mail.yandex"
        + ".net\",\"port\":\"6432\",\"dbname\":\"maildb\",\"dataCenter\":\"IVA"
        + "\"},\"role\":\"replica\"},{\"address\":{\"host\":"
        + "\"xdb2002d.mail.yandex.net\",\"port\":\"6432\",\"dbname\":"
        + "\"maildb\",\"dataCenter\":\"UGR\"},\"role\":\"unknown\"},"
        + "{\"address\":{\"host\":\"xdb2002h.mail.yandex.net\",\"port\":"
        + "\"6432\",\"dbname\":\"maildb\",\"dataCenter\":\"UGR\"},"
        + "\"role\":\"master\",\"status\":\"dead\"}]}}";

    private static void checkShardInfo(final ShardInfo shardInfo) {
        Assert.assertEquals(Integer.toString(1), shardInfo.id());
        Assert.assertEquals(
            "jdbc:postgresql://xdb2001g.mail.yandex.net:6432/maildb",
            shardInfo.master().pgUrl());
        Assert.assertEquals("FOL", shardInfo.master().dc());
        Assert.assertEquals(2, shardInfo.replicas().size());
        Assert.assertEquals(
            "jdbc:postgresql://xdb2001d.mail.yandex.net:6432/maildb",
            shardInfo.replicas().get(0).pgUrl());
        Assert.assertEquals("UGR", shardInfo.replicas().get(0).dc());
        Assert.assertEquals(
            "jdbc:postgresql://xdb2001e.mail.yandex.net:6432/maildb",
            shardInfo.replicas().get(1).pgUrl());
        Assert.assertEquals("IVA", shardInfo.replicas().get(1).dc());
    }

    @Test
    public void testShardInfo() throws Exception {
        try (StaticServer server = new StaticServer(Configs.baseConfig());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            SharpeiClient client =
                new SharpeiClient(reactor, Configs.hostConfig(server)))
        {
            server.add(STAT + 1, DATA);
            server.start();
            reactor.start();
            client.start();
            ShardInfo shardInfo = client.shardInfo(
                Integer.toString(1),
                EmptyFutureCallback.INSTANCE).get();
            checkShardInfo(shardInfo);
        }
    }

    @Test
    public void testShardNotFound() throws Exception {
        try (StaticServer server = new StaticServer(Configs.baseConfig());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            SharpeiClient client =
                new SharpeiClient(reactor, Configs.hostConfig(server)))
        {
            server.add(STAT + 2, DATA);
            server.start();
            reactor.start();
            client.start();
            try {
                client.shardInfo(
                    Integer.toString(2),
                    EmptyFutureCallback.INSTANCE).get();
                Assert.fail();
            } catch (ExecutionException e) {
                YandexAssert.assertInstanceOf(
                    SharpeiNotFoundException.class,
                    e.getCause());
            }
        }
    }

    @Test
    public void testUnrecognizedRole() throws Exception {
        try (StaticServer server = new StaticServer(Configs.baseConfig());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            SharpeiClient client =
                new SharpeiClient(reactor, Configs.hostConfig(server)))
        {
            server.add(STAT + 1, DATA.replace("unknown", "abracadabra"));
            server.start();
            reactor.start();
            client.start();
            try {
                client.shardInfo(
                    Integer.toString(1),
                    EmptyFutureCallback.INSTANCE).get();
                Assert.fail();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                YandexAssert.assertInstanceOf(SharpeiException.class, cause);
            }
        }
    }

    @Test
    public void testBadFormat() throws Exception {
        try (StaticServer server = new StaticServer(Configs.baseConfig());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            SharpeiClient client =
                new SharpeiClient(reactor, Configs.hostConfig(server)))
        {
            server.add(STAT + 1, "[]");
            server.start();
            reactor.start();
            client.start();
            try {
                client.shardInfo(
                    Integer.toString(1),
                    EmptyFutureCallback.INSTANCE).get();
                Assert.fail();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                YandexAssert.assertInstanceOf(SharpeiException.class, cause);
                YandexAssert.assertInstanceOf(
                    JsonUnexpectedTokenException.class,
                    cause.getCause());
            }
        }
    }

    @Test
    public void testMalformedJson() throws Exception {
        try (StaticServer server = new StaticServer(Configs.baseConfig());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            SharpeiClient client =
                new SharpeiClient(reactor, Configs.hostConfig(server)))
        {
            server.add(STAT + 1, "]");
            server.start();
            reactor.start();
            client.start();
            try {
                client.shardInfo(
                    Integer.toString(1),
                    EmptyFutureCallback.INSTANCE).get();
                Assert.fail();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                YandexAssert.assertInstanceOf(IOHttpException.class, cause);
                cause = cause.getCause();
                YandexAssert.assertInstanceOf(BadRequestException.class, cause);
                cause = cause.getCause();
                YandexAssert.assertInstanceOf(JsonException.class, cause);
            }
        }
    }
}

