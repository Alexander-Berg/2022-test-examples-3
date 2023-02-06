package ru.yandex.market.pers.qa;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import ru.yandex.market.pers.test.db.EmbeddedPostgreFactory;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 29.02.2020
 */
@Configuration
public class DatabaseMockConfiguration {

    public DatabaseMockConfiguration() throws IOException {
    }

    @Bean(destroyMethod = "close")
    public Object embeddedPostgres() {
        return EmbeddedPostgreFactory.embeddedPostgres(x -> x);
    }

    @Bean
    public DataSource embeddedDatasource() {
        return EmbeddedPostgreFactory.embeddedDatasource(embeddedPostgres(), Map.of());
    }

    @Bean
    @Qualifier("datasource")
    public DataSource dataSourceMock() {
        return embeddedDatasource();
    }

    @Bean
    @Qualifier("stDatasource")
    public DataSource stDataSourceMock(@Qualifier("datasource") DataSource dataSource) {
        return dataSource;
    }

    @Bean
    public SpringLiquibase springLiquibase(@Qualifier("datasource") DataSource dataSource) {
        SpringLiquibase result = new SpringLiquibase();
        result.setDataSource(dataSource);
        result.setResourceLoader(new DefaultResourceLoader());
        result.setChangeLog("classpath:/changesets/changelog.xml");
        result.setChangeLogParameters(Collections.singletonMap("is-unit-testing", "true"));
        return result;
    }

    public static void applySqlScript(String sqlFile, DataSource dataSource) {
        ResourceDatabasePopulator scriptLauncher = new ResourceDatabasePopulator();
        scriptLauncher.addScript(new ClassPathResource(sqlFile));
        scriptLauncher.execute(dataSource);
    }

}
