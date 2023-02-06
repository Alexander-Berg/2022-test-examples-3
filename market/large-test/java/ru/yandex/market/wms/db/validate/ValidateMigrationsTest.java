package ru.yandex.market.wms.db.validate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.configuration.HubConfiguration;
import liquibase.configuration.LiquibaseConfiguration;
import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;
import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

public class ValidateMigrationsTest {
    public static final String DB_SECRET_ID = "sec-01fkjd4e7j3xsjxgfb9cqcbz7g";

    private static final Predicate<String> MATCHER_DB_PROP = Pattern.compile("Db.*(User|Pass)").asPredicate();

    private final Logger log = org.slf4j.LoggerFactory.getLogger(ValidateMigrationsTest.class);

    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    @ClassRule
    private static DockerComposeContainer<?> container =
            new DockerComposeContainer<>(asFile("docker-compose.yml"))
                    .withExposedService("infordb", 1433)
                    .waitingFor("infordb", new HostPortWaitStrategy());

    @BeforeAll
    public static void startContainer() {
        container.start();
    }

    @AfterAll
    public static void stopContainer() {
        container.stop();
    }

    @BeforeAll
    public static void setupEnv() throws Exception {
        injectSecretsIntoEnv();
    }

    @Test
    public void testMigrations() {
        Assertions.assertDoesNotThrow(this::runMigrations);
    }

    public void runMigrations() throws Exception {
        int servicePort = container.getServicePort("infordb", 1433);

        ConParams params = new ConParams(
                "jdbc:sqlserver://localhost:" + servicePort,
                System.getProperty("DbWmsAdminUser"),
                System.getProperty("DbWmsAdminPass"));

        //Disabling HUB to avoid "Skipping auto-registration" messages in system.out
        LiquibaseConfiguration.getInstance().getConfiguration(HubConfiguration.class).setLiquibaseHubMode("OFF");

        Path scprdChangelogPath = asPath("changelog-master.xml");
        migrateDb(params, "SCPRD", scprdChangelogPath.getParent(), scprdChangelogPath);
        migrateDb(params, "SCPRDMST", scprdChangelogPath.getParent(), scprdChangelogPath);
        migrateDbs(params, asPath("db"));

        Path loginsRoot = asPath("users");
        migrateDb(params, "SCPRD", loginsRoot, loginsRoot.resolve("changelog-master.xml"));
        migrateDbs(params, asPath("users/db"));

        log.info("All databases migrated and rolled back successfully");
    }

    private void migrateDbs(ConParams params, Path root) throws Exception {
        Files.newDirectoryStream(root).forEach(p -> {
            if (Files.isDirectory(p)) {
                try {
                    Path dbChangelogPath = p.resolve("changelog-master.xml");
                    migrateDb(params, p.getFileName().toString(), root, dbChangelogPath);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void migrateDb(ConParams params, String dbName, Path pathToRoot, Path pathToChangelog) throws Exception {
        log.info("Executing {} migrations", dbName);

        try (Connection con = DriverManager.getConnection(params.getUrl(), params.getUser(), params.getPass())) {
            try (Statement stmt = con.createStatement()) {
                switchDbWaitingRecovery(dbName, stmt);
            }

            ResourceAccessor ra = new FileSystemResourceAccessor(pathToRoot.toFile());
            DatabaseConnection lbCon = new JdbcConnection(con);

            Date beforeMigration = new Date();
            try (Liquibase lb = new Liquibase(pathToRoot.relativize(pathToChangelog).toString(), ra, lbCon)) {
                List<ChangeSet> unrunChangesets = lb.listUnrunChangeSets(null, null);
                int changesetsNum = unrunChangesets.size();

                if (changesetsNum > 0) {
                    log.info("Collected {} unrun changesets", changesetsNum);

                    lb.update((String) null);
                    log.info("Changesets executed");

                    lb.rollback(beforeMigration, (Contexts) null, null);
                    log.info("Changesets rolled back");
                } else {
                    log.info("No changesets to run");
                }
            }
        }
    }

    private void switchDbWaitingRecovery(String dbName, Statement stmt) throws Exception {
        Instant deadline = Instant.now().plus(2, ChronoUnit.MINUTES);
        while (Instant.now().isBefore(deadline)) {
            try {
                stmt.execute("USE " + dbName);
                break;
            } catch (SQLException e) {
                if (shouldWaitAndRetry(e)) {
                    log.info("DB is not ready, retrying: {}", e.getMessage());
                    Thread.sleep(5000);
                } else {
                    throw e;
                }
            }
        }
    }

    private boolean shouldWaitAndRetry(SQLException e) {
        String msg = e.getMessage();
        return msg.endsWith("is being recovered. Waiting until recovery is finished.")
            || msg.contains("Lock request time out period exceeded");
    }

    /**
     * Добавляет в окружение логины и пароли пользователей БД из секрета, с которым создавался образ базы.
     *
     * @throws Exception
     */
    private static void injectSecretsIntoEnv() throws Exception {
        VaultClient vc;
        String token = System.getenv("YAV_TOKEN");
        if (token != null) {
            vc = new HttpVaultClient(token);
        } else {
            vc = new YaVaultClient();
        }
        Map<String, String> entries = vc.getSecretEntries(DB_SECRET_ID);
        entries.keySet().stream()
                .filter(MATCHER_DB_PROP)
                .forEach(k -> System.setProperty(k, entries.get(k)));
    }

    private static Path asPath(String resource) {
        return asFile(resource).toPath();
    }

    private static File asFile(String resource) {
        return new File(ValidateMigrationsTest.class.getClassLoader().getResource(resource).getFile());
    }

    private static class ConParams {
        private String url;
        private String user;
        private String pass;

        ConParams(String url, String user, String pass) {
            this.url = url;
            this.user = user;
            this.pass = pass;
        }

        String getUrl() {
            return url;
        }

        String getUser() {
            return user;
        }

        String getPass() {
            return pass;
        }
    }
}
