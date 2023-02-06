package ru.yandex.market.wms.common.spring.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.wms.common.spring.utils.uuid.FixedListTestUuidGenerator;
import ru.yandex.market.wms.common.spring.utils.uuid.UuidGenerator;

@TestConfiguration
@Deprecated
public class OrderReplenishmentTestConfig {
    @Bean
    @Primary
    public UuidGenerator getUuidGenerator() {
        return new FixedListTestUuidGenerator(
                Arrays.asList(
                        "6d809e60-d707-11ea-9550-a9553a7b0571",
                        "6d809e60-d707-11ea-9550-a9553a7b0572",
                        "6d809e60-d707-11ea-9550-a9553a7b0573"
                ));
    }

    @Bean
    public FixedListTestUuidGenerator getFixedUuidGenerator() {
        return (FixedListTestUuidGenerator) getUuidGenerator();
    }

    @Bean
    @Primary
    Clock orderReplenishmentClock() {
        return Clock.fixed(Instant.parse("2020-03-17T12:34:56.789Z"), ZoneOffset.UTC);
    }

}
