package ru.yandex.blackbox;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.nio.client.SharedConnectingIOReactor;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.writer.JsonType;
import ru.yandex.test.util.TestBase;

public class BlackboxSessionidTest extends TestBase {
    private static final String GOOD = "good.json";
    private static final String IP = "37.9.100.133";
    private static final String HOST = "mail-search-tools.n.yandex-team.ru";
    private static final String SESSION_ID =
        "3:1402919845.5.0.1402919845000:_6bJtA:7e.0"
            + "|4000013288.0.2|63386.792535.KS5oomcKiZpsg057LIugJEGy8yA";
    private static final String URI =
        "/blackbox/?format=json&method=sessionid&userip=" + IP
            + "&sessionid=" + SESSION_ID + "&host=" + HOST;

    @Test
    public void testCheckSession() throws Exception {
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
            BlackboxSessionUserinfo session =
                client.sessionid(
                    new BlackboxSessionidRequest(SESSION_ID, HOST).ip(IP))
                    .get();
            Assert.assertEquals(uid, session.uid());

            server.add(
                URI,
                "{\"status\": {\"value\": \"INVALID\", \"id\": 5}, "
                    + "\"error\": \"signature has bad format or is broken\"}");
            try {
                client.sessionid(
                    new BlackboxSessionidRequest(SESSION_ID, HOST).ip(IP))
                    .get();
                Assert.fail();
            } catch (ExecutionException ee) {
                Assert.assertTrue(
                    ee.getCause() instanceof BlackboxSessionException);
                Assert.assertTrue(
                    ((BlackboxSessionException) ee.getCause()).invalid());
            }

            server.add(
                URI,
                "{\"status\": {\"value\": \"EXPIRED\", \"id\": 2}, "
                    + "\"error\": \"OK\"}");
            try {
                client.sessionid(
                    new BlackboxSessionidRequest(SESSION_ID, HOST).ip(IP))
                    .get();
                Assert.fail();
            } catch (ExecutionException ee) {
                Assert.assertTrue(
                    ee.getCause() instanceof BlackboxSessionException);
                Assert.assertTrue(
                    ((BlackboxSessionException) ee.getCause()).expired());
                Assert.assertFalse(
                    ((BlackboxSessionException) ee.getCause()).invalid());
                Assert.assertEquals(
                    "OK",
                    ((BlackboxSessionException) ee.getCause()).errorMessage());
                Assert.assertEquals(
                    "EXPIRED",
                    ((BlackboxSessionException) ee.getCause()).status());
                Assert.assertEquals(
                    2L,
                    ((BlackboxSessionException) ee.getCause()).errorId());
            }
        }
    }

    @Test
    public void testUserTicket() throws Exception {
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
            final String userTicketUri = URI + "&get_user_ticket=true";
            server.add(
                userTicketUri,
                new File(
                    BlackboxOAuthTest.class.getResource(GOOD).toURI()));
            BlackboxSessionUserinfo session =
                client.sessionid(
                    new BlackboxSessionidRequest(SESSION_ID, HOST, true).ip(IP))
                    .get();
            Assert.assertEquals(uid, session.uid());
            Assert.assertEquals(
                "3:user:CNYJEP...y4Vdt3ztnNx8",
                session.userTicket());

            JsonObject responseJson =
                TypesafeValueContentHandler.parse(
                    new String(
                        Files.readAllBytes(
                        new File(
                            BlackboxOAuthTest.class.getResource(GOOD)
                                .toURI()).toPath()),
                        StandardCharsets.UTF_8));
            responseJson = responseJson.asMap().remove("user_ticket");
            server.add(
                userTicketUri,
                JsonType.HUMAN_READABLE.toString(responseJson));
            try {
                client.sessionid(
                    new BlackboxSessionidRequest(
                        SESSION_ID,
                        HOST,
                        true).ip(IP))
                    .get();
                Assert.fail();
            } catch (ExecutionException ee) {
                Assert.assertTrue(ee.getCause() instanceof BlackboxException);
            }
        }
    }
}
