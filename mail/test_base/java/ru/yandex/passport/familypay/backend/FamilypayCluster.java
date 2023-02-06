package ru.yandex.passport.familypay.backend;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import ru.yandex.client.pg.PgClientCluster;
import ru.yandex.client.pg.SqlQuery;
import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.FakeBlackboxServer;
import ru.yandex.http.test.FakeTvmServer;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.logbroker2.MessageSenderCluster;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.passport.familypay.backend.config.FamilyPaybackConfigBuilder;
import ru.yandex.passport.familypay.snitch.Snitch;
import ru.yandex.passport.familypay.snitch.config.SnitchConfigBuilder;
import ru.yandex.test.util.TestBase;
import ru.yandex.util.filesystem.CloseableDeleter;

public class FamilypayCluster extends GenericAutoCloseableHolder<
    IOException,
    GenericAutoCloseableChain<IOException>>
{
    public static final String BLACKBOX_TVM_CLIENT_ID = "222";
    public static final String BLACKBOX_TVM_TICKET = "here the ticket";
    public static final String SO_FRAUD_TVM_CLIENT_ID = "2019898";
    public static final String SO_FRAUD_TVM_TICKET = "so_fraud ticket";
    public static final String YANDEXPAY_TVM_CLIENT_ID = "1";
    public static final String YANDEXPAY_TVM_TICKET = "yandexpay ticket";
    public static final String TVM_CLIENT_ID = "2021718";
    public static final String FAMILY_INFO_PREFIX =
        "/blackbox/?format=json&method=family_info&family_id=";
    public static final String SERVICE_TICKET =
        "3:serv:CBAQ__________9_IggIuqR7ENayew:I3BgmYvyoGQCUtpfLGxN3tjDFM1tRS0"
        + "DLlCkffk8D1ThAczqx86OzBtVcOusUjTYXCkq5slitDKzYYaD9vGxsQvelKWaYw0vDz"
        + "RxZwFqsK7Gs5epvaXOOcAXOz_ggb1vFP0fQtMffU_cEkAAJqoo25Hyrg9zfBN_sj8-l"
        + "muRaNI";
    public static final String USER_TICKET_100500 =
        "3:user:CA0Q__________9_GhIKBAiUkQYQlJEGINKF2MwEKAE:Lchvmmy2d4l1prK2T-"
        + "OsfP4sTMrcGz8h9TuNZGnngIUmoYmVUstDJlL9kmKKpn-GmSTT__-nhtJFGuypcYhP2"
        + "EYNZm74Zd08PzehA4lvVKIn9BfHLNUCbsaht3QT0MUfeU9WX3kD3jtjoEM50PC9MU9j"
        + "EMNrlL4yjNo0u8GNGms";
    public static final String USER_TICKET_100501 =
        "3:user:CA0Q__________9_GhIKBAiVkQYQlZEGINKF2MwEKAE:FO0vqQS6d45ZcM7Bt0"
        + "x6ML1osn4KP2zBtxFBEuQ5_OL0nCc356HDEX0wpmX_FBoVzLQmyQB0fDZCUmgWM1fLT"
        + "Sp2cppurF2gtA4BjRkESXorpLA8VanjnGZywrdwB0JQhMBUofcyJaLIRmKJAsZZtW0T"
        + "cWFRPhuXosZCzn0VZVo";
    public static final String USER_TICKET_100509 =
        "3:user:CA0Q__________9_GhIKBAidkQYQnZEGINKF2MwEKAE:Mzn6H7qqrEDqhxoKV2"
        + "Nn6GfmXoE90b8PmaNucEwhH47nnnWydiVGFWoJNRCjrcA3fgpSkxAl7IOPt6ZRakHyh"
        + "jzI_jEqcwzmmsS6zlyvuVmJBbi_Wc7x5HQNWdiiRFyIunoR1wuhE9RPBpqEbjhDekN4"
        + "jKWFjhFlJPrAnwtFXo8";
    public static final String USER_TICKET_100510 =
        "3:user:CA0Q__________9_GhIKBAiekQYQnpEGINKF2MwEKAE:GeiUbpCJg140_BvvwO"
        + "ScN0toF2qs1bvfFzC3zJrdOwuN7cdeOafv9D0wHkDelFIe0b5zNBAbAWaMp1WsZZ0BD"
        + "6XGDnQO8syJ19AEm_ADPUdFPBPP9_6m0gX_698K1y5ENFsNhNskU7m4lL5viRvlCJ-9"
        + "hPaeIPptQmnsGZR8E3c";
    public static final String NO_SECURE_PHONE = "\"phones\": []";
    public static final String SECURE_PHONE_TEMPLATE =
        "\"phones\": [{\"id\":\"235_uid_\",\"attributes\":{\"108\":\"1\"}}]";

    private final TestBase testBase;
    private final PgClientCluster pgClientCluster;
    private final FakeTvmServer tvm2;
    private final FakeBlackboxServer blackbox;
    private final StaticServer bunker;
    private final StaticServer antifraud;
    private final FamilypayBackend familypayBackend;
    private final StaticServer yandexpay;
    private final Snitch snitch;
    private final MessageSenderCluster passportMessageSenderCluster;
    private final MessageSenderCluster cardEventsMessageSenderCluster;
    private final MessageSenderCluster glogoutMessageSenderCluster;
    private final MessageSenderCluster phoneEventsMessageSenderCluster;

    public FamilypayCluster(final TestBase testBase) throws Exception {
        this.testBase = testBase;
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<>()))
        {
            pgClientCluster = new PgClientCluster(testBase);
            chain.get().add(pgClientCluster);

            tvm2 = FakeTvmServer.fromContext(testBase, chain.get());

            tvm2.addTicket(BLACKBOX_TVM_CLIENT_ID, BLACKBOX_TVM_TICKET);
            tvm2.addTicket(SO_FRAUD_TVM_CLIENT_ID, SO_FRAUD_TVM_TICKET);
            tvm2.addTicket(YANDEXPAY_TVM_CLIENT_ID, YANDEXPAY_TVM_TICKET);
            tvm2.addTicket(TVM_CLIENT_ID, SERVICE_TICKET);

            blackbox = FakeBlackboxServer.fromContext(testBase, chain.get());

            bunker = new StaticServer(Configs.baseConfig("Bunker"));
            chain.get().add(bunker);
            String bunkerUri =
                "/v1/cat?node=/fpay/config&version=stable";
            bunker.add(
                bunkerUri,
                testBase.loadResourceAsString(
                    "mail/so/familypay/familypay_backend/test_base"
                    + "/resources/ru/yandex/passport/familypay/backend"
                    + "/services.json"));
            bunker.start();
            System.setProperty(
                "BUNKER_URI",
                bunker.host().toString() + bunkerUri);

            antifraud = new StaticServer(Configs.baseConfig("Antifraud"));
            chain.get().add(antifraud);

            System.setProperty("FAMILYPAY_BACKEND_PORT", "0");
            System.setProperty("FAMILYPAY_BACKEND_HTTPS_PORT", "0");
            System.setProperty(
                "BALANCER_NAME",
                "familypay-backend.so.yandex.net");
            // TODO: remove
            System.setProperty("INSTANCE_TAG_CTYPE", "prod");
            System.setProperty("SERVICE_CONFIG", "empty.conf");
            System.setProperty("TVM_CLIENT_ID", TVM_CLIENT_ID);
            System.setProperty("SECRET", "1234567890123456789012");
            System.setProperty("PASSPORT_TVM_CLIENT_ID", "2010646");
            System.setProperty("TAXI_TVM_CLIENT_ID", "2011198");
            System.setProperty("TAXI_LK_TVM_CLIENT_ID", "2031266");
            System.setProperty(
                "BLACKBOX_TVM_CLIENT_ID",
                BLACKBOX_TVM_CLIENT_ID);
            System.setProperty("SO_FRAUD_HOST", antifraud.host().toString());
            System.setProperty(
                "SO_FRAUD_TVM_CLIENT_ID",
                SO_FRAUD_TVM_CLIENT_ID);
            System.setProperty(
                "PGHOSTS",
                "localhost:" + System.getenv("PG_LOCAL_PORT"));
            System.setProperty(
                "PGDATABASE",
                System.getenv("PG_LOCAL_DATABASE"));
            System.setProperty(
                "PGUSER",
                System.getenv("PG_LOCAL_USER"));
            System.setProperty(
                "PGPASSWORD",
                System.getenv("PG_LOCAL_PASSWORD"));
            System.setProperty(
                "CONFIG_DIRS",
                Paths.getSourcePath(
                    "mail/tools/nanny_helpers/nanny_service_base/files"));

            IniConfig ini =
                new IniConfig(
                    new File(
                        Paths.getSourcePath(
                            "mail/so/familypay/familypay_backend_service/files"
                            + "/familypay_backend.conf")));
            TestBase.clearLoggerSection(ini.section("log"));
            TestBase.clearLoggerSection(ini.section("accesslog"));
            TestBase.clearLoggerSection(ini.section("stderr"));
            TestBase.clearLoggerSection(ini.section("tskvlog"));
            ini.section("tvm2").put("blackbox-env", "test");
            ini.section("antifraud").sections().remove("https");
            ini.section("pg-client").keys().remove("pem-certificate");
            ini.section("server.https.keystore")
                .put(
                    "file",
                    Paths.getSourcePath(
                        "mail/library/http/http_test/main/resources/ru/yandex"
                        + "/http/test/localhost.jks"));
            CloseableDeleter localCacheDir =
                new CloseableDeleter(Files.createTempDirectory("local-cache"));
            chain.get().add(localCacheDir);
            ini.section("server")
                .put(
                    "local-cache-dir",
                    localCacheDir.path().toString());

            familypayBackend =
                new FamilypayBackend(
                    new FamilyPaybackConfigBuilder(
                        new FamilyPaybackConfigBuilder(ini))
                        .build());
            chain.get().add(familypayBackend);
            ini.checkUnusedKeys();

            yandexpay = new StaticServer(Configs.baseConfig("Yandexpay"));
            chain.get().add(yandexpay);

            String familypayBackendHost =
                familypayBackend.httpHost().toString();
            System.setProperty("FAMILYPAY_HOST", familypayBackendHost);
            System.setProperty("YANDEXPAY_HOST", yandexpay.host().toString());
            System.setProperty(
                "YANDEXPAY_TVM_CLIENT_ID",
                YANDEXPAY_TVM_CLIENT_ID);

            System.setProperty("SNITCH_PORT", "0");
            ini =
                new IniConfig(
                    new File(
                        Paths.getSourcePath(
                            "mail/so/familypay/familypay_lbconsumer_service"
                            + "/files/snitch.conf")));
            TestBase.clearLoggerSection(ini.section("log"));
            TestBase.clearLoggerSection(ini.section("accesslog"));
            TestBase.clearLoggerSection(ini.section("stderr"));
            ini.section("tvm2").put("blackbox-env", "test");
            ini.section("familypay").sections().remove("https");
            ini.section("yandexpay").sections().remove("https");
            snitch =
                new Snitch(
                    new SnitchConfigBuilder(new SnitchConfigBuilder(ini))
                        .build());
            chain.get().add(snitch);
            ini.checkUnusedKeys();

            System.setProperty("SNITCH_PORT", Integer.toString(snitch.port()));
            System.setProperty(
                "FAMILY_EVENTS_CLIENT_ID",
                "family-events-consumer");
            System.setProperty(
                "FAMILY_EVENTS_BALANCER_HOSTS",
                "lbkx.logbroker.yandex.net");
            System.setProperty("FAMILY_EVENTS_TOPICS", "family-events-topic");

            System.setProperty(
                "CARD_EVENTS_CLIENT_ID",
                "card-events-consumer");
            System.setProperty(
                "CARD_EVENTS_BALANCER_HOSTS",
                "lbkx.logbroker.yandex.net");
            System.setProperty("CARD_EVENTS_TOPICS", "card-events-topic");

            System.setProperty(
                "GLOGOUT_EVENTS_CLIENT_ID",
                "family-events-consumer");
            System.setProperty(
                "GLOGOUT_EVENTS_BALANCER_HOSTS",
                "lbkx.logbroker.yandex.net");
            System.setProperty("GLOGOUT_EVENTS_TOPICS", "passport-topic");

            System.setProperty(
                "PHONE_EVENTS_CLIENT_ID",
                "family-events-consumer");
            System.setProperty(
                "PHONE_EVENTS_BALANCER_HOSTS",
                "lbkx.logbroker.yandex.net");
            System.setProperty("PHONE_EVENTS_TOPICS", "passport-topic");

            passportMessageSenderCluster = new MessageSenderCluster(
                testBase,
                "",
                "",
                1,
                false,
                "mail/library/logbroker/logbroker2_consumer_service/files"
                + "/familypay-consumer.conf",
                "family-events",
                familypayBackendHost);
            chain.get().add(passportMessageSenderCluster);

            cardEventsMessageSenderCluster = new MessageSenderCluster(
                testBase,
                "",
                "",
                1,
                false,
                "mail/library/logbroker/logbroker2_consumer_service/files"
                + "/familypay-consumer.conf",
                "card-events",
                familypayBackendHost);
            chain.get().add(cardEventsMessageSenderCluster);

            glogoutMessageSenderCluster = new MessageSenderCluster(
                testBase,
                "",
                "",
                1,
                false,
                "mail/library/logbroker/logbroker2_consumer_service/files"
                + "/familypay-consumer.conf",
                "glogout-events",
                familypayBackendHost);
            chain.get().add(glogoutMessageSenderCluster);

            phoneEventsMessageSenderCluster = new MessageSenderCluster(
                testBase,
                "",
                "",
                1,
                false,
                "mail/library/logbroker/logbroker2_consumer_service/files"
                + "/familypay-consumer.conf",
                "phone-events",
                familypayBackendHost);
            chain.get().add(phoneEventsMessageSenderCluster);

            reset(chain.release());
        }
    }

    public void start() throws Exception {
        antifraud.start();
        familypayBackend.start();
        yandexpay.start();
        snitch.start();
        passportMessageSenderCluster.start();
        cardEventsMessageSenderCluster.start();
        glogoutMessageSenderCluster.start();
        phoneEventsMessageSenderCluster.start();
    }

    @Override
    public void close() throws IOException {
        try {
            // Need some time to complete all in-air requests after
            // fire-and-forget, like pushes
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
        }
        super.close();
    }

    public PgClientCluster pgClientCluster() {
        return pgClientCluster;
    }

    public FakeBlackboxServer blackbox() {
        return blackbox;
    }

    public StaticServer antifraud() {
        return antifraud;
    }

    public FamilypayBackend familypayBackend() {
        return familypayBackend;
    }

    public StaticServer yandexpay() {
        return yandexpay;
    }

    public MessageSenderCluster passportMessageSenderCluster() {
        return passportMessageSenderCluster;
    }

    public MessageSenderCluster cardEventsMessageSenderCluster() {
        return cardEventsMessageSenderCluster;
    }

    public MessageSenderCluster glogoutMessageSenderCluster() {
        return glogoutMessageSenderCluster;
    }

    public MessageSenderCluster phoneEventsMessageSenderCluster() {
        return phoneEventsMessageSenderCluster;
    }

    public String userinfo(final BlackboxUserState userState)
        throws Exception
    {
        String userinfo =
            testBase.loadResourceAsString(
                "mail/so/familypay/familypay_backend/test_base"
                + "/resources/ru/yandex/passport/familypay/backend"
                + "/blackbox-userinfo-template.json");
        if (userState.hasSecurePhone) {
            userinfo =
                userinfo.replace(NO_SECURE_PHONE, SECURE_PHONE_TEMPLATE);
        }
        return userinfo.replace("_uid_", Long.toString(userState.uid));
    }

    public void addBlackboxUserinfo(final BlackboxUserState userState)
        throws Exception
    {
        blackbox.addUserinfo(userState.uid, userinfo(userState));
    }

    public void addFamily(
        final String familyId,
        final long adminUid,
        final long... uids)
        throws Exception
    {
        BlackboxUserState[] users = new BlackboxUserState[uids.length];
        for (int i = 0; i < uids.length; ++i) {
            users[i] = new BlackboxUserState(uids[i], true);
        }
        addFamily(
            new BlackboxFamilyState(
                familyId,
                new BlackboxUserState(adminUid, true),
                users));
    }

    public void addFamily(final BlackboxFamilyState familyState)
        throws Exception
    {
        StringBuilder sb = new StringBuilder("{\"family\":{\"");
        sb.append(familyState.familyId);
        sb.append("\":{\"admin_uid\":\"");
        sb.append(familyState.adminState.uid);
        sb.append("\",\"users\":[{\"uid\":\"");
        sb.append(familyState.adminState.uid);
        addBlackboxUserinfo(familyState.adminState);
        for (int i = 0; i < familyState.membersState.length; ++i) {
            BlackboxUserState memberState = familyState.membersState[i];
            sb.append("\"},{\"uid\":\"");
            sb.append(memberState.uid);
            addBlackboxUserinfo(memberState);
        }
        sb.append("\"}]}}}");
        blackbox.add(
            FAMILY_INFO_PREFIX + familyState.familyId,
            new StaticHttpResource(new StaticHttpItem(new String(sb))));
    }

    public void clearDatabase() throws Exception {
        pgClientCluster.client()
            .executeOnMaster(
                new SqlQuery(
                    "drop-schema",
                    "DROP SCHEMA IF EXISTS familypay CASCADE"))
            .get();
    }

    public void applyMigration1() throws Exception {
        pgClientCluster.executeScript(
            testBase.loadResourceAsString(
                "mail/so/familypay/database/migrations/V001__Initial.sql"),
            testBase.logger());
    }

    public void applyMigration2() throws Exception {
        pgClientCluster.executeScript(
            testBase.loadResourceAsString(
                "mail/so/familypay/database/migrations"
                + "/V002__FailedPaymentStatus.sql"),
            testBase.logger());
    }

    public void applyMigration3() throws Exception {
        pgClientCluster.executeScript(
            testBase.loadResourceAsString(
                "mail/so/familypay/database/migrations"
                + "/V003__PaymentService.sql"),
            testBase.logger());
    }

    public void applyMigration4() throws Exception {
        pgClientCluster.executeScript(
            testBase.loadResourceAsString(
                "mail/so/familypay/database/migrations"
                + "/V004__UnlimSupport.sql"),
            testBase.logger());
    }

    public void applyMigration5() throws Exception {
        pgClientCluster.executeScript(
            testBase.loadResourceAsString(
                "mail/so/familypay/database/migrations"
                + "/V005__AddAllowedServices.sql"),
            testBase.logger());
    }

    public void applyMigration6() throws Exception {
        pgClientCluster.executeScript(
            testBase.loadResourceAsString(
                "mail/so/familypay/database/migrations"
                + "/V006__MigrateBlockedServices.sql"),
            testBase.logger());
    }

    public void applyMigration7() throws Exception {
        pgClientCluster.executeScript(
            testBase.loadResourceAsString(
                "mail/so/familypay/database/migrations"
                + "/V007__LimitCurrency.sql"),
            testBase.logger());
    }

    public void applyMigration8() throws Exception {
        pgClientCluster.executeScript(
            testBase.loadResourceAsString(
                "mail/so/familypay/database/migrations"
                + "/V008__SecurePhoneStatus.sql"),
            testBase.logger());
    }

    public void applyMigration9() throws Exception {
        pgClientCluster.executeScript(
            testBase.loadResourceAsString(
                "mail/so/familypay/database/migrations"
                + "/V009__NotNullSecurePhoneStatus.sql"),
            testBase.logger());
    }

    public void prepareDatabase() throws Exception {
        clearDatabase();
        applyMigration1();
        applyMigration2();
        applyMigration3();
        applyMigration4();
        applyMigration5();
        applyMigration6();
        applyMigration7();
        applyMigration8();
        applyMigration9();
    }

    public static class BlackboxUserState {
        private final long uid;
        private final boolean hasSecurePhone;

        public BlackboxUserState(
            final long uid,
            final boolean hasSecurePhone)
        {
            this.uid = uid;
            this.hasSecurePhone = hasSecurePhone;
        }
    }

    public static class BlackboxFamilyState {
        private final String familyId;
        private final BlackboxUserState adminState;
        private final BlackboxUserState[] membersState;

        public BlackboxFamilyState(
            final String familyId,
            final BlackboxUserState adminState,
            final BlackboxUserState... membersState)
        {
            this.familyId = familyId;
            this.adminState = adminState;
            this.membersState = membersState;
        }
    }

    public static class FamilyChange {
        private final String change;
        private final BlackboxFamilyState[] familiesStateAtTheMomentOfChange;

        public FamilyChange(
            final String change,
            final BlackboxFamilyState... familiesStateAtTheMomentOfChange)
        {
            this.change = change;
            this.familiesStateAtTheMomentOfChange =
                familiesStateAtTheMomentOfChange;
        }

        public String change() {
            return change;
        }

        public BlackboxFamilyState[] familiesStateAtTheMomentOfChange() {
            return familiesStateAtTheMomentOfChange;
        }
    }
}

