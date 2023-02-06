package ru.yandex.market.api.server.sec;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.api.AppProperties;
import ru.yandex.market.api.server.sec.oauth.OAuthSecurityConfig;

@Configuration
public class SecurityTestConfig {
    @Inject
    @Bean
    public OAuthSecurityConfig oAuthSecurityConfig(AppProperties appProperties) {
        return OAuthSecurityConfig.of(appProperties);
    }
}
