package ru.yandex.market.deepmind.common.config;

import javax.sql.DataSource;

import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.deepmind.common.utils.YqlOverPgUtils;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_NESTED;

@Profile("test")
@TestConfiguration
public class TestDeepmindSqlDatasourceConfig extends DeepmindSqlDatasourceConfig {
    @Primary
    @Bean
    @Override
    public DataSource deepmindDataSource() {
        // We don't have separate DB-s in tests, tests run in singe transaction,
        // so creating separate yql datasource doesn't work properly
        // So wrap existing connection to yql
        return ProxyDataSourceBuilder
            .create(super.deepmindDataSource())
            .queryTransformer(transformInfo -> YqlOverPgUtils.convertYqlToPgSql(transformInfo.getQuery()))
            .build();
    }

    @Bean
    @Override
    public DataSource slaveDeepmindDataSource() {
        // We don't have separate DB-s in tests, tests run in singe transaction,
        // so separate datasource doesn't have changes to work properly. So just link it to single bean.
        return deepmindDataSource();
    }

    @Primary
    @Bean
    @Override
    public TransactionTemplate deepmindSqlTransactionTemplate() {
        var transactionTemplate = super.deepmindSqlTransactionTemplate();
        // enable savepoints in transactionTemplate for GenericMapperRepositoryImpl
        transactionTemplate.setPropagationBehavior(PROPAGATION_NESTED);
        return transactionTemplate;
    }

    @Primary
    @Bean
    @Override
    public JdbcTemplate deepmindSqlJdbcTemplate() {
        return super.deepmindSqlJdbcTemplate();
    }

    @Primary
    @Bean
    @Override
    public PlatformTransactionManager deepmindSqlTransactionManager() {
        return super.deepmindSqlTransactionManager();
    }

    @Primary
    @Bean
    @Override
    public NamedParameterJdbcTemplate deepmindSqlNamedParameterJdbcTemplate() {
        return super.deepmindSqlNamedParameterJdbcTemplate();
    }

    @Primary
    @Bean
    @Override
    public TransactionHelper deepmindTransactionHelper() {
        return super.deepmindTransactionHelper();
    }
}
