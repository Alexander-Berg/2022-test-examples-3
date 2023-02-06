package ru.yandex.iex.proxy;

import java.io.File;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.http.HttpException;
import org.apache.http.protocol.HttpCoreContext;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.http.proxy.BasicProxySession;
import ru.yandex.http.proxy.HttpProxy;
import ru.yandex.http.proxy.ProxySession;
import ru.yandex.http.test.MockHttpExchange;
import ru.yandex.http.test.MockServerConnection;
import ru.yandex.iex.proxy.xutils.mailsender.MailSender;
import ru.yandex.iex.proxy.xutils.mailsender.MultiIexForwarder;
import ru.yandex.iex.proxy.xutils.mailsender.SendmailContext;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.xpath.JsonUnexpectedTokenException;
import ru.yandex.logger.PrefixedLogger;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.parser.email.types.MessageType;
import ru.yandex.parser.string.CollectionParser;
import ru.yandex.parser.uri.CgiParams;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.test.util.TestBase;

public class MailForwardTest extends TestBase {
    private static final String CONFIG_PATH =
        Paths.getSourcePath(
            "mail/iex/iex_proxy_config/files/forward/forwards.conf");

    private static final String UID = "12351235";
    private static final String MID = "236436345";
    private static final String USER_EMAIL = "vasya@yandex.ru";
    private static final long RECEIVED_DATE = 1530375520L;

    // CSOFF: ParameterNumber
    // CSOFF: MagicNumber
    // CSOFF: MultipleStringLiterals
    private static final long DAY_START =
        MultiIexForwarder.startOfDay(1530544703000L);

    private static final long GOOD_TIME =
        DAY_START
            + TimeUnit.HOURS.toMillis(16)
            + TimeUnit.MINUTES.toMillis(30);

    private static final long BAD_TIME_LESS =
        DAY_START
            + TimeUnit.HOURS.toMillis(10)
            + TimeUnit.MINUTES.toMillis(30);

   // private static final long BAD_TIME_OVER =
   //     DAY_START
   //         + TimeUnit.HOURS.toMillis(13)
   //         + TimeUnit.MINUTES.toMillis(30);

    private static ImmutableIexForwardersConfig loadConfig() throws Exception {
        IniConfig initConfig =
            new IniConfig(
                new File(CONFIG_PATH));

        IniConfig forwardersConfig = initConfig.section("forwarders");
        Assert.assertNotNull(forwardersConfig);

        ImmutableIexForwardersConfig config =
            new IexForwardersConfigBuilder(forwardersConfig).build();
        initConfig.checkUnusedKeys();
        return config;
    }

    @Test
    public void testTicket() throws Exception {
        try (IexProxyCluster cluster = new IexProxyCluster(this)) {
            ImmutableIexForwardersConfig config = loadConfig();

            MockMailSender mailSender = new MockMailSender();
            MultiIexForwarder forwarder =
                new MultiIexForwarder(mailSender, config.forwarder("ticket"));
            final String yndxTicket = "yndx.ticket@yandex.ru";

            //test reset
            forwarder.accept(
                create(
                    cluster,
                    "new_day_mail@aeroflot.ru",
                    BAD_TIME_LESS));
            Assert.assertEquals(0, mailSender.callCount());
            forwarder.accept(
                create(cluster, "ticket@aeroflot.ru", MessageType.S_NEWS));
            Assert.assertEquals(1, mailSender.callCount());
            Assert.assertEquals(
                1,
                mailSender.callCount(yndxTicket));

            forwarder.accept(
                create(
                    cluster,
                    "enlargeyourticket@aeroflot.ru",
                    MessageType.S_NEWS));
            Assert.assertEquals(1, mailSender.callCount());
            Assert.assertEquals(
                1,
                mailSender.callCount(yndxTicket));
            forwarder.accept(
                create(cluster, "end@s7.ru", MessageType.S_TRAVEL));
            Assert.assertEquals(1, mailSender.callCount());
            Assert.assertEquals(
                1,
                mailSender.callCount(yndxTicket));
            forwarder.accept(
                create(cluster, "end@s7.ru", MessageType.ESHOP));
            Assert.assertEquals(2, mailSender.callCount());
            Assert.assertEquals(
                2,
                mailSender.callCount(yndxTicket));
            //test ticket travel
            final String ticketTravel = "yndx.ticket.travel@yandex.ru";
            forwarder.accept(
                create(cluster, "zozo@ozon.travel", MessageType.S_TRAVEL));
            Assert.assertEquals(3, mailSender.callCount());
            Assert.assertEquals(
                1,
                mailSender.callCount(ticketTravel));
            forwarder.accept(
                create(cluster, "vovo@ozon.travel", MessageType.S_TRAVEL));
            Assert.assertEquals(3, mailSender.callCount());
            Assert.assertEquals(
                1,
                mailSender.callCount(ticketTravel));

            //test reset
            forwarder.accept(
                create(
                    cluster,
                    "new_day_mail@aeroflot.ru",
                    GOOD_TIME + TimeUnit.DAYS.toMillis(1)));
            Assert.assertEquals(
                3,
                mailSender.callCount(yndxTicket));
        }
    }

    @Test
    public void testMultiForwarder() throws Exception {
        try (IexProxyCluster cluster = new IexProxyCluster(this)) {
            final String iniStr =
                "[event-ticket.invite]\n"
                + "limiters=types,total\n"
                + "types.list=invite\n"
                + "total.limit=2\n"
                + "email=yndx.cal@yandex.ru\n"
                + "[event-ticket.other]\n"
                + "limiters=types,total\n"
                + "types.list=s_event,s_training\n"
                + "types.any=true\n"
                + "total.limit=2\n"
                + "email=yndx.event.ticket@yandex.ru\n";

            final IniConfig iniConfig = new IniConfig(new StringReader(iniStr));
            ImmutableIexForwardersConfig config =
                new IexForwardersConfigBuilder(iniConfig).build();

            iniConfig.checkUnusedKeys();

            MockMailSender mailSender = new MockMailSender();
            MultiIexForwarder forwarder =
                new MultiIexForwarder(
                    mailSender,
                    config.forwarder("event-ticket"));

            forwarder.accept(create(cluster, "notype@ticket.ru"));
            forwarder.accept(
                create(
                    cluster,
                    "invite@ticket.ru",
                    MessageType.INVITE,
                    MessageType.NEWS));
            forwarder.accept(
                create(
                    cluster,
                    "invite@avia.ru",
                    MessageType.NOTIFICATION));
            forwarder.accept(
                create(
                    cluster,
                    "training@anywayanyday.ru",
                    MessageType.S_TRAINING,
                    MessageType.PROMO));
            forwarder.accept(
                create(cluster, "event1@events.ru", MessageType.S_EVENT));
            forwarder.accept(
                create(cluster, "overlimit@events.ru", MessageType.S_EVENT));
            forwarder.accept(
                create(cluster, "overlimit@invite.ru", MessageType.INVITE));
            Assert.assertEquals(
                2,
                mailSender.callCount("yndx.cal@yandex.ru"));
            Assert.assertEquals(
                2,
                mailSender.callCount("yndx.event.ticket@yandex.ru"));
            Assert.assertEquals(4, mailSender.callCount());
        }
    }

    @Test
    public void testForwarderConfig() throws Exception {
        try (IexProxyCluster cluster = new IexProxyCluster(this)) {
            CgiParams params =
                new CgiParams("/action?&subject=Ranking+on+the+First+Page+of"
                    + "+Google&email=nareshwebservice@hotmail.com"
                    + "&user_email=zpziy@mail.ru&received_date=1530727245"
                    + "&uid=58231729&suid=159643945&mdb=pg&pgshard=2172"
                    + "&mid=166070236259365166&stid=320.mail:58231729.E1069916:"
                    + "386168186681587050647487553057&firstline=Hi,+I+am+Nares+"
                    + "Marketing+Consultant.+Do+you+want+to+see+your+website+"
                    + "in+Top+10+positions+in+Google+or+other+major+search+"
                    + "engine?+We+have+launched+our+SEO+packages+on+discounted+"
                    + "prices.+Avail+the+benefit+of+our+discounted+packages+"
                    + "to+boost+up+your+business.+"
                    + "I+was+wondering+if+you+would+be+inter"
                    + "ested+in+getting+the+SEO+done+for+your+website.+There+"
                    + "is+a+simple+equation+that+is&types=4,46,51,9999");
            Set<MessageType> expected = new LinkedHashSet<>(
                Arrays.asList(
                    MessageType.PEOPLE,
                    MessageType.FIRSTMAIL,
                    MessageType.TRUST_1,
                    null));
            Assert.assertEquals(
                expected,
                params.get(
                    "types",
                    Collections.emptySet(),
                    new CollectionParser<>(
                        new MessageTypesCodeOrEnumParser(null),
                        LinkedHashSet::new)));

            final String iniStr =
                "[limiters_globals]\n"
                + "total.limit=1\n"
                + "[fines]\n"
                + "email=yndx.fines@yandex.ru\n"
                + "limiters=timerange,peremail,total\n"
                + "total.limit=4\n"
                + "timerange.start-time=00:00.00\n"
                + "timerange.end-time=23:59.59\n"
                + "peremail.a@b\\\\.ru=1\n"
                + "peremail.hello@yandex\\\\.ru=10\n"
                + "[eshop]\n"
                + "email=yndx.ticket@yandex.ru\n"
                + "limiters=total";
            final IniConfig iniConfig = new IniConfig(new StringReader(iniStr));
            ImmutableIexForwardersConfig config =
                new IexForwardersConfigBuilder(iniConfig).build();

            iniConfig.checkUnusedKeys();

            MockMailSender mailSender = new MockMailSender();
            MultiIexForwarder finesForwarder =
                new MultiIexForwarder(mailSender, config.forwarder("fines"));

            final String valid1 = "a@b.ru";
            final String valid2 = "hello@yandex.ru";

            finesForwarder.accept(create(cluster, "asdga@ergw.ru"));
            Assert.assertEquals(0, mailSender.callCount());
            finesForwarder.accept(create(cluster, valid1));
            Assert.assertEquals(1, mailSender.callCount());
            finesForwarder.accept(create(cluster, valid1));
            //not sent
            Assert.assertEquals(1, mailSender.callCount());

            finesForwarder.accept(create(cluster, valid2));
            Assert.assertEquals(2, mailSender.callCount());
            finesForwarder.accept(create(cluster, valid2));
            Assert.assertEquals(3, mailSender.callCount());
            finesForwarder.accept(create(cluster, "asdga@ergw.ru"));
            Assert.assertEquals(3, mailSender.callCount());
            finesForwarder.accept(create(cluster, valid2));
            Assert.assertEquals(4, mailSender.callCount());
            finesForwarder.accept(create(cluster, valid2));
            //peremail still esists, total limit exhausted
            Assert.assertEquals(4, mailSender.callCount());
            finesForwarder.accept(create(cluster, valid2));
            Assert.assertEquals(4, mailSender.callCount());
            Assert.assertEquals(
                4,
                mailSender.callCount("yndx.fines@yandex.ru"));
            //check reset limiters
            finesForwarder.accept(
                create(
                    cluster,
                    valid2,
                    System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)));
            Assert.assertEquals(5, mailSender.callCount());
        }
    }

    @Test
    public void testForbiddenTypes() throws Exception {
        try (IexProxyCluster cluster = new IexProxyCluster(this)) {
            ImmutableIexForwardersConfig config = loadConfig();

            MockMailSender mailSender = new MockMailSender();
            MultiIexForwarder forwarder =
                new MultiIexForwarder(
                    mailSender,
                    config.forwarder("ticket"));
            final String yndxTicket = "yndx.ticket@yandex.ru";

            forwarder.accept(
                create(cluster, "ticket@aeroflot.ru", MessageType.S_EVENT));
            Assert.assertEquals(1, mailSender.callCount());
            Assert.assertEquals(1, mailSender.callCount(yndxTicket));

            // ignore people mail type
            forwarder.accept(
                create(cluster, "ticket@s7.ru", MessageType.PEOPLE));
            Assert.assertEquals(1, mailSender.callCount());
            Assert.assertEquals(1, mailSender.callCount(yndxTicket));

            // ignore corp mails
            forwarder.accept(
                create(
                    cluster,
                    "ticket@thy.com",
                    GOOD_TIME,
                    "1120000001111111",
                    MessageType.S_EVENT));
            Assert.assertEquals(1, mailSender.callCount());
            Assert.assertEquals(1, mailSender.callCount(yndxTicket));
        }
    }

    private static final class MockEntityContext
        extends AbstractEntityContext
    {
        private MockEntityContext(
            final IexProxy iexProxy,
            final ProxySession session,
            final Map<?, ?> json)
            throws HttpException, JsonUnexpectedTokenException
        {
            super(iexProxy, session, json);
        }
    }

    private static AbstractEntityContext create(
        final IexProxyCluster cluster,
        final String email,
        final long ts)
        throws Exception
    {
        return create(cluster, "", "", email, ts, UID);
    }

    private static AbstractEntityContext create(
        final IexProxyCluster cluster,
        final String email,
        final long ts,
        final String uid,
        final MessageType... types)
        throws Exception
    {
        String typesStr =
            Arrays.stream(types)
                .map(MessageType::name)
                .collect(Collectors.joining(","));
        return create(cluster, typesStr, "", email, ts, uid);
    }

    private static AbstractEntityContext create(
        final IexProxyCluster cluster,
        final String email,
        final MessageType... types)
        throws Exception
    {
        return create(cluster, email, GOOD_TIME, UID, types);
    }

    private static AbstractEntityContext create(
        final IexProxyCluster cluster,
        final String email)
        throws Exception
    {
        return create(cluster, "", "", email, GOOD_TIME, UID);
    }

    private static AbstractEntityContext create(
        final IexProxyCluster cluster,
        final String types,
        final String subject,
        final String email,
        final long ts,
        final String uid)
        throws Exception
    {
        QueryConstructor qc = new QueryConstructor("/test?");
        qc.append("uid", uid);
        qc.append("mid", MID);
        qc.append("email", email);
        qc.append("types", types);
        qc.append("subject", subject);
        qc.append("received_date", String.valueOf(RECEIVED_DATE));
        qc.append("user_email", USER_EMAIL);

        MockHttpExchange exchange = new MockHttpExchange(qc.toString());

        HttpCoreContext coreContext = HttpCoreContext.create();
        coreContext.setAttribute(
            "http.connection",
            new MockServerConnection(ts));
        coreContext.setAttribute(
            HttpProxy.LOGGER,
            new PrefixedLogger(
                Logger.getAnonymousLogger(),
                "forward",
                ""));

        return new MockEntityContext(
            cluster.iexproxy(),
            new BasicProxySession(
                cluster.iexproxy(),
                exchange,
                coreContext),
            JsonMap.EMPTY);
    }

    private static class MockMailSender implements MailSender {
        private final AtomicInteger callCount = new AtomicInteger(0);
        private final Map<String, AtomicInteger> targets =
            new ConcurrentHashMap<>();

        @Override
        public void send(final SendmailContext context) {
            callCount.incrementAndGet();

            targets.putIfAbsent(context.getTo(), new AtomicInteger(0));
            targets.get(context.getTo()).incrementAndGet();
        }

        public int callCount() {
            return callCount.get();
        }

        public int callCount(final String email) {
            return targets.get(email).get();
        }
    }
    // CSON: MagicNumber
    // CSON: ParameterNumber
    // CSON: MultipleStringLiterals
}
