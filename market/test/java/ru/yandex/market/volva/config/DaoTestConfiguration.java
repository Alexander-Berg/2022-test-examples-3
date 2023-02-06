package ru.yandex.market.volva.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.volva.data.DatasourcePack;
import ru.yandex.market.volva.data.DbCredentials;
import ru.yandex.market.volva.data.MultiSourceJdbcTemplate;


/**
 * @author dzvyagin
 */
@Configuration
@Import({
        EmbeddedPgConfig.class
})
public class DaoTestConfiguration {

    @Value("${volva.jdbc.url}")
    private String jdbcUrl;
    @Value("${volva.jdbc.driver}")
    private String jdbcDriver;
    @Value("${volva.jdbc.username}")
    private String jdbcUsername;
    @Value("${volva.jdbc.password}")
    private String jdbcPassword;
    @Value("${volva.jdbc.schema:}")
    private String jdbcSchema;


    @Bean
    public DatasourcePack datasourcePack() {
        return new DatasourcePack(
                DbCredentials.builder()
                        .jdbcUrl(jdbcUrl)
                        .jdbcDriver(jdbcDriver)
                        .jdbcSchema(jdbcSchema)
                        .jdbcUsername(jdbcUsername)
                        .jdbcPassword(jdbcPassword)
                        .build()
        );
    }

    @Bean
    @Primary
    public NamedParameterJdbcOperations pgaasJdbcOperations(DatasourcePack datasourcePack) {
        return new MultiSourceJdbcTemplate(datasourcePack);
    }

    @Bean
    public PlatformTransactionManager transactionManager(DatasourcePack datasourcePack) {
        return new DataSourceTransactionManager(datasourcePack.getMasterDataSource());
    }

    @Bean
    public TransactionTemplate pgaasTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
