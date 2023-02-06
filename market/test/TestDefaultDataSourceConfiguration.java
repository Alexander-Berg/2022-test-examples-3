package ru.yandex.market.jmf.db.test;

import javax.inject.Named;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.embedded.postgresql.EmbeddedPostgresRunner;
import ru.yandex.embedded.postgresql.PostgresRunner;
import ru.yandex.embedded.postgresql.RecipePostgresRunner;
import ru.yandex.market.crm.util.CrmStrings;
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

    @Bean
    public DbTestTool dbTestTool(
            @Named(Constants.DEFAULT_DATA_SOURCE) DataSource dataSource,
            @Value("${sql.clear.script}") String[] clearScriptPaths
    ) {
        if (clearScriptPaths.length == 1 && CrmStrings.isSpringPlaceHolder(clearScriptPaths[0])) {
            // Зарезолвить значение по умолчанию через аннотацию, почему-то, не получается
            // Конструкция "${sql.clear.script:/sql/clearDatabase.sql}" и её вариации
            // всегда возврещают "/sql/clearDatabase.sql"
            clearScriptPaths = new String[]{"/sql/clearDatabase.sql"};
        }
        return new DbTestTool(dataSource, clearScriptPaths);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public PostgresRunner postgres() {
        if (Boolean.parseBoolean(System.getenv("YA_TESTS"))) {
            return new RecipePostgresRunner();
        }
        return new EmbeddedPostgresRunner();
    }
}
