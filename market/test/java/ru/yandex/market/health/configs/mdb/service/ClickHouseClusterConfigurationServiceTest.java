package ru.yandex.market.health.configs.mdb.service;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.clickhouse.BalancedClickhouseDataSource;
import ru.yandex.clickhouse.ClickHouseConnection;
import ru.yandex.clickhouse.ClickHouseDataSource;
import ru.yandex.market.health.configs.clickhouse.config.ClickHouseClusterConfig;
import ru.yandex.market.health.configs.clickhouse.config.ClusterConnectionConfig;
import ru.yandex.market.health.configs.clickhouse.service.ClickHouseClusterConfigurationService;
import ru.yandex.market.health.configs.clickhouse.service.ClusterBalancedDataSourceFactory;
import ru.yandex.market.health.configs.clickhouse.service.ClusterDataSourceFactory;
import ru.yandex.market.health.configs.clickhouse.spring.ClickHouseClusterSpringConfig;

@Disabled
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ClickHouseClusterConfigurationServiceTest.TestConfig.class})
@TestPropertySource(locations = "classpath:test.properties")
class ClickHouseClusterConfigurationServiceTest {
    @Autowired
    ClickHouseClusterConfigurationService clickHouseClusterConfigurationService;
    @Autowired
    ClusterConnectionConfig clusterConnectionConfig;
    @Autowired
    ClusterBalancedDataSourceFactory balancedDataSourceFactory;
    @Autowired
    ClusterDataSourceFactory dataSourceFactory;

    @Test
    public void test() throws InterruptedException {
        Thread.sleep(2000);
        List<ClickHouseClusterConfig> clusterConfigs = Arrays.asList(
            clickHouseClusterConfigurationService.getClusterConfigByClusterId("mdbqigl4m89hc106eli4").orElse(null),
            clickHouseClusterConfigurationService.getClusterConfigByClusterId("market_health@1234").orElse(null)
        );
        BalancedClickhouseDataSource balancedDataSource = null;
        ClickHouseDataSource dataSource = null;
        for (ClickHouseClusterConfig clusterConfig : clusterConfigs) {
            if (clusterConfig.isMdbCluster()) {
                balancedDataSource = balancedDataSourceFactory.create(clusterConfig);
            } else {
                dataSource = dataSourceFactory.create(clusterConfig);
            }
        }

        String sql = "SELECT DISTINCT * FROM system.clusters";

        try (ClickHouseConnection wmsConnection = balancedDataSource.getConnection()) {
            ResultSet resultSet = wmsConnection.createStatement().executeQuery(sql);
            resultSet.next();
            String cluster = resultSet.getString("cluster");
            System.out.println(cluster);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Thread.sleep(1000 * 60);
    }

    @Configuration
    @EnableScheduling
    @Import(ClickHouseClusterSpringConfig.class)
    static class TestConfig {

    }
}
