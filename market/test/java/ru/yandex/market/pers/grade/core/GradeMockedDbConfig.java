package ru.yandex.market.pers.grade.core;

import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import ru.yandex.market.pers.service.common.trace.PersTraceUtils;
import ru.yandex.market.pers.test.db.EmbeddedPostgreFactory;
import ru.yandex.market.request.trace.Module;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 13.05.2021
 */
@Configuration
public class GradeMockedDbConfig {

    @Autowired
    private ResourceLoader resourceLoader;

    @Bean(destroyMethod = "close")
    public Object embeddedPostgres() {
        return EmbeddedPostgreFactory.embeddedPostgres(x -> x);
    }

    @Bean
    public DataSource embeddedDatasource() {
        return PersTraceUtils.wrapTrace(
            EmbeddedPostgreFactory.embeddedDatasource(embeddedPostgres(), Map.of("currentSchema", "grade")),
            Module.PGAAS);
    }

    @Bean
    public SpringLiquibase pgLiquibase() throws LiquibaseException {
        DataSource postgresDatabase = embeddedDatasource();
        SpringLiquibase result = new SpringLiquibase();
        result.setDataSource(postgresDatabase);
        result.setChangeLog("classpath:changelog.xml");
        result.setChangeLogParameters(Collections.singletonMap("is-unit-testing", "true"));
        result.setResourceLoader(resourceLoader);
        result.setLiquibaseSchema("public");
        result.afterPropertiesSet();

        // randomize start sequences
        applySqlScript(postgresDatabase, "db_init.sql");
        return result;
    }

    private void applySqlScript(DataSource dataSource, String sql) {
        ResourceDatabasePopulator scriptLauncher = new ResourceDatabasePopulator();
        scriptLauncher.addScript(new ClassPathResource(sql));
        scriptLauncher.execute(dataSource);
    }

    @Bean
    public DataSource gradePgDataSource(SpringLiquibase liquibase) {
        return embeddedDatasource();
    }

    @Bean
    public DataSource gradePgDataSourceNoTimeout(SpringLiquibase liquibase) {
        return gradePgDataSource(liquibase);
    }

    @Bean
    public DataSource gradePgStDataSource(SpringLiquibase liquibase) {
        return gradePgDataSource(liquibase);
    }

}
