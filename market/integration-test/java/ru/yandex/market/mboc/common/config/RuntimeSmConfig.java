package ru.yandex.market.mboc.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.mboc.common.services.smartmatcher.runtime.RuntimeSmartMatcherService;
import ru.yandex.market.mboc.common.services.smartmatcher.runtime.RuntimeSmartMatcherServiceMock;

@Configuration
public class RuntimeSmConfig {
    @Bean
    RuntimeSmartMatcherService runtimeSmartMatcherService() {
        return new RuntimeSmartMatcherServiceMock();
    }
}
