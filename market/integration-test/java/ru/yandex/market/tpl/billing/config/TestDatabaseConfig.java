package ru.yandex.market.tpl.billing.config;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.Objects;

import javax.sql.DataSource;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import liquibase.integration.spring.SpringLiquibase;
import net.ttddyy.dsproxy.listener.logging.OutputParameterLogEntryCreator;
import net.ttddyy.dsproxy.listener.logging.SystemOutQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.data.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

import ru.yandex.market.common.postgres.test.PGConfigBuilder;
import ru.yandex.market.common.postgres.test.PGEmbeddedDatabase;
import ru.yandex.market.common.postgres.test.PGEmbeddedDatasource;
import ru.yandex.market.tpl.billing.utils.TplBillingDataTypeFactory;
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;
import ru.yandex.qatools.embed.postgresql.distribution.Version;

@ActiveProfiles(profiles = "integration-test")
@Configuration
@Import({
        RepositoryConfiguration.class,
        TestDatabaseConfig.EmbeddedDbConfig.class,
        TestDatabaseConfig.EnvironmentDbConfig.class
})
public class TestDatabaseConfig {
    @Bean
    public DatabaseDataSourceConnectionFactoryBean dbUnitDatabaseConnection(
            DataSource pgDataSource,
            DatabaseConfigBean dbUnitDatabaseConfig
    ) {
        DatabaseDataSourceConnectionFactoryBean dbConnection =
                new com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean(pgDataSource);
        dbConnection.setDatabaseConfig(dbUnitDatabaseConfig);
        dbConnection.setSchema("public");
        return dbConnection;
    }

    @Bean
    public DatabaseConfigBean dbUnitDatabaseConfig(TplBillingDataTypeFactory tplBillingDataTypeFactory) {
        DatabaseConfigBean databaseConfigBean = new DatabaseConfigBean();
        databaseConfigBean.setDatatypeFactory(tplBillingDataTypeFactory);
        databaseConfigBean.setAllowEmptyFields(true);
        return databaseConfigBean;
    }

    @Bean
    public TplBillingDataTypeFactory tplBillingDataTypeFactory() {
        return new TplBillingDataTypeFactory();
    }

    @Bean
    public EntityManagerFactoryDependsOnPostProcessor emDepends() {
        return new EntityManagerFactoryDependsOnPostProcessor("liquibase");
    }

    @Bean
    public AbstractPlatformTransactionManager transactionManager() {
        return new JpaTransactionManager();
    }

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource) {
        SpringLiquibase springLiquibase = new SpringLiquibase();
        springLiquibase.setChangeLog("classpath:changelog.xml");
        springLiquibase.setDataSource(dataSource);
        springLiquibase.setDefaultSchema("public");
        return springLiquibase;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource pgDataSource) {
        return new JdbcTemplate(pgDataSource);
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource pgDataSource) {
        return new NamedParameterJdbcTemplate(pgDataSource);
    }

    @Configuration
    @Conditional(EnvironmentDbConfig.ActivateCondition.class)
    static class EnvironmentDbConfig {
        // переменные, специфичные для рецепта. можно задать для работы с локальным pg
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
            // InetAddress.getLocalhost иногда может выдавать некорректный адрес вроде 127.0.1.1
            var host = InetAddress.getByName("localhost").getHostAddress();
            var port = Objects.requireNonNull(environment.getProperty(ENV_PORT, Integer.class), ENV_PORT);
            var db = Objects.requireNonNull(environment.getProperty(ENV_DATABASE), ENV_DATABASE);
            return new PostgresConfig(
                    Version.Main.PRODUCTION, // stub
                    new AbstractPostgresConfig.Net(host, port),
                    new AbstractPostgresConfig.Storage(db),
                    new AbstractPostgresConfig.Timeout(),
                    new AbstractPostgresConfig.Credentials(
                            environment.getProperty(ENV_USER),
                            environment.getProperty(ENV_PASSWORD))
            );
        }

        @Bean(name = {"dataSource", "pgDataSource"})
        public DataSource pgDataSource(PostgresConfig postgresConfig) {
            SystemOutQueryLoggingListener listener = new SystemOutQueryLoggingListener();
            OutputParameterLogEntryCreator queryLogEntryCreator = new OutputParameterLogEntryCreator();
            queryLogEntryCreator.setMultiline(true);
            listener.setQueryLogEntryCreator(queryLogEntryCreator);

            return new ProxyDataSourceBuilder()
                    .dataSource(new PGEmbeddedDatasource(postgresConfig))
                    .name("EmbeddedDatabase")
                    .listener(listener)
                    .build();
        }
    }

    @Configuration
    @Conditional(EmbeddedDbConfig.ActivateCondition.class)
    static class EmbeddedDbConfig {
        private static final String POSTGRES_VERSION = "10.5-1";

        static class ActivateCondition implements Condition {
            @Override
            public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
                return !new EnvironmentDbConfig.ActivateCondition().matches(context, metadata);
            }
        }

        @Bean
        public PostgresConfig postgresConfig() {
            try {
                return new PGConfigBuilder()
                        .setVersion(POSTGRES_VERSION)
                        .build();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Bean
        public PGEmbeddedDatabase pgEmbeddedDatabase() {
            boolean useUserHome = Files.isWritable(PGEmbeddedDatabase.USER_HOME.asFile().toPath());

            // Пытаемся использовать домашнюю директорию пользователя в качестве хранилища дистрибутива Postgres
            return useUserHome
                    ? new PGEmbeddedDatabase(postgresConfig(), PGEmbeddedDatabase.USER_HOME)
                    : new PGEmbeddedDatabase(postgresConfig(), new File(StringUtils.EMPTY).getAbsolutePath());
        }

        @Bean(name = {"dataSource", "pgDataSource"})
        @DependsOn("pgEmbeddedDatabase")
        public DataSource pgDataSource() {
            SystemOutQueryLoggingListener listener = new SystemOutQueryLoggingListener();
            OutputParameterLogEntryCreator queryLogEntryCreator = new OutputParameterLogEntryCreator();
            queryLogEntryCreator.setMultiline(true);
            listener.setQueryLogEntryCreator(queryLogEntryCreator);

            return new ProxyDataSourceBuilder()
                    .dataSource(new PGEmbeddedDatasource(postgresConfig()))
                    .name("EmbeddedDatabase")
                    .listener(listener)
                    .build();
        }
    }
}
