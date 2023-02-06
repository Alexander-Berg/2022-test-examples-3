package ru.yandex.market.fintech.creditbroker;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fintech.creditbroker.helper.FakeTvmClient;
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;
import ru.yandex.passport.tvmauth.TvmClient;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
        SpringApplicationConfig.class
    }
)
@TestPropertySource("classpath:functionalTest.properties")
public abstract class AbstractFunctionalTest {
    @TestConfiguration
    public static class TestBaseConfig {
        @Bean
        public TvmClient tvmClient() {
            return new FakeTvmClient();
        }
    }
}

