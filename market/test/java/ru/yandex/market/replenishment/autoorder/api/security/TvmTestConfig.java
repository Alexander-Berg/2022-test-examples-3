package ru.yandex.market.replenishment.autoorder.api.security;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.replenishment.autoorder.repository.postgres.EnvironmentRepository;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Configuration
@Profile("tvm-testing")
class TvmTestConfig {
    @Bean
    @Primary
    public EnvironmentRepository getEnvironmentRepository() {
        EnvironmentRepository environmentRepository = Mockito.mock(EnvironmentRepository.class);
        when(environmentRepository.findById(anyString()))
            .thenThrow(new IllegalStateException("Security must not to use database"));
        return environmentRepository;
    }
}
