package ru.yandex.market.ff4shops.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import ru.yandex.common.util.date.TestableClock;

@Lazy
@Configuration
public class UtilsConfig {

    @Bean
    public TestUrlBuilder testUrlBuilder(@Value("${local.server.port}") int localPort) {
        return new TestUrlBuilder(localPort);
    }

    @Bean
    public TestableClock clock() {
        return new TestableClock();
    }

}
