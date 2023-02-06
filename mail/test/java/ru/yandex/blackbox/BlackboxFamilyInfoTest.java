package ru.yandex.blackbox;

import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.nio.client.SharedConnectingIOReactor;
import ru.yandex.json.dom.BasicContainerFactory;
import ru.yandex.json.writer.JsonType;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class BlackboxFamilyInfoTest extends TestBase {
    private static final String PREFIX =
        "/blackbox/?format=json&method=family_info&family_id=";

    @Test
    public void testNotFound() throws Exception {
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
            String id = "f100500";
            server.add(
                PREFIX + id,
                new StaticHttpItem("{\"family\":{\"f100500\":{}}}"));
            try {
                client.familyInfo(new BlackboxFamilyInfoRequest(id)).get();
                Assert.fail();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                YandexAssert.assertInstanceOf(
                    BlackboxNotFoundException.class,
                    cause);
                Assert.assertEquals(
                    id,
                    ((BlackboxNotFoundException) cause).id());
            }
        }
    }

    @Test
    public void testComplete() throws Exception {
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
            String id = "f9000";
            server.add(
                PREFIX + id,
                new StaticHttpItem(
                    "{\"family\":{\"f9000\":{"
                    + "\"admin_uid\":\"5598601\",\"users\":"
                    + "[{\"uid\":\"5598601\"},{\"uid\":\"577338048\"}]}}}"));
            BlackboxFamilyInfo familyInfo =
                client.familyInfo(new BlackboxFamilyInfoRequest(id)).get();
            Assert.assertEquals(id, familyInfo.id());
            Assert.assertEquals(5598601L, familyInfo.adminUid());
            Assert.assertEquals(577338048L, familyInfo.members().get(1).uid());
            Assert.assertEquals(
                "(family_id=f9000,admin_uid=5598601,users=["
                + "(uid=5598601),(uid=577338048)])",
                familyInfo.toString());
            String expectedJson =
                "{\"family_id\":\"f9000\",\"admin_uid\":5598601,\"users\":["
                + "{\"uid\":5598601},{\"uid\":577338048}]}";
            YandexAssert.check(
                new JsonChecker(expectedJson),
                JsonType.NORMAL.toString(familyInfo));
            YandexAssert.check(
                new JsonChecker(expectedJson),
                JsonType.NORMAL.toString(
                    familyInfo.toJsonObject(BasicContainerFactory.INSTANCE)));
        }
    }
}

