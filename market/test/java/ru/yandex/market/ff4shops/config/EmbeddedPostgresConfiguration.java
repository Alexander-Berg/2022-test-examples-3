package ru.yandex.market.ff4shops.config;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import net.ttddyy.dsproxy.listener.DataSourceQueryCountListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import ru.yandex.market.ff4shops.pg.PGConfigBuilder;
import ru.yandex.market.ff4shops.pg.PGEmbeddedDatabase;
import ru.yandex.market.ff4shops.pg.PGEmbeddedDatasource;
import ru.yandex.market.request.datasource.trace.TraceQueryExecutionListener;
import ru.yandex.market.request.trace.Module;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;
import ru.yandex.yql.YqlDataSource;

/**
 * @author fbokovikov
 */
@Configuration
public class EmbeddedPostgresConfiguration {

    private static final String POSTGRES_VERSION = "10.5-1";
    // If you use monterrey
    //private static final String POSTGRES_VERSION = "11.1-1";

    @Value("${ff4shops.datasource.username}")
    private String username;

    @Value("${ff4shops.datasource.password}")
    private String password;

    @Bean
    public PostgresConfig postgresConfig() {
        try {
            return new PGConfigBuilder(POSTGRES_VERSION)
                    .setUser(username)
                    .setPassword(password)
                    .build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Bean(name = "pgEmbeddedDatabase")
    public PGEmbeddedDatabase pgEmbeddedDatabase() {
        return new PGEmbeddedDatabase(
                postgresConfig(),
                new File("").getAbsolutePath()
        );
    }

    @Bean(name = "dataSource")
    @DependsOn("pgEmbeddedDatabase")
    public DataSource dataSource() {
        HikariDataSource hikariDataSource = new HikariDataSource();
        PGEmbeddedDatasource pgEmbeddedDataSource = new PGEmbeddedDatasource();
        pgEmbeddedDataSource.setConfig(postgresConfig());
        pgEmbeddedDataSource.setDefaultSchemaName(username);
        hikariDataSource.setDataSource(pgEmbeddedDataSource);
        hikariDataSource.setRegisterMbeans(true);
        return hikariDataSource;
    }

    @Bean(name = "tmsDataSource")
    @DependsOn("pgEmbeddedDatabase")
    public DataSource tmsDataSource() {
        return dataSource();
    }

    @Bean
    public DataSourceTraceBeanPostProcessorWithCount dataSourceTraceBeanPostProcessor() {
        return new DataSourceTraceBeanPostProcessorWithCount();
    }

    private static class DataSourceTraceBeanPostProcessorWithCount implements BeanPostProcessor {
        /**
         * {@inheritDoc}
         */
        @Override
        public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
            if (bean instanceof YqlDataSource) {
                return bean;
            }
            if (bean instanceof DataSource) {
                return ProxyDataSourceBuilder
                        .create((DataSource) bean)
                        .listener(new TraceQueryExecutionListener(Module.PGAAS))
                        .listener(new DataSourceQueryCountListener())
                        .build();
            }
            return bean;
        }
    }

}

