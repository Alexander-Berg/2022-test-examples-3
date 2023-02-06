package ru.yandex.direct.dbutil.testing;

import java.util.TimeZone;

import org.apache.commons.lang3.ArrayUtils;
import org.jooq.ExecuteListener;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.config.EssentialConfiguration;
import ru.yandex.direct.db.config.DbConfigFactory;
import ru.yandex.direct.dbutil.configuration.DbUtilConfiguration;
import ru.yandex.direct.dbutil.sharding.ShardSupport;
import ru.yandex.direct.dbutil.sharding.ShardedValuesGenerator;
import ru.yandex.direct.dbutil.wrapper.DatabaseWrapperProvider;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.mysql.MySQLInstance;

import static java.util.Collections.emptyMap;
import static ru.yandex.direct.utils.DateTimeUtils.MOSCOW_TIMEZONE;

/**
 * Переопределяет некоторые бин-дефинишены из common
 * для подключения к тестовой базе
 */
@Configuration
@Import({DbUtilConfiguration.class, EssentialConfiguration.class})
public class DbUtilTestingConfiguration {
    // инициализация тестовой БД и конфига

    @Bean(name = EssentialConfiguration.ENVIRONMENT_TYPE)
    static EnvironmentType environmentType() {
        // тесты должны работать в той же таймзоне, что и production
        // но гарантировать настройку всех все тестовые стендов мы не можем,
        // поэтому явно выставляем нужную нам timezone-у
        TimeZone.setDefault(TimeZone.getTimeZone(MOSCOW_TIMEZONE));

        return EnvironmentType.DB_TESTING;
    }


    @Bean(name = DbUtilConfiguration.DB_CONFIG_FACTORY)
    public DbConfigFactory dbConfigFactory(TestDbInitializer testDbInitializer, DirectConfig directConfig) {
        String dbConfigContent;
        if (testDbInitializer.isEnabled()) {
            String dbConfigTemplateUri = directConfig.getString("db_config_template");
            dbConfigContent = createDynamicDbConfig(dbConfigTemplateUri, testDbInitializer.getConnector());
        } else {
            String dbConfigUri = directConfig.getString("db_config");
            dbConfigContent = LiveResourceFactory.get(dbConfigUri).getContent();
        }
        return new DbConfigFactory(dbConfigContent, emptyMap());
    }

    @Bean
    public TestDbInitializer testDbInitializer(DirectConfig directConfig) {
        boolean dynamicDb = directConfig.getBoolean("use_dynamic_db");
        return new TestDbInitializer(dynamicDb);
    }

    private String createDynamicDbConfig(String templateUri, MySQLInstance mySQLConnector) {
        String dbConfigTemplate = LiveResourceFactory.get(templateUri).getContent();
        return String.format(dbConfigTemplate, mySQLConnector.getHost(), mySQLConnector.getPort(),
                mySQLConnector.getUsername(), mySQLConnector.getPassword());
    }

    @Bean
    public ShardSupport shardSupport(DatabaseWrapperProvider databaseWrapperProvider,
                                     ShardedValuesGenerator valuesGenerator,
                                     @Value("${db_shards}") int numOfPpcShards) {
        return new TestShardSupport(databaseWrapperProvider, valuesGenerator, numOfPpcShards);
    }

    @Bean
    public DslContextProvider dslContextProvider(DatabaseWrapperProvider databaseWrapperProvider) {
        DslContextProvider provider = new DslContextProvider(databaseWrapperProvider);
        addExecuteListener(provider.ppc(1).configuration(), new JooqExplainExecuteListener());
        return provider;
    }

    private void addExecuteListener(org.jooq.Configuration configuration, ExecuteListener listener) {
        var executeListenerProviders = configuration.executeListenerProviders();
        configuration.set(ArrayUtils.add(executeListenerProviders, new DefaultExecuteListenerProvider(listener)));
    }
}
