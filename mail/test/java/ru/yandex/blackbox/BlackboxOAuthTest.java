package ru.yandex.blackbox;

import java.io.File;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.nio.client.SharedConnectingIOReactor;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class BlackboxOAuthTest extends TestBase {
    private static final String GOOD = "good.json";
    private static final String IP = "37.9.100.133";
    private static final String OAUTH_TOKEN =
        "AQAAAAAAVW2JAAEyK7awWn-2JkwNlKHnrg9ITaI";
    private static final String SCOPES = "tv:use";
    private static final String URI =
        "/blackbox/?format=json&method=oauth&userip=" + IP
        + "&oauth_token=" + OAUTH_TOKEN + "&scopes=" + SCOPES;

    @Test
    public void test() throws Exception {
        try (StaticServer server = new StaticServer(Configs.baseConfig());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            BlackboxClient client =
                new BlackboxClient(reactor, Configs.hostConfig(server)))
        {
            server.start();
            reactor.start();
            client.start();
            final long uid = 5598601L;
            server.add(
                URI,
                new File(
                    BlackboxOAuthTest.class.getResource(GOOD).toURI()));
            BlackboxUserinfo userinfo =
                client.oauth(
                    new BlackboxOAuthRequest(OAUTH_TOKEN, SCOPES).ip(IP))
                    .get();
            Assert.assertEquals(uid, userinfo.uid());
        }
    }

    @Test
    public void testCgiParams() throws Exception {
        try (StaticServer server = new StaticServer(Configs.baseConfig());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            BlackboxClient client =
                new BlackboxClient(reactor, Configs.hostConfig(server)))
        {
            server.start();
            reactor.start();
            client.start();
            final long uid = 5598601L;
            server.add(
                URI.replace(OAUTH_TOKEN, OAUTH_TOKEN + "+Hello%21"),
                new File(
                    BlackboxOAuthTest.class.getResource(GOOD).toURI()));
            BlackboxUserinfo userinfo =
                client.oauth(
                    new BlackboxOAuthRequest(OAUTH_TOKEN + " Hello!", SCOPES)
                        .ip(IP))
                    .get();
            Assert.assertEquals(uid, userinfo.uid());
        }
    }

    @Test
    public void testNoUserIp() throws Exception {
        try (StaticServer server = new StaticServer(Configs.baseConfig());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            BlackboxClient client =
                new BlackboxClient(reactor, Configs.hostConfig(server)))
        {
            server.start();
            reactor.start();
            client.start();
            server.add(
                URI.replace(IP, ""),
                new File(
                    BlackboxOAuthTest.class.getResource("no-userip.json")
                        .toURI()));
            try {
                client.oauth(
                    new BlackboxOAuthRequest(OAUTH_TOKEN, SCOPES).ip(""))
                    .get();
                Assert.fail();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                YandexAssert.assertInstanceOf(
                    BlackboxErrorException.class,
                    cause);
                Assert.assertEquals(
                    "BlackBox error: Missing userip argument",
                    ((BlackboxErrorException) cause).message());
            }
        }
    }

    @Test
    public void testBadScopes() throws Exception {
        try (StaticServer server = new StaticServer(Configs.baseConfig());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            BlackboxClient client =
                new BlackboxClient(reactor, Configs.hostConfig(server)))
        {
            server.start();
            reactor.start();
            client.start();
            String scopes = "tv:uses";
            server.add(
                URI.replace(SCOPES, scopes),
                new File(
                    BlackboxOAuthTest.class.getResource("bad-scopes.json")
                        .toURI()));
            try {
                client.oauth(
                    new BlackboxOAuthRequest(OAUTH_TOKEN, scopes).ip(IP))
                    .get();
                Assert.fail();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                YandexAssert.assertInstanceOf(
                    BlackboxOAuthException.class,
                    cause);
                Assert.assertEquals(
                    "INVALID(5): scope check failed: some scopes are not"
                    + " present in the granted scopes list: " + scopes,
                    cause.getMessage());
            }
        }
    }
}

