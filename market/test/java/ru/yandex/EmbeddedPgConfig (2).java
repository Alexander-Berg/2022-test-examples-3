package ru.yandex;

/**
 * @author imelnikov
 * @since 04.03.2021
 */

import javax.sql.DataSource;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import liquibase.integration.spring.SpringLiquibase;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author komarovns
 * @date 12.11.2019
 */
@Configuration
public class EmbeddedPgConfig {
    private static final int EMBEDDED_PG_RETRY_COUNT = 3;

    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws Throwable {
        Throwable lastException = null;
        for (int i = 0; i < EMBEDDED_PG_RETRY_COUNT; ++i) {
            try {
                return EmbeddedPostgres.start();
            } catch (Throwable e) {
                lastException = e;
            }
        }
        throw lastException;
    }

    @Bean
    public DataSource embeddedPgDatasource(EmbeddedPostgres embeddedPostgres) {
        return embeddedPostgres.getPostgresDatabase();
    }

    @Primary
    @Bean(name = {"ucDataSource"})
    public DataSource dataSource(DataSource embeddedPgDatasource) {
        return ProxyDataSourceBuilder.create(embeddedPgDatasource)
//                .listener(new TraceQueryExecutionListener(Module.PGAAS))
                .build();
    }

    @Bean(name = {"ucJdbcTemplate"})
    @DependsOn("liquibase")
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public SpringLiquibase liquibase(@Qualifier("embeddedPgDatasource") DataSource embeddedPgDatasource) {
        var liquibase = new SpringLiquibase();
        liquibase.setDataSource(embeddedPgDatasource);
        liquibase.setChangeLog("classpath:liquibase/db.changelog.xml");
        liquibase.setDropFirst(true);
        return liquibase;
    }
}

