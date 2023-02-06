package ru.yandex.market.bidding;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.bidding.config.DataAccessConfig;
import ru.yandex.market.core.config.MemCachedTestConfig;
import ru.yandex.market.core.database.EmbeddedPostgresConfig;
import ru.yandex.market.core.history.HistoryService;

import static org.mockito.Mockito.mock;

@Configuration
@Import({
        DataAccessConfig.class,
        MemCachedTestConfig.class,
        EmbeddedPostgresConfig.class
})
@PropertySource("functional-test.properties")
public class FunctionalTestConfig {
    @Bean
    public JdbcTemplate auxiliaryJdbcTemplate(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate;
    }

    @Bean
    public TransactionTemplate auxiliaryTransactionTemplate(TransactionTemplate transactionTemplate) {
        return transactionTemplate;
    }

    @Bean
    public HistoryService historyService() {
        return mock(HistoryService.class);
    }
}
