package ru.yandex.market.antifraud.orders.test.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import ru.yandex.market.antifraud.orders.config.DatasourceConfiguration;
import ru.yandex.market.antifraud.orders.config.LiquibaseConfiguration;
import ru.yandex.market.antifraud.orders.storage.dao.MarketUserIdDao;

/**
 * @author dzvyagin
 */
@Configuration
@Import({
        EmbeddedPgConfig.class,
        DatasourceConfiguration.class,
        LiquibaseConfiguration.class
})
public class DaoTestConfiguration {

    @Bean
    public MarketUserIdDao marketUserIdDao(NamedParameterJdbcOperations jdbcTemplate) {
        return new MarketUserIdDao(jdbcTemplate);
    }
}
