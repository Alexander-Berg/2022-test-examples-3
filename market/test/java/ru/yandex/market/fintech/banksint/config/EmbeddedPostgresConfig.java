package ru.yandex.market.fintech.banksint.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.sql.DataSource;

import de.flapdoodle.embed.process.runtime.Network;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.datasource.embedded.ConnectionProperties;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseConfigurer;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactoryBean;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import ru.yandex.market.common.postgres.test.PGConfigBuilder;
import ru.yandex.market.common.postgres.test.PGEmbeddedDatabase;
import ru.yandex.market.common.postgres.test.PGEmbeddedDatasource;
import ru.yandex.market.common.test.jdbc.InstrumentedDataSourceFactory;
import ru.yandex.market.common.test.spring.DbUnitTestConfig;
import ru.yandex.market.common.test.transformer.StringTransformer;
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;
import ru.yandex.qatools.embed.postgresql.distribution.Version;

@Configuration
@Import({
        EmbeddedPostgresConfig.EmbeddedDbConfig.class,
        EmbeddedPostgresConfig.EnvironmentDbConfig.class
})
public class EmbeddedPostgresConfig extends DbUnitTestConfig {
    private static final Logger log = LoggerFactory.getLogger(EmbeddedPostgresConfig.class);

    @Autowired
    PostgresConfig postgresConfig; // приедет из одного из конфигов в зависимости от окружения

    @Autowired(required = false)
    PGEmbeddedDatabase pgEmbeddedDatabase;

    @Override
    protected final EmbeddedDatabaseType databaseType() {
        return EmbeddedDatabaseType.H2; // не используется по факту, но null вернуть нельзя
    }

    @Override
    protected List<Resource> databaseResources() throws Exception {
        return Collections.emptyList();
    }

    @Override
    protected StringTransformer createSqlTransformer() {
        return string -> string;
    }

    @Override
    protected final EmbeddedDatabaseFactoryBean createDataSourceFactory(Resource... scripts) {
        var factoryBean = new EmbeddedDatabaseFactoryBean();
        factoryBean.setDatabaseConfigurer(new EmbeddedDatabaseConfigurer() {
            @Override
            public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
                // prefer config values
                var host = StringUtils.defaultIfBlank(postgresConfig.net().host(), "localhost");
                var port = postgresConfig.net().port();
                var db = StringUtils.defaultIfBlank(postgresConfig.storage().dbName(), databaseName);
                var user = StringUtils.defaultString(postgresConfig.credentials().username());
                var password = StringUtils.defaultString(postgresConfig.credentials().password());

                properties.setDriverClass(Driver.class);
                properties.setUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, db));
                properties.setUsername(user);
                properties.setPassword(password);
            }

            @Override
            public void shutdown(DataSource dataSource, String databaseName) {
                Connection con = null;
                try {
                    con = dataSource.getConnection();
                    if (con != null) {
                        con.close();
                    }
                } catch (SQLException ex) {
                    log.error("Could not close JDBC Connection on shutdown", ex);
                }
            }
        });
        factoryBean.setDataSourceFactory(new InstrumentedDataSourceFactory(createSqlTransformer()));
        factoryBean.setDatabaseName("testDataBase" + System.currentTimeMillis());
        return factoryBean;
    }

    @Conditional(EnvironmentDbConfig.ActivateCondition.class)
    @Configuration
    static class EnvironmentDbConfig {
        // переменные, специфичные для рецепта. можно задать для работы с локальным pg
        private static final String PG_LOCAL_PORT = "PG_LOCAL_PORT";
        private static final String PG_LOCAL_DATABASE = "PG_LOCAL_DATABASE";
        private static final String PG_LOCAL_USER = "PG_LOCAL_USER";
        private static final String PG_LOCAL_PASSWORD = "PG_LOCAL_PASSWORD";

        static class ActivateCondition implements Condition {
            @Override
            public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
                return StringUtils.isNotBlank(context.getEnvironment().getProperty(PG_LOCAL_PORT));
            }
        }

        @Autowired
        Environment environment;

        @Bean
        PostgresConfig postgresConfig() throws Exception {
            return new PostgresConfig(
                    Version.Main.PRODUCTION, // stub
                    new AbstractPostgresConfig.Net(
                            Network.getLocalHost().getHostAddress(),
                            Integer.parseInt(Objects.requireNonNull(environment.getProperty(PG_LOCAL_PORT),
                                    PG_LOCAL_PORT))
                    ),
                    new AbstractPostgresConfig.Storage(
                            Objects.requireNonNull(environment.getProperty(PG_LOCAL_DATABASE), PG_LOCAL_DATABASE)
                    ),
                    new AbstractPostgresConfig.Timeout(),
                    new AbstractPostgresConfig.Credentials(
                            environment.getProperty(PG_LOCAL_USER),
                            environment.getProperty(PG_LOCAL_PASSWORD)
                    )
            );
        }
    }

    @Conditional(EmbeddedDbConfig.ActivateCondition.class)
    @Configuration
    static class EmbeddedDbConfig {
        static class ActivateCondition implements Condition {
            @Override
            public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
                return !new EnvironmentDbConfig.ActivateCondition().matches(context, metadata);
            }
        }

        @Bean
        PostgresConfig postgresConfig() throws Exception {
            return new PGConfigBuilder()
                    .setVersion(Version.Main.PRODUCTION)
                    .build();
        }

        @Bean
        @DependsOn("pgEmbeddedDatabase")
        public PGEmbeddedDatasource postgresDatasource() throws Exception {
            return new PGEmbeddedDatasource(postgresConfig());
        }

        @Bean
        PGEmbeddedDatabase pgEmbeddedDatabase() throws Exception {
            return new PGEmbeddedDatabase(postgresConfig());
        }


    }

}
