package ru.yandex.search.sharpei;

import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.http.util.nio.client.SharedConnectingIOReactor;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class ConnInfoTest extends TestBase {
    private static final String CONNINFO = "/conninfo?format=json&uid=";
    private static final String READ_WRITE = "&mode=read_write";

    @Test
    public void test() throws Exception {
        try (StaticServer server = new StaticServer(Configs.baseConfig());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            SharpeiClient client =
                new SharpeiClient(reactor, Configs.hostConfig(server)))
        {
            final long uid = 123123123L;
            String uri = CONNINFO + uid + "&mode=write_only";
            server.add(
                uri,
                "{\"id\":\"7\",\"databases\":[{\"address\":{\"host\":"
                + "\"xdb01e.mail.yandex.net\", \"port\":\"6432\",\"dbname\":"
                + "\"maildb\",\"dataCenter\":\"IVA\"},\"role\":\"master\"}]}");
            server.start();
            reactor.start();
            client.start();
            ConnInfo connInfo = client.connInfo(
                uid,
                SharpeiMode.WRITE_ONLY,
                EmptyFutureCallback.INSTANCE).get();
            Assert.assertEquals(
                "jdbc:postgresql://xdb01e.mail.yandex.net:6432/maildb",
                connInfo.pgUrl());
            Assert.assertEquals("7", connInfo.shardId());
            Assert.assertEquals("IVA", connInfo.dc());
        }
    }

    @Test
    public void testMultiline() throws Exception {
        try (StaticServer server = new StaticServer(Configs.baseConfig());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            SharpeiClient client =
                new SharpeiClient(reactor, Configs.hostConfig(server)))
        {
            final long uid = 123123123123L;
            String uri = CONNINFO + uid + READ_WRITE;
            server.add(
                uri,
                "{\"id\":\"8\",\"databases\":[{\"address\":{\"host\":"
                + "\"xdb01f.mail.yandex.net\",\"port\":\"6432\",\"dbname\":"
                + "\"maildb\",\"dataCenter\":\"MYT\"},\"role\":\"replica\"},"
                + "{\"address\":{\"host\":\"xdb01g.mail.yandex.net\",\"port\":"
                + "\"6432\",\"dbname\":\"maildb\",\"dataCenter\":\"FOL\"},"
                + "\"role\":\"replica\"},{\"address\":{\"host\":"
                + "\"xdb01e.mail.yandex.net\",\"port\":\"6432\",\"dbname\":"
                + "\"maildb\",\"dataCenter\":\"IV\"},\"role\":\"master\"}]}");
            server.start();
            reactor.start();
            client.start();
            ConnInfo connInfo = client.connInfo(
                uid,
                SharpeiMode.READ_WRITE,
                EmptyFutureCallback.INSTANCE).get();
            Assert.assertEquals(
                "jdbc:postgresql://xdb01f.mail.yandex.net:6432/maildb",
                connInfo.pgUrl());
            Assert.assertEquals("MYT", connInfo.dc());
        }
    }

    @Test
    public void testMissingHost() throws Exception {
        try (StaticServer server = new StaticServer(Configs.baseConfig());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            SharpeiClient client =
                new SharpeiClient(reactor, Configs.hostConfig(server)))
        {
            final long uid = 123123L;
            String uri = CONNINFO + uid + READ_WRITE;
            server.add(
                uri,
                "{\"id\":\"7\",\"databases\":[{\"address\":{\"port\":\"6432\","
                + "\"dbname\":\"maildb\",\"dataCenter\":\"MYT\"},"
                + "\"role\":\"replica\"}]}");
            server.start();
            reactor.start();
            client.start();
            try {
                client.connInfo(
                    uid,
                    SharpeiMode.READ_WRITE,
                    EmptyFutureCallback.INSTANCE).get();
                Assert.fail();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                YandexAssert.assertInstanceOf(SharpeiException.class, cause);
            }
        }
    }

    @Test
    public void testEmptyAddrs() throws Exception {
        try (StaticServer server = new StaticServer(Configs.baseConfig());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            SharpeiClient client =
                new SharpeiClient(reactor, Configs.hostConfig(server)))
        {
            final long uid = 1231234L;
            String uri = CONNINFO + uid + READ_WRITE;
            server.add(uri, "{\"id\":\"1\",\"databases\":[]}");
            server.start();
            reactor.start();
            client.start();
            try {
                client.connInfo(
                    uid,
                    SharpeiMode.READ_WRITE,
                    EmptyFutureCallback.INSTANCE).get();
                Assert.fail();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                YandexAssert.assertInstanceOf(SharpeiException.class, cause);
            }
        }
    }

    @Test
    public void testNoUsable() throws Exception {
        try (StaticServer server = new StaticServer(Configs.baseConfig());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            SharpeiClient client =
                new SharpeiClient(reactor, Configs.hostConfig(server)))
        {
            final long uid = 123125L;
            String uri = CONNINFO + uid + READ_WRITE;
            server.add(
                uri,
                "{\"id\":\"8\",\"databases\":[{\"address\":{\"port\":\"6433\","
                + "\"host\":\"xdb01f.mail.yandex.net\",\"dbname\":\"maildb\","
                + "\"dataCenter\":\"MYT\"},\"role\":\"unknown\"}]}");
            server.start();
            reactor.start();
            client.start();
            try {
                client.connInfo(
                    uid,
                    SharpeiMode.READ_WRITE,
                    EmptyFutureCallback.INSTANCE).get();
                Assert.fail();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                YandexAssert.assertInstanceOf(SharpeiException.class, cause);
            }
        }
    }
}

