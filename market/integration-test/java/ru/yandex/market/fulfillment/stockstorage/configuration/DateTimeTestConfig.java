package ru.yandex.market.fulfillment.stockstorage.configuration;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.fulfillment.stockstorage.util.JdbcClock;

import static ru.yandex.market.fulfillment.stockstorage.config.DateTimeConfig.JDBC_CLOCK;

@Configuration
public class DateTimeTestConfig {
    public static final String REAL_JDBC_CLOCK = "realJdbcClock";

    @Bean
    @Primary
    public Clock getClock() {
        ZoneId zoneId = ZoneId.of("Europe/Moscow");
        return Clock.fixed(LocalDateTime.of(2018, 1, 1, 0, 0)
                .atZone(zoneId).toInstant(), zoneId);
    }

    @Bean
    @Qualifier(JDBC_CLOCK)
    public Clock getJdbcClock() {
        return Clock.fixed(Instant.parse("2018-01-01T12:34:56.789Z"), ZoneOffset.UTC);
    }

    @Bean
    @Qualifier(REAL_JDBC_CLOCK)
    public Clock getRealJdbcClock(NamedParameterJdbcTemplate jdbcTemplate) {
        return JdbcClock.atUTC(jdbcTemplate);
    }
}
