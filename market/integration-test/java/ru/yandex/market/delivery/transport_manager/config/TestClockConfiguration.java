package ru.yandex.market.delivery.transport_manager.config;

import java.time.ZoneOffset;
import java.util.TimeZone;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.delivery.transport_manager.util.Profiles;

@Configuration
public class TestClockConfiguration {
    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    private void afterPropertiesSet() {
        objectMapper.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
    }

    @Bean("clock")
    @Profile(Profiles.INTEGRATION_TEST)
    public TestableClock clock() {
        return new TestableClock();
    }
}
