package ru.yandex.market.logistics.lrm.config;

import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistics.lrm.config.locals.UuidGenerator;

@Configuration
public class LocalsConfiguration {
    public static final String TEST_UUID = "e11c5e64-3694-40c9-b9b4-126efedaa098";
    public static final String TEST_UUID2 = "e11c5e64-3694-40c9-b9b4-126efedaa099";

    @Bean
    public TestableClock clock() {
        return new TestableClock();
    }

    @Bean
    @SuppressWarnings("Convert2Lambda")
    public UuidGenerator uuidGenerator() {
        return new UuidGenerator() {
            @Override
            public UUID get() {
                return UUID.fromString(TEST_UUID);
            }
        };
    }
}
