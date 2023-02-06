package ru.yandex.market.wms.receiving.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.wms.common.spring.utils.uuid.FixedListTestUuidGenerator;
import ru.yandex.market.wms.common.spring.utils.uuid.UuidGenerator;

@Configuration
public class TransportRegisterTestConfig {

    @Bean
    @Primary
    public UuidGenerator uuidGenerator() {
        return new FixedListTestUuidGenerator(
                Arrays.asList(
                        "6d809e60-d707-11ea-9550-a9553a7b0571",
                        "6d809e60-d707-11ea-9550-a9553a7b0572",
                        "6d809e60-d707-11ea-9550-a9553a7b0573"
                ));
    }

}
