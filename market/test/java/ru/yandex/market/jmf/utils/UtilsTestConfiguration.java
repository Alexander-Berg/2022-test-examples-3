package ru.yandex.market.jmf.utils;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.jmf.common.security.SecurityEventsLogService;
import ru.yandex.market.jmf.utils.html.SafeUrlService;

@Configuration
@Import(UtilsConfiguration.class)
@PropertySource(name = "testUtilsConfiguration", value = "classpath:utils/test/utils-test.properties")
@ComponentScan("ru.yandex.market.jmf.utils.test.impl")
public class UtilsTestConfiguration {
    @Bean
    public SecurityEventsLogService mockSecurityEventsLogService() {
        return Mockito.mock(SecurityEventsLogService.class);
    }

    @Bean
    public SafeUrlService echoSafeUrlService() {
        return s -> s;
    }
}
