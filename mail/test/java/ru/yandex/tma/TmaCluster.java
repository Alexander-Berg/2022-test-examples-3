package ru.yandex.tma;

import java.io.File;
import java.io.IOException;

import ru.yandex.client.pg.PgClientCluster;
import ru.yandex.client.pg.SqlQuery;
import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.FakeTvmServer;
import ru.yandex.http.test.StaticServer;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.test.util.TestBase;
import ru.yandex.tma.config.TmaConfigBuilder;

public class TmaCluster extends GenericAutoCloseableHolder<
    IOException,
    GenericAutoCloseableChain<IOException>>
{
    public static final String TVM_CLIENT_ID = "2035399";
    public static final String MESSENGER_GATEWAY_TVM_CLIENT_ID = "2031424";
    public static final String MESSENGER_GATEWAY_TVM_TICKET =
        "3:serv:CBAQ__________9_IggIx518EMD-ew:HQonuKDADLY7ow6EI6FC4oQa1x0H7Mf"
        + "YblteY6WcCvlzqFS95eqP-qLWPRe6iRy5ntASlMMSoPmE46Msc7PGGezuaAVMvz63m_"
        + "Jj08mI9MJxq8NdUSutYmQ5FlhELJzhToOobbFlRAXqlZDuebospp3fymFi6kGaaUFSr"
        + "RiJNy4";
    public static final String SERVICE_TICKET =
        "3:serv:CBAQ__________9_IggIwP57EMedfA:S7E-Mwi_0QJkIk-Q4uKVdkD7Uyb2Eip"
        + "iQfx1-kt6uT95p6QC9wUaex68hLDzKTEzXk7SRjyvOmkQKE0lsjFnFWp0bJAe8yVSlG"
        + "iP_b5MpHoeRhuLf7-XyXzArh1caHnH6odl3s-66MKyJxE_eVkAo7Ezj2ptjiAqIO_R4"
        + "sD5wAE";

    private final TestBase testBase;
    private final PgClientCluster pgClientCluster;
    private final FakeTvmServer tvm2;
    private final StaticServer messengerGateway;
    private final TmaServer tmaServer;

    public TmaCluster(final TestBase testBase) throws Exception {
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

            tvm2.addTicket(TVM_CLIENT_ID, SERVICE_TICKET);
            tvm2.addTicket(
                MESSENGER_GATEWAY_TVM_CLIENT_ID,
                MESSENGER_GATEWAY_TVM_TICKET);

            messengerGateway =
                new StaticServer(Configs.baseConfig("MessengerGateway"));
            chain.get().add(messengerGateway);

            System.setProperty("TMA_PORT", "0");
            System.setProperty("TMA_HTTPS_PORT", "0");
            System.setProperty("SMPP_PORT", "0");
            System.setProperty("BALANCER_NAME", "tma.so.yandex.net");
            System.setProperty("INSTANCE_TAG_CTYPE", "prod");
            System.setProperty("TVM_CLIENT_ID", TVM_CLIENT_ID);
            System.setProperty("SECRET", "1234567890123456789012");
            System.setProperty(
                "MESSENGER_GATEWAY_TVM_CLIENT_ID",
                MESSENGER_GATEWAY_TVM_CLIENT_ID);
            System.setProperty(
                "MESSENGER_GATEWAY_HOST",
                messengerGateway.host().toString());
            System.setProperty("MESSENGER_SERVICE", "yasms_auth");
            System.setProperty(
                "MESSENGER_ACCOUNT",
                "yango_pro_africa_whatsapp");
            System.setProperty("SMPP_PASSWORD", "Secret");
            System.setProperty("AES_KEY", "MTIzNDU2Nzg5MDEyMzQ1Ng==");
            System.setProperty("AES_IV", "AAAAAAAAAAAAAAAAAAAAAA==");
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
                            "mail/so/daemons/tma/tma_service/files/tma.conf")));
            TestBase.clearLoggerSection(ini.section("log"));
            TestBase.clearLoggerSection(ini.section("accesslog"));
            TestBase.clearLoggerSection(ini.section("smppaccesslog"));
            TestBase.clearLoggerSection(ini.section("stderr"));
            TestBase.clearLoggerSection(ini.section("tskvlog"));
            ini.section("tvm2").put("blackbox-env", "test");
            ini.section("messenger-gateway").sections().remove("https");
            ini.section("pg-client").keys().remove("pem-certificate");
            ini.section("server").sections().remove("https");

            tmaServer =
                new TmaServer(
                    new TmaConfigBuilder(new TmaConfigBuilder(ini)).build());
            chain.get().add(tmaServer);
            ini.checkUnusedKeys();

            reset(chain.release());
        }
    }

    public void start() throws Exception {
        messengerGateway.start();
        tmaServer.start();
    }

    public PgClientCluster pgClientCluster() {
        return pgClientCluster;
    }

    public StaticServer messengerGateway() {
        return messengerGateway;
    }

    public TmaServer tmaServer() {
        return tmaServer;
    }

    public void clearDatabase() throws Exception {
        pgClientCluster.client()
            .executeOnMaster(
                new SqlQuery(
                    "drop-schema",
                    "DROP SCHEMA IF EXISTS tma CASCADE"))
            .get();
    }

    public void applyMigration1() throws Exception {
        pgClientCluster.executeScript(
            testBase.loadResourceAsString(
                "mail/so/daemons/tma/database/migrations/V001__Initial.sql"),
            testBase.logger());
    }

    public void applyMigration2() throws Exception {
        pgClientCluster.executeScript(
            testBase.loadResourceAsString(
                "mail/so/daemons/tma/database/migrations"
                + "/V002__FailedState.sql"),
            testBase.logger());
    }

    public void applyMigration3() throws Exception {
        pgClientCluster.executeScript(
            testBase.loadResourceAsString(
                "mail/so/daemons/tma/database/migrations"
                + "/V003__AddTimezoneToTimestamps.sql"),
            testBase.logger());
    }

    public void applyMigration4() throws Exception {
        pgClientCluster.executeScript(
            testBase.loadResourceAsString(
                "mail/so/daemons/tma/database/migrations"
                + "/V004__AddHostname.sql"),
            testBase.logger());
    }

    public void applyMigration5() throws Exception {
        pgClientCluster.executeScript(
            testBase.loadResourceAsString(
                "mail/so/daemons/tma/database/migrations"
                + "/V005__NotNullHostname.sql"),
            testBase.logger());
    }

    public void prepareDatabase() throws Exception {
        clearDatabase();
        applyMigration1();
        applyMigration2();
        applyMigration3();
        applyMigration4();
        applyMigration5();
    }
}

