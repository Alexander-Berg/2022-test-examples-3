package ru.yandex.market.deepmind.common.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.mboc.common.users.UserRepository;

@Profile("test")
@TestConfiguration
public class TestMbocAdapterConfig extends MbocAdapterConfig {

    public TestMbocAdapterConfig() {
        super(null);
    }

    @Bean
    public UserRepository userRepository() {
        return Mockito.mock(UserRepository.class);
    }
}
