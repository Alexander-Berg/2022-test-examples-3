package ru.yandex.market.stat.hyperduct.config;

import java.util.Properties;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ResourceLoader;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 31.05.19
 */
@Configuration
public class LiquibaseTestConfig {
    @DependsOn({"mainDataSource"})
    @Bean
    public SpringLiquibase mainLiquibase(DataSource metadataDataSource,
                                         ResourceLoader resourceLoader,
                                         @Value("${mstat.hyperduct.metadata.liquibase.contexts}") String contexts,
                                         @Value("${mstat.hyperduct.metadata.jdbc.schema:}") String jdbcSchema) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setChangeLog("classpath:liquibase/db-changelog.xml");
        liquibase.setContexts(contexts);
        liquibase.setDataSource(metadataDataSource);
        liquibase.setResourceLoader(resourceLoader);
        if (jdbcSchema.isEmpty()) {
            liquibase.setDefaultSchema("public");
        } else {
            Properties props = System.getProperties();
            props.setProperty("mstat.hyperduct.liquibase.databaseChangeLogTableName",
                    "DATABASECHANGELOG_" + jdbcSchema.toUpperCase());
            props.setProperty("mstat.hyperduct.liquibase.databaseChangeLogLockTableName",
                    "DATABASECHANGELOGLOCK_" + jdbcSchema.toUpperCase());
            liquibase.setDefaultSchema(jdbcSchema);
        }
        return liquibase;
    }
}
