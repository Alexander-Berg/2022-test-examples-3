package ru.yandex.market.mbo.integration.test.config.db;

import com.google.common.collect.ImmutableMap;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.mbo.core.conf.databases.MboOracleDBConfig;
import ru.yandex.market.mbo.core.conf.databases.MboTmsDBConfig;

import javax.sql.DataSource;

/**
 * @author s-ermakov
 */
@Configuration
@Import({
    MboOracleDBConfig.class,
    MboTmsDBConfig.class
})
public class SpringLiquibaseConfig {

    @Value("${market_content.scat.default_schema}")
    private String marketContentSchema;

    @Value("${market_content_draft.scat.default_schema}")
    private String marketContentDraftSchema;

    @Value("${site_catalog.scat.default_schema}")
    private String siteCatalogSchema;

    @Value("${market_depot.scat.default_schema}")
    private String watchesSchema;

    @Bean(name = "marketContentSpringLiquibase")
    public SpringLiquibase marketContentSpringLiquibase(
        @Qualifier("contentDataSource") DataSource dataSource,
        @Value("${market_content.scat.liquibase.change_log}") String changeLog) {
        return springLiquibase(dataSource, marketContentSchema, changeLog);
    }

    @Bean(name = "marketContentDraftSpringLiquibase")
    public SpringLiquibase marketContentDraftSpringLiquibase(
        @Qualifier("contentDraftDataSource") DataSource dataSource,
        @Value("${market_content_draft.scat.liquibase.change_log}") String changeLog) {
        return springLiquibase(dataSource, marketContentDraftSchema, changeLog);
    }

    @Bean(name = "marketDepotSpringLiquibase")
    public SpringLiquibase marketDepotSpringLiquibase(
        @Qualifier("marketDepotDataSource") DataSource dataSource,
        @Value("${market_depot.scat.liquibase.change_log}") String changeLog) {
        return springLiquibase(dataSource, watchesSchema, changeLog);
    }

    @Bean(name = "siteCatalogSpringLiquibase")
    public SpringLiquibase siteCatalogSpringLiquibase(
        @Qualifier("siteCatalogOracleDataSource") DataSource dataSource,
        @Value("${site_catalog.scat.liquibase.change_log}") String changeLog) {
        return springLiquibase(dataSource, siteCatalogSchema, changeLog);
    }

    private SpringLiquibase springLiquibase(DataSource dataSource, String defaultSchema, String changeLog) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setDefaultSchema(defaultSchema);
        liquibase.setChangeLog(changeLog);
        liquibase.setContexts("tests_only");
        liquibase.setChangeLogParameters(ImmutableMap.of(
            "market_content", marketContentSchema,
            "market_content_draft", marketContentDraftSchema,
            "site_catalog", siteCatalogSchema,
            "watches", watchesSchema
        ));
        return liquibase;
    }
}
