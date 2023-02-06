package ru.yandex.market.mbo.mdm.common.utils;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mboc.db.config.SqlDatasourceConfig;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_NESTED;

@Primary
@Configuration
public class TestMdmSqlDatasourceConfig extends SqlDatasourceConfig {

    @Override
    @Primary
    @DependsOn("liquibase")
    @Bean(name = {"sqlDataSource", "dataSource"})
    public DataSource sqlDataSource() {
        return super.sqlDataSource();
    }

    @Override
    @DependsOn("liquibase")
    @Bean
    public DataSource slaveSqlDataSource() {
        // We don't have separate DB-s in tests, tests run in singe transaction,
        // so separate datasource doesn't have changes to work properly. So just link it to single bean.
        return sqlDataSource();
    }

    @Override
    public TransactionTemplate sqlTransactionTemplate() {
        var transactionTemplate = super.sqlTransactionTemplate();
        // enable savepoints in transactionTemplate for GenericMapperRepositoryImpl
        transactionTemplate.setPropagationBehavior(PROPAGATION_NESTED);
        return transactionTemplate;
    }

    @Override
    public TransactionTemplate newTransactionTemplate() {
        return sqlTransactionTemplate();
    }
}
