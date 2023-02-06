package ru.yandex.market.mbo.db.pg;

import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import liquibase.Contexts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mboc.common.infrastructure.sql.AppNameTransactionHelper;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;

@Configuration
public class BasePgTestConfig {

    private static final Contexts LIQUIBASE_CONTEXTS = new Contexts("tests_only");

    @Bean
    public PlatformTransactionManager platformTransactionManager() {
        return new DataSourceTransactionManager(
            siteCatalogPgDb()
        );
    }

    @Bean
    @Primary
    public TransactionHelper siteCatalogTransactionHelper() {
        return new AppNameTransactionHelper(
            new NamedParameterJdbcTemplate(siteCatalogPgDb()),
            new TransactionTemplate(platformTransactionManager()),
            "test"
        );
    }

    @Bean
    public DataSource siteCatalogPgDb() {
        return createDataSourceWithMigration("mbo-db/site_catalog_pg.changelog.xml");
    }

    @Bean
    public DataSource marketModelPgDb() {
        return createDataSourceWithMigration("mbo-db/market_model_pg.changelog.xml");
    }

    @Bean
    public DataSource mboIrTmsDb() {
        return createDataSourceWithMigration("mbo-db/mbo_ir_tms_pg.changelog.xml");
    }

    @Bean
    public DataSource mboTmsDb() {
        return createDataSourceWithMigration("mbo-db/mbo_tms_pg.changelog.xml");
    }

    private static DataSource createDataSourceWithMigration(String classpathMigrationResource) {
        try {
            final DataSource postgresDatabase = EmbeddedPostgres.builder()
                .start()
                .getPostgresDatabase();
            LiquibasePreparer.forClasspathLocation(classpathMigrationResource, LIQUIBASE_CONTEXTS)
                .prepare(postgresDatabase);
            return postgresDatabase;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
