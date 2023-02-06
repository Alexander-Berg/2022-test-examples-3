package ru.yandex.market.adv.data.sync.config;

import javax.sql.DataSource;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.adv.service.time.TimeService;
import ru.yandex.market.adv.test.service.time.TestTimeService;

@Configuration
@RequiredArgsConstructor
public class DataSyncConfigTest {

    @Primary
    @Bean
    public TimeService timeService() {
        return new TestTimeService();
    }

    @Bean
    public NamedParameterJdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
