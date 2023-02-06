package ru.yandex.market.mboc.common.config;

import java.util.function.Supplier;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;

@Import({
    TestYqlOverPgDatasourceConfig.class
})
@Configuration
public class TestYqlAutoClusterConfig extends YqlAutoClusterConfig {
    public TestYqlAutoClusterConfig(TestYqlOverPgDatasourceConfig testYqlOverPgDatasourceConfig) {
        super(testYqlOverPgDatasourceConfig, null);
    }

    @Override
    public YtAutoCluster ytAutoCluster() {
        return YtAutoCluster.create(UnstableInit.notInit(), UnstableInit.notInit(),
            () -> YqlAutoClusterJdbcOperations.Cluster.PRIMARY);
    }

    @Override
    public YtAndYqlJdbcAutoCluster ytAutoClusterAndJdbcOperations() {
        return YtAndYqlJdbcAutoCluster.create(UnstableInit.notInit(), UnstableInit.notInit(), null, null,
            () -> YqlAutoClusterJdbcOperations.Cluster.PRIMARY);
    }

    @Override
    protected Supplier<YqlAutoClusterJdbcOperations.Cluster> availableClusterSupplier() {
        return () -> YqlAutoClusterJdbcOperations.Cluster.PRIMARY;
    }
}
