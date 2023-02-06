package ru.yandex.market.gutgin.tms.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Import({
    TestServiceConfig.class,
    TestPipelineConfig.class,
    CommonDaoConfig.class
})
public class TestConfig {
    @Bean
    public JdbcTemplate yqlJdbcTemplate() {
        return Mockito.mock(JdbcTemplate.class);
    }
}

