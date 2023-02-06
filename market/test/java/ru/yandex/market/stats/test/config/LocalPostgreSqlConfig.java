package ru.yandex.market.stats.test.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres;
import ru.yandex.qatools.embed.postgresql.distribution.Version;

import javax.sql.DataSource;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Profile("integration-tests")
@Configuration
@Order(HIGHEST_PRECEDENCE + 1)
public class LocalPostgreSqlConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public EmbeddedPostgres postgreSqlServer() {
        return new EmbeddedPostgres(Version.V10_6);
    }

    @Bean
    public DataSource metadataDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(org.postgresql.Driver.class.getCanonicalName());
        dataSource.setJdbcUrl(postgreSqlServer().getConnectionUrl().orElseThrow(IllegalStateException::new));
        dataSource.setMaximumPoolSize(15);
        dataSource.setConnectionTimeout(10_000L);
        return dataSource;
    }

    @Bean
    public NamedParameterJdbcTemplate metadataTemplate() {
        return new NamedParameterJdbcTemplate(metadataDataSource());
    }

    @Bean(autowire = Autowire.BY_NAME)
    public DataSourceTransactionManager metadataTransactionManager() {
        return new DataSourceTransactionManager(metadataDataSource());
    }

    @Bean
    public TransactionTemplate metadataTransactionTemplate() {
        return new TransactionTemplate(metadataTransactionManager());
    }
}
