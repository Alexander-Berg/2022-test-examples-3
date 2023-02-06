package ru.yandex.market.wrap.infor.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.wrap.infor.service.stocks.now.NowTimeService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Configuration
public class DateTimeTestConfiguration {

    @Bean
    public Clock getClock() {

        ZoneId zoneId = ZoneId.of("Europe/Moscow");
        return Clock.fixed(LocalDateTime.of(2018, 1, 1, 0, 0)
            .atZone(zoneId).toInstant(), zoneId);
    }

    @Bean
    public NowTimeService nowTimeService() {
        return () -> DateTime.fromLocalDateTime(LocalDateTime.now(getClock()));
    }
}
