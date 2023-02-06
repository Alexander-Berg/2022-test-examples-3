package ru.yandex.market.antifraud.orders.test.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.antifraud.orders.config.DatasourceConfiguration;
import ru.yandex.market.antifraud.orders.config.LiquibaseConfiguration;
import ru.yandex.market.antifraud.orders.detector.OrderFraudDetector;
import ru.yandex.market.common.zk.ZooClient;

import static org.mockito.Mockito.mock;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 20.05.19
 */
@Configuration
@Import({EmbeddedPgConfig.class, DatasourceConfiguration.class, LiquibaseConfiguration.class})
@ComponentScan(basePackages = {
        "ru.yandex.market.antifraud.orders.storage.dao",
        "ru.yandex.market.antifraud.orders.service"
})
public class ControllerTestConfig {


    @Bean
    public ZooClient zooClient() {
        return mock(ZooClient.class);
    }

    @Bean
    public OrderFraudDetector detector() {
        return mock(OrderFraudDetector.class);
    }

    @Bean
    public NamedParameterJdbcTemplate checkouterJdbcTemplate() {
        return mock(NamedParameterJdbcTemplate.class);
    }

    @Bean
    public String zooNamespace() {
        return "";
    }

}
