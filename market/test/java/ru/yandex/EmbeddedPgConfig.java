package ru.yandex;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.abo.config.noauto.db.JpaConfig;
import ru.yandex.market.abo.test.db.EmbeddedTestPostgres;
import ru.yandex.market.abo.test.db.RecipeTestPostgres;
import ru.yandex.market.abo.test.db.TestPostgres;
import ru.yandex.market.request.datasource.trace.TraceQueryExecutionListener;
import ru.yandex.market.request.trace.Module;

/**
 * @author komarovns
 * @date 12.11.2019
 */
@Slf4j
@Configuration
@Import(JpaConfig.class)
public class EmbeddedPgConfig {

    @Bean(destroyMethod = "close")
    public TestPostgres embeddedPostgres(@Value("${market.abo.use.pg.recipe:false}") boolean useRecipe) {
        var pg = useRecipe ? new RecipeTestPostgres() : new EmbeddedTestPostgres();
        log.info("embedded pg: {}", pg.getJdbcUrl());
        return pg;
    }

    @Bean
    public DataSource embeddedPgDatasource(TestPostgres testPostgres) {
        return testPostgres.getDataSource();
    }

    @Primary
    @Bean(name = {"dataSource", "pgDataSource", "pgRoDataSource", "masterDataSource"})
    public DataSource dataSource(DataSource embeddedPgDatasource) {
        return ProxyDataSourceBuilder.create(embeddedPgDatasource)
                .listener(new TraceQueryExecutionListener(Module.PGAAS))
                .build();
    }

    @Bean(name = {
            "jdbcTemplate", "pgJdbcTemplate", "pgRoJdbcTemplate",
            "urlCheckerJdbcTemplate", "marketPingerPgJdbcTemplate"
    })
    @DependsOn("liquibase")
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public SpringLiquibase liquibase(@Qualifier("embeddedPgDatasource") DataSource embeddedPgDatasource) {
        var liquibase = new SpringLiquibase();
        liquibase.setDataSource(embeddedPgDatasource);
        liquibase.setChangeLog("classpath:test-liquibase.xml");
        return liquibase;
    }
}
