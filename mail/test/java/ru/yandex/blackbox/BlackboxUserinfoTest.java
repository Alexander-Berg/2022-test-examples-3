package ru.yandex.blackbox;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.http.entity.ContentType;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.nio.client.SharedConnectingIOReactor;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class BlackboxUserinfoTest extends TestBase {
    private static final String PREFIX =
        "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&";

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
            final long uid = 123123123123L;
            server.add(
                PREFIX + "sid=2&uid=123123123123",
                new StaticHttpItem(
                    "{\"users\":[{\"id\":\"123123123123\",\"uid\":{},"
                    + "\"karma\":{\"value\":0},"
                    + "\"karma_status\":{\"value\":0}}]}"));
            try {
                client.userinfo(new BlackboxUserinfoRequest(uid)).get();
                Assert.fail();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                YandexAssert.assertInstanceOf(
                    BlackboxNotFoundException.class,
                    cause);
                Assert.assertEquals(
                    Long.toString(uid),
                    ((BlackboxNotFoundException) cause).id());
            }
        }
    }

    @Test
    public void testEmptyList() throws Exception {
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
            final long uid = 123123123124L;
            String googleDns = "8.8.8.8";
            server.add(
                PREFIX.replace("127.0.0.1", googleDns)
                + "sid=1&uid=123123123124",
                new StaticHttpItem("{\"users\":[]}"));
            try {
                client.userinfo(
                    new BlackboxUserinfoRequest(uid)
                        .ip(googleDns)
                        .sid("1"))
                    .get();
                Assert.fail();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                YandexAssert.assertInstanceOf(
                    BlackboxMalformedResponseException.class,
                    cause);
            }
        }
    }

    @Test
    public void testMismatchUsersCount() throws Exception {
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
            final long uid = 123123123125L;
            server.add(
                PREFIX + "sid=2&uid=123123123125",
                new StaticHttpItem(
                    "{\"users\":[{\"id\":\"214384396\",\"uid\":{\"value\":"
                    + "\"214384396\",\"lite\":false,\"hosted\":false},"
                    + "\"login\":\"havr.test28\",\"karma\":{\"value\":0},"
                    + "\"karma_status\":{\"value\":0}},{\"id\":\"5598601\","
                    + "\"uid\":{\"value\":\"5598601\",\"lite\":false,"
                    + "\"hosted\":false},\"login\":\"Analizer\",\"karma\":{"
                    + "\"value\":0},\"karma_status\":{\"value\":6000}}]}"));
            try {
                client.userinfo(new BlackboxUserinfoRequest(uid)).get();
                Assert.fail();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                YandexAssert.assertInstanceOf(
                    BlackboxMalformedResponseException.class,
                    cause);
            }
        }
    }

    @Test
    public void testInvalidUid() throws Exception {
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
                PREFIX + "sid=2&uid=abc",
                new StaticHttpItem(
                    "{\"exception\":{\"value\":\"INVALID_PARAMS\",\"id\":2},"
                    + "\"error\":\"BlackBox error: invalid uid value\"}"));
            try {
                client.userinfo(
                    new BlackboxUserinfoRequest(BlackboxUserIdType.UID, "abc"))
                    .get();
                Assert.fail();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                YandexAssert.assertInstanceOf(
                    BlackboxErrorException.class,
                    cause);
                BlackboxErrorException ex = (BlackboxErrorException) cause;
                Assert.assertEquals(
                    BlackboxErrorException.ERROR_INVALID_PARAMS,
                    ex.errorId());
                Assert.assertEquals("INVALID_PARAMS", ex.errorValue());
                Assert.assertEquals(
                    "BlackBox error: invalid uid value",
                    ex.message());
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
            server.add(
                PREFIX
                + "dbfields=hosts.db_id.2,subscription.suid.2&emails=getall"
                + "&get_family_info&sid=2&uid=5598601,203889311",
                new File(
                    BlackboxUserinfoTest.class.getResource("complete.json")
                        .toURI()),
                    ContentType.APPLICATION_JSON);
            final long hirthworkUid = 203889311L;
            final long analizerUid = 5598601L;
            BlackboxUserinfos userinfos =
                client.userinfo(
                    new BlackboxUserinfoRequest(analizerUid, hirthworkUid)
                        .dbfields(BlackboxDbfield.SUID, BlackboxDbfield.MDB)
                        .emailsType(BlackboxEmailsType.GETALL)
                        .getFamilyInfo(true))
                    .get();
            String common = "(born-date=2013-03-14 20:58:02,native,validated)";
            Assert.assertEquals(
                "[hirthwork(uid=203889311,"
                + "[hirthwork@narod.ru" + common
                + ",hirthwork@ya.ru" + common
                + ",hirthwork@yandex.by" + common
                + ",hirthwork@yandex.com" + common
                + ",hirthwork@yandex.kz" + common
                + ",hirthwork@yandex.ru(born-date=2013-03-14 20:58:02,default,"
                + "native,validated)"
                + ",hirthwork@fake1.yandex.com(born-date=1970-01-01 03:00:00,"
                + "rpop,unsafe,validated)],"
                + "{hosts.db_id.2=mdb220,subscription.suid.2=632123143}), "
                + "Analizer(uid=5598601,karma=100,karma-status=6000,"
                + "[Analizer@yandex.com(born-date=2003-09-04 21:34:25,default,"
                + "native,validated),79267227664@yandex.com("
                + "born-date=2003-09-04 21:34:25,native,validated),"
                + "Analizer@fake1.yandex.com(born-date=2012-10-24 00:40:04,"
                + "validated)],"
                + "{hosts.db_id.2=mdb300,subscription.suid.2=12054080},"
                + "family_info={family_id=f12984,admin_uid=5598601})]",
                userinfos.toString());
            Assert.assertEquals(2, userinfos.size());
            BlackboxUserinfo userinfo = userinfos.get(0);
            Assert.assertEquals("hirthwork", userinfo.login());
            Assert.assertEquals(0, userinfo.karma());
            Assert.assertEquals(0, userinfo.karmaStatus());
            Assert.assertEquals(hirthworkUid, userinfo.uid());
            Assert.assertFalse(userinfo.hosted());
            Assert.assertFalse(userinfo.lite());
            Map<BlackboxDbfield, String> dbfields = new HashMap<>();
            dbfields.put(BlackboxDbfield.MDB, "mdb220");
            dbfields.put(BlackboxDbfield.SUID, "632123143");
            Assert.assertEquals(dbfields, userinfo.dbfields());
            String[] emails = new String[] {
                "hirthwork@narod.ru",
                "hirthwork@ya.ru",
                "hirthwork@yandex.by",
                "hirthwork@yandex.com",
                "hirthwork@yandex.kz",
                "hirthwork@yandex.ru"
            };
            List<BlackboxAddress> addressList = userinfo.addressList();
            Assert.assertEquals(emails.length + 1, addressList.size());
            final long hirthworkBornDate = 1363280282000L;
            for (int i = 0; i < emails.length; ++i) {
                BlackboxAddress address = addressList.get(i);
                Assert.assertEquals(emails[i], address.email());
                Assert.assertEquals(hirthworkBornDate, address.bornDate());
                Assert.assertTrue(address.nativeFlag());
                Assert.assertTrue(address.validatedFlag());
                if (i == emails.length - 1) {
                    Assert.assertTrue(address.defaultFlag());
                } else {
                    Assert.assertFalse(address.defaultFlag());
                }
                Assert.assertFalse(address.rpopFlag());
                Assert.assertFalse(address.unsafeFlag());
            }
            BlackboxAddress address = addressList.get(emails.length);
            Assert.assertEquals(
                "hirthwork@fake1.yandex.com",
                address.email());
            Assert.assertEquals(0L, address.bornDate());
            Assert.assertTrue(address.rpopFlag());
            Assert.assertTrue(address.unsafeFlag());
            Assert.assertTrue(address.validatedFlag());
            Assert.assertFalse(address.defaultFlag());
            Assert.assertFalse(address.nativeFlag());

            userinfo = userinfos.get(1);
            Assert.assertEquals("Analizer", userinfo.login());
            Assert.assertEquals(100, userinfo.karma());
            Assert.assertEquals(6000, userinfo.karmaStatus());
            Assert.assertEquals(analizerUid, userinfo.uid());
            Assert.assertFalse(userinfo.hosted());
            Assert.assertFalse(userinfo.lite());
            dbfields = new HashMap<>();
            dbfields.put(BlackboxDbfield.MDB, "mdb300");
            dbfields.put(BlackboxDbfield.SUID, "12054080");
            Assert.assertEquals(dbfields, userinfo.dbfields());
            addressList = userinfo.addressList();
            Assert.assertEquals(2 + 1, addressList.size());
        }
    }

    @Test
    public void testName() throws Exception {
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
                PREFIX
                + "dbfields=userinfo.firstname.uid&sid=2&login=dpotapovagg",
                new File(
                    BlackboxUserinfoTest.class.getResource("name.json")
                        .toURI()),
                    ContentType.APPLICATION_JSON);
            String login = "dpotapovagg";
            BlackboxUserinfos userinfos =
                client.userinfo(
                    new BlackboxUserinfoRequest(
                        BlackboxUserIdType.LOGIN,
                        login)
                        .requiredDbfields(BlackboxDbfield.FIRSTNAME))
                    .get();
            Assert.assertEquals(
                "[dpotapovagg(uid=313730867,{userinfo.firstname.uid=Vasily})]",
                userinfos.toString());
            Assert.assertEquals(1, userinfos.size());
            BlackboxUserinfo userinfo = userinfos.get(0);
            Assert.assertEquals(login, userinfo.login());
            Assert.assertEquals(0, userinfo.karma());
            Assert.assertEquals(0, userinfo.karmaStatus());
            final long uid = 313730867L;
            Assert.assertEquals(uid, userinfo.uid());
            Assert.assertFalse(userinfo.hosted());
            Assert.assertFalse(userinfo.lite());
            Map<BlackboxDbfield, String> dbfields = new HashMap<>();
            dbfields.put(BlackboxDbfield.FIRSTNAME, "Vasily");
            Assert.assertEquals(dbfields, userinfo.dbfields());
        }
    }

    @Test
    public void requiredFields() throws Exception {
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
                PREFIX + "dbfields=hosts.db_id.2,subscription.suid.2"
                + ",userinfo.reg_date.uid&sid=2&uid=317571101",
                new File(
                    BlackboxUserinfoTest.class.getResource("pg.json")
                        .toURI()),
                    ContentType.APPLICATION_JSON);
            final long uid = 317571101L;
            try {
                client.userinfo(
                    new BlackboxUserinfoRequest(uid)
                        .dbfields(BlackboxDbfield.MDB)
                        .requiredDbfields(
                            BlackboxDbfield.REG_DATE,
                            BlackboxDbfield.SUID))
                    .get();
                Assert.fail();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                YandexAssert.assertInstanceOf(
                    BlackboxMalformedResponseException.class,
                    cause);
            }
        }
    }
}

