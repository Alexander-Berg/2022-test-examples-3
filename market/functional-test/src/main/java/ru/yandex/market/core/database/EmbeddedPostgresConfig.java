package ru.yandex.market.core.database;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.CompositeDatabasePopulator;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;

import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.request.trace.Module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.database.OracleToPostgresJdbcTemplate.ExpQueryRouting;

/**
 * Конфигурация, используемая в тестах, для запуска инстанса postgresql для тестов.
 *
 * @see <a href="https://jdbc.postgresql.org/documentation/head/connect.html">connect string docs</a>
 */
@Configuration
@Import({
        EmbeddedPostgresConfig.EmbeddedDbConfig.class,
        EmbeddedPostgresConfig.EnvironmentDbConfig.class
})
@ParametersAreNonnullByDefault
public class EmbeddedPostgresConfig {
    private static final Logger log = LoggerFactory.getLogger(EmbeddedPostgresConfig.class);
    private static final boolean EXP_DATASOURCE_READ_ONLY = false;
    private static final Supplier<ExpQueryRouting> EXP_QUERY_ROUTING = () -> ExpQueryRouting.ENABLED;
    // полный боевой changelog, включающий общие changesetы с ораклом
    static final String CHANGELOG_FULL = "changelog-pg.xml";
    // базовый боевой changelog, входит в CHANGELOG_FULL
    static final String CHANGELOG_TO_VALIDATE = "pg/changelog.xml";
    // порядок важен
    private static final List<String> CHANGELOGS = Stream.concat(
            Stream.of(
                    // порядок идентичен liquibasePgDev.sh
                    CHANGELOG_FULL
            ).peek(path -> assertThat(path)
                    .as("тут только production, тестовые скрипты добавляй в список ниже")
                    .doesNotContain("unittest", "functional_test", "functionalTest")),
            Stream.of(
                    // test
                    "unittest/pg/changelog.xml"
                    // тестовые супер-общие данные, грубо говоря дополнительные тестовые словари
                    // "unittest/data/market_billing_tariff.sql",
                    // "unittest/data/market_billing_r_active_billing_types.sql",
                    // "unittest/data/shops_web_regions.sql"
            )
    ).collect(Collectors.toUnmodifiableList());

    EmbeddedPostgresConfig() {
        // package private для запрета наследования
    }

    @Autowired
    private PostgresConfig postgresConfig;

    @PostConstruct
    void runLiquibaseMigrations() {
        // явно выключаем трансформер на время миграций на всякий случай
        var dataSource = dataSource();
        var transformer = flagStringTransformer();
        transformer.setShouldTransform(false);
        DatabasePopulatorUtils.execute(new CompositeDatabasePopulator(List.of(
                // ресурсы мы прогоним через liquibase, чтобы избежать проблем с несколькими запусками
                EmbeddedPostgresConfig::runLiquibase,
                // commit в конце, тк иногда после ResourceDatabasePopulator данные оказываются не видны тесту
                Connection::commit
        )), dataSource);
    }

    private static void runLiquibase(Connection connection) {
        var jdbcConnection = new JdbcConnection(connection);
        var resourceAccessor = new ClassLoaderResourceAccessor();
        var ctx = new Contexts("functionalTest");
        var labels = new LabelExpression();
        for (String changelogFile : CHANGELOGS) {
            log.info("Running liquibase for [{}]...", changelogFile);
            try {
                var liquibase = new Liquibase(changelogFile, resourceAccessor, jdbcConnection);
                liquibase.update(ctx, labels);
            } catch (LiquibaseException e) {
                throw new RuntimeException(e);
            }
            log.info("...done liquibase for [{}]", changelogFile);
        }
    }

    @Bean
    public DataSource dataSource() {
        return OracleToPostgresDatasourceFactory.enhance(
                hikariDataSource(),
                flagStringTransformer(),
                /*queryTimeout=*/null,
                /*tracingModule=*/null,
                /*failOnNestedConnections=*/false // TODO change to true and fix all issues
        );
    }

    /**
     * объявляем пул hikari отдельно, чтобы корректно работал lifecycle
     */
    @Bean
    DataSource hikariDataSource() {
        log.info(
                "Connecting to postgresql at [{}] with credentials [{}:{}]",
                postgresConfig.jdbcUrl, postgresConfig.user, postgresConfig.password
        );
        return OracleToPostgresDatasourceFactory.make(
                Module.PGAAS,
                false,
                postgresConfig.jdbcUrl,
                postgresConfig.user,
                postgresConfig.password,
                4,
                0,
                10
        );
    }

    static class PostgresConfig {
        final String jdbcUrl;
        final String user;
        final String password;

        PostgresConfig(
                String jdbcUrl,
                String user,
                String password
        ) {
            this.jdbcUrl = jdbcUrl;
            this.user = user;
            this.password = password;
        }
    }

    @Configuration
    @Conditional(EnvironmentDbConfig.ActivateCondition.class)
    static class EnvironmentDbConfig {
        // переменные, специфичные для рецепта. можно задать для работы с локальнй pg
        private static final String ENV_PORT = "PG_LOCAL_PORT";
        private static final String ENV_DATABASE = "PG_LOCAL_DATABASE";
        private static final String ENV_USER = "PG_LOCAL_USER";
        private static final String ENV_PASSWORD = "PG_LOCAL_PASSWORD";

        static class ActivateCondition implements Condition {
            @Override
            public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
                return StringUtils.isNotBlank(context.getEnvironment().getProperty(ENV_PORT));
            }
        }

        @Bean
        PostgresConfig postgresConfig(Environment environment) throws Exception {
            log.info("Using postgres from environment...");
            // InetAddress.getLocalhost иногда может выдавать некорректный адрес вроде 127.0.1.1
            var host = InetAddress.getByName("localhost").getHostAddress();
            var port = Objects.requireNonNull(environment.getProperty(ENV_PORT, Integer.class), ENV_PORT);
            var db = Objects.requireNonNull(environment.getProperty(ENV_DATABASE), ENV_DATABASE);
            return new PostgresConfig(
                    String.format("jdbc:postgresql://%s:%d/%s", host, port, db),
                    environment.getProperty(ENV_USER),
                    environment.getProperty(ENV_PASSWORD)
            );
        }
    }

    /**
     * Для быстрого локального запуска тестов сделайте себе ramdisk.
     * <br>
     * Для линукса например так:
     * <pre>
     * $ sudo mkdir /mnt/ramdisk
     * $ sudo mount -t tmpfs -o size=3G tmpfs /mnt/ramdisk
     * ...
     * $ sudo umount /mnt/ramdisk
     * </pre>
     * <br>
     * Для мака например так:
     * <pre>
     * diskutil erasevolume HFS+ 'ramdisk' `hdiutil attach -nobrowse -nomount ram://6291456`
     * </pre>
     * Там логика размеров несколько странная, ram://2048 = 1MB, соответственно ram://2097152 = 1GB и тд.
     * <p>
     * Путь к диску можно указать через переменную
     * <a href="https://wiki.yandex-team.ru/yatool/test/#ramdrive">YA_TEST_RAM_DRIVE_PATH</a>.
     */
    @Configuration
    @Conditional(EmbeddedDbConfig.ActivateCondition.class)
    static class EmbeddedDbConfig {
        private static final String ENV_RAMDISK = "YA_TEST_RAM_DRIVE_PATH";
        private static final int DESIRED_PORT = 0; // 0 means random free, set eg to 5432 to simplify local debugging

        static final class ActivateCondition implements Condition {
            @Override
            public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
                return !new EnvironmentDbConfig.ActivateCondition().matches(context, metadata);
            }
        }

        @Bean
        EmbeddedPostgres embeddedPostgres(Environment environment) throws Exception {
            @Nullable String envRamDiskPath = environment.getProperty(ENV_RAMDISK);
            @Nullable Path rootDir = null;
            if (StringUtils.isNotBlank(envRamDiskPath)) {
                rootDir = Paths.get(envRamDiskPath).toAbsolutePath();
            } else if (SystemUtils.IS_OS_MAC_OSX) {
                rootDir = Paths.get("/Volumes/ramdisk");
            } else if (SystemUtils.IS_OS_LINUX) {
                rootDir = Paths.get("/mnt/ramdisk");
            }

            // PGaaSZonkyInitializer is slightly overkill for now
            if (rootDir == null || !Files.isWritable(rootDir)) {
                log.warn("Running postgres locally without ramdisk, may affect tests performance");
                return EmbeddedPostgres.builder()
                        .setPort(DESIRED_PORT)
                        .setServerConfig("unix_socket_directories", "")
                        .start();
            } else {
                // в отличие от setOverrideWorkingDirectory выставление этого проперти
                // помогает удалить случайно оставшиеся старые данные
                // должно быть вызвано до создания билдера
                System.setProperty("ot.epg.working-dir", rootDir.toString());
                log.info("Running postgres locally using ramdisk [{}]", rootDir);
                return EmbeddedPostgres.builder()
                        .setPort(DESIRED_PORT)
                        .setServerConfig("unix_socket_directories", "")
                        .setDataDirectory(rootDir.resolve("PG-content-" + System.currentTimeMillis()))
                        .setOverrideWorkingDirectory(rootDir.toFile())
                        .start();
            }
        }

        @Bean
        PostgresConfig postgresConfig(EmbeddedPostgres embeddedPostgres) {
            log.info("Using local postgres...");
            var user = "postgres";
            var password = "postgres";
            var dbName = "postgres";
            return new PostgresConfig(
                    embeddedPostgres.getJdbcUrl(user, dbName),
                    user,
                    password
            );
        }
    }

    @Bean
    FlagStringTransformer flagStringTransformer() {
        return new FlagStringTransformer(
                SqlTransformers.caching(
                        SqlTransformers.composite(
                                ForbiddenSql.INSTANCE,
                                SqlTransformers.ora2pg()
                        ),
                        2048L // should be enough for tests
                ),
                false // do not transform initially
        );
    }

    private static class FlagStringTransformer implements SqlTransformers.Transformer {
        private final SqlTransformers.Transformer decoratee;
        private volatile boolean shouldTransform;

        FlagStringTransformer(
                SqlTransformers.Transformer decoratee,
                boolean transform
        ) {
            this.decoratee = decoratee;
            this.shouldTransform = transform;
        }

        /**
         * @param transform by default true
         */
        void setShouldTransform(boolean transform) {
            this.shouldTransform = transform;
        }

        @Override
        public String transform(String sql) {
            return shouldTransform
                    ? decoratee.transform(sql)
                    : sql;
        }
    }

    /**
     * Простой слушатель, который включает трансформер запросов если таковой есть в контексте только для тест-метода.
     */
    static class EnableTransformersOnlyForTestMethods extends AbstractTestExecutionListener {
        @Override
        public int getOrder() {
            // хотим уходить в проверки dbunit без трансформеров
            return DbUnitTestExecutionListener.LISTENER_ORDER + 100;
        }

        @Override
        public void beforeTestMethod(TestContext testContext) {
            toggle(testContext, true);
        }

        @Override
        public void afterTestMethod(TestContext testContext) {
            toggle(testContext, false);
        }

        private static void toggle(TestContext testContext, boolean transform) {
            try {
                testContext.getApplicationContext().getBean(FlagStringTransformer.class).setShouldTransform(transform);
            } catch (NoSuchBeanDefinitionException e) {
                // do nothing
            }
        }
    }

    public static PlatformTransactionManager txManager(DataSource dataSource) {
        return new OracleToPostgresDataSourceTransactionManager(
                faultyDataSource(),
                dataSource,
                EXP_DATASOURCE_READ_ONLY,
                EXP_QUERY_ROUTING
        );
    }

    public static JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new OracleToPostgresJdbcTemplate(
                faultyDataSource(),
                dataSource,
                EXP_DATASOURCE_READ_ONLY,
                EXP_QUERY_ROUTING,
                null
        );
    }

    private static DataSource faultyDataSource() {
        var dataSource = mock(DataSource.class, "faultyDataSource");
        try {
            when(dataSource.getConnection()).thenThrow(new CannotGetJdbcConnectionException("should not get here"));
        } catch (SQLException ignored) {
        }
        return dataSource;
    }
}
