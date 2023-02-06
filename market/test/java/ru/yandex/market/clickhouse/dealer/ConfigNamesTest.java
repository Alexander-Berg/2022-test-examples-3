package ru.yandex.market.clickhouse.dealer;

import java.util.Objects;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.application.monitoring.ComplicatedMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.clickhouse.dealer.config.DealerConfig;
import ru.yandex.market.clickhouse.dealer.config.DealerConfigurationService;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 08.07.2019
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ConfigNamesTest.class, DealerConfigurationSpringTestingConfig.class})
@PropertySource("classpath:test.properties")
public class ConfigNamesTest {
    @Autowired
    private DealerConfigurationService dealerConfigurationService;

    @Autowired
    private ComplicatedMonitoring complicatedMonitoring;

    @Value("${dealer.market-clickhouse.tm-cluster}")
    private String healthClickHouseClusterName;

    @Bean
    public ComplicatedMonitoring monitoring() {
        return new ComplicatedMonitoring();
    }

    @Test
    public void configLoads() {
        assertEquals(MonitoringStatus.OK, complicatedMonitoring.getResult().getStatus());
    }

    @Test
    public void configFileNamesAreCorrect() {
        for (DealerConfig dealerConfig : dealerConfigurationService.getConfigs()) {
            assertEquals(
                "Incorrect config name. Configs should be named 'cluster-name_db-name_table_name.json'. " +
                    "https://nda.ya.ru/3VMa4W.",
                String.format(
                    "%s_%s_%s.json",
                    Objects.equals(healthClickHouseClusterName, dealerConfig.getClickHouseCluster().getCluster())
                        ? "health"
                        : "mdb",
                    dealerConfig.getDistributedTable().getDatabase(),
                    dealerConfig.getDistributedTable().getTable()
                ),
                dealerConfig.getConfigName()
            );
        }
    }
}
