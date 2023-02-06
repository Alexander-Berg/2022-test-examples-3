package stockstorage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import stockstorage.testdata.TestService;

import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.TestDatabaseInitializer;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.DatabaseCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.base.CompoundDatabaseCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.liquibase.LiquibaseSchemaCleanerConfigProvider;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.SchemaCleanerConfigProvider;
import ru.yandex.market.logistics.test.integration.db.container.EnablePostgreSQLContainer;

@Configuration
@EnablePostgreSQLContainer
@Import(DbUnitTestConfiguration.class)
public class TestConfiguration {

    @Bean
    public TestService testBean() {
        return new TestService();
    }

    @Bean
    public TestDatabaseInitializer testDatabaseInitializer() {
        return dataSource -> new JdbcTemplate(dataSource).execute("SELECT 1");
    }

    /**
     * Объяви бин типа {@link DatabaseCleanerConfig}
     * для того что бы определить таблицы котрые не надо удалять перед тестами
     * если в контексте объявлен бин типа {@link liquibase.integration.spring.SpringLiquibase}
     * таблицы котрые создает ликвибейз будут заигнорены
     * {@link LiquibaseSchemaCleanerConfigProvider}
     *
     * @return
     * @see CompoundDatabaseCleanerConfig
     */
    @Bean
    public SchemaCleanerConfigProvider stockStorageCleanerConfig() {
        return SchemaCleanerConfigProvider.builder()
            .schema("some").truncateOnly("SOME_TABLE")
            .schema("some2").truncateAllExcept("SOME_TABLE2")
            .schema("some3").truncateAll()
            .schema("dome3")
            .dontResetSequences()
            .truncateAll().build();
    }
}
