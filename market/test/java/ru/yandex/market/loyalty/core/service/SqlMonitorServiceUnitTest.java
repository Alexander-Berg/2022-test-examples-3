package ru.yandex.market.loyalty.core.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@RunWith(SpringRunner.class)
@ContextConfiguration
public class SqlMonitorServiceUnitTest {

    @Autowired
    SqlMonitorService sqlMonitorService;
    @Autowired
    JdbcTemplate jdbcTemplateMock;


    @Test
    public void shouldExecuteAllMonitoringQueries() {
        sqlMonitorService.checkDbState();
        Arrays.stream(MonitoringSqlQuery.values())
                .forEach(monitoringSqlQuery -> assertMonitoringQueryExecuted(monitoringSqlQuery.getSql()));
    }

    @SuppressWarnings("unchecked")
    private void totalQueriesExecutedCount(int expectedInvocationCount) {
        verify(jdbcTemplateMock, times(expectedInvocationCount))
                .query(anyString(), any(RowMapper.class));
    }

    @SuppressWarnings("unchecked")
    private void assertMonitoringQueryExecuted(String monitoringQuery) {
        assertNotNull(monitoringQuery);
        verify(jdbcTemplateMock, atLeastOnce())
                .query(eq(monitoringQuery), any(RowMapper.class));
    }

    @Configuration
    public static class Config {

        @Bean
        JdbcTemplate jdbcTemplate() {
            return mock(JdbcTemplate.class);
        }

        @Bean
        SqlMonitorService sqlMonitorService(JdbcTemplate jdbcTemplate) {
            return new SqlMonitorService(jdbcTemplate);
        }

    }
}
