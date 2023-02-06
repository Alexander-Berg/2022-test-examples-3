package ru.yandex.market.core.testing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.core.history.EntityFinder;
import ru.yandex.market.core.history.HistoryService;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Vadim Lyalin
 */
@Configuration
public class TestingStatusDaoConfig {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Bean
    public DefaultTestingStatusDao testingStatusDao(HistoryService historyService) {
        return new DefaultTestingStatusDao(jdbcTemplate, historyService);
    }

    @Bean
    public HistoryService historyService() {
        HistoryService.Record.Builder builder = mock(HistoryService.Record.Builder.class);
        HistoryService historyService = mock(HistoryService.class);
        when(historyService.buildCreateRecord(any(EntityFinder.class))).thenReturn(builder);
        when(historyService.buildDeleteRecord(any(EntityFinder.class))).thenReturn(builder);
        when(historyService.buildUpdateRecord(any(EntityFinder.class))).thenReturn(builder);
        return historyService;
    }
}
