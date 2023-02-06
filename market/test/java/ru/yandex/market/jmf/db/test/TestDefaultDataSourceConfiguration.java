package ru.yandex.market.jmf.db.test;

import javax.inject.Named;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.embedded.postgresql.EmbeddedPostgresRunner;
import ru.yandex.embedded.postgresql.PostgresRunner;
import ru.yandex.embedded.postgresql.RecipePostgresRunner;
import ru.yandex.market.jmf.db.Constants;
import ru.yandex.market.jmf.db.DataSourceConfig;
import ru.yandex.market.jmf.db.DefaultDataSourceConfiguration;
import ru.yandex.market.jmf.db.qualifiers.Shared;
import ru.yandex.market.jmf.tx.TxTestConfiguration;

/**
 * Используется в тестах OCRM
 */
@Configuration
@Import({
        DefaultDataSourceConfiguration.class,
        TxTestConfiguration.class,
})
@ComponentScan("ru.yandex.market.jmf.db.test.impl")
@PropertySource(name = "testDbSupportProperties", value = "classpath:test_db_support.properties")
public class TestDefaultDataSourceConfiguration {

    @Bean
    @Shared
    @Primary
    public DataSourceConfig sharedDataSourceConfig(
            @Named(Constants.SHARED_DATA_SOURCE_CONFIG) DataSourceConfig sharedDataSourceConfig,
            PostgresRunner postgresRunner) {
        return sharedDataSourceConfig.toBuilder()
                .withUrl(postgresRunner.getUrl().split("\\?")[0])
                .createDataSourceConfig();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public PostgresRunner postgres() {
        if (Boolean.parseBoolean(System.getenv("YA_TESTS"))) {
            return new RecipePostgresRunner();
        }
        return new EmbeddedPostgresRunner();
    }
}
