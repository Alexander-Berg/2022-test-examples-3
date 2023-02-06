package ru.yandex.mail.so.cretur;

import java.io.File;
import java.io.IOException;

import ru.yandex.client.pg.PgClientCluster;
import ru.yandex.client.pg.SqlQuery;
import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.FakeTvmServer;
import ru.yandex.mail.so.cretur.config.CreturConfigBuilder;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.test.util.TestBase;

public class CreturCluster extends GenericAutoCloseableHolder<
    IOException,
    GenericAutoCloseableChain<IOException>>
{
    public static final String SINGLE_SO_TVM_CLIENT_ID = "2001103";
    public static final String SO_TVM_CLIENT_ID =
        SINGLE_SO_TVM_CLIENT_ID + ",2016973,2001119";
    public static final String SINGLE_SO_TVM_TICKET =
        "3:serv:CBAQ__________9_IggIz5F6EPucfA:WqPPT4ogglmWWHxQOJzYxS4piZUyNuj"
        + "oL-E8FiGLzjaRj1KK1XC10kTyv9twQfjdmoTwoazUTUZ9Xua_8QX_8HbbnSaJMFKsuq"
        + "hdUTwOgPA2yYTVtDP-l_OUaQLR4DgrX8F7o2SAkRjmtqfVkC5W2NP1I3qCOSJJVprKX"
        + "eHir4U";
    public static final String TVM_CLIENT_ID = "2035323";

    private final TestBase testBase;
    private final PgClientCluster pgClientCluster;
    private final Cretur cretur;

    public CreturCluster(final TestBase testBase) throws Exception {
        this.testBase = testBase;
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<>()))
        {
            pgClientCluster = new PgClientCluster(testBase);
            chain.get().add(pgClientCluster);

            FakeTvmServer.fromContext(testBase, chain.get());

            System.setProperty("CRETUR_PORT", "0");
            System.setProperty("CRETUR_HTTPS_PORT", "0");
            System.setProperty("BALANCER_NAME", "cretur.so.yandex.net");
            System.setProperty("INSTANCE_TAG_CTYPE", "prod");
            System.setProperty("TVM_CLIENT_ID", TVM_CLIENT_ID);
            System.setProperty("SECRET", "1234567890123456789012");
            System.setProperty("API360_TVM_CLIENT_ID", "2032926");
            System.setProperty("SO_TVM_CLIENT_ID", SO_TVM_CLIENT_ID);
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
                            "mail/so/daemons/cretur/cretur_service/files"
                            + "/cretur.conf")));
            TestBase.clearLoggerSection(ini.section("log"));
            TestBase.clearLoggerSection(ini.section("accesslog"));
            TestBase.clearLoggerSection(ini.section("stderr"));
            TestBase.clearLoggerSection(ini.section("tskvlog"));
            ini.section("tvm2").put("blackbox-env", "test");
            ini.section("pg-client").keys().remove("pem-certificate");
            ini.section("server.https.keystore")
                .put(
                    "file",
                    Paths.getSourcePath(
                        "mail/library/http/http_test/main/resources/ru/yandex"
                        + "/http/test/localhost.jks"));

            cretur =
                new Cretur(
                    new CreturConfigBuilder(new CreturConfigBuilder(ini))
                        .build());
            chain.get().add(cretur);
            ini.checkUnusedKeys();

            reset(chain.release());
        }
    }

    public void start() throws IOException {
        cretur.start();
    }

    public PgClientCluster pgClientCluster() {
        return pgClientCluster;
    }

    public Cretur cretur() {
        return cretur;
    }

    public void clearDatabase() throws Exception {
        pgClientCluster.client()
            .executeOnMaster(
                new SqlQuery(
                    "drop-schema",
                    "DROP SCHEMA IF EXISTS cretur CASCADE"))
            .get();
    }

    public void applyMigration1() throws Exception {
        pgClientCluster.executeScript(
            testBase.loadResourceAsString(
                "mail/so/daemons/cretur/database/migrations/V001__Initial.sql"),
            testBase.logger());
    }

    public void prepareDatabase() throws Exception {
        clearDatabase();
        applyMigration1();
    }
}

