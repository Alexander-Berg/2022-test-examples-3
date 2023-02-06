package ru.yandex.market.wms.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import({
        IntegrationTestBackendConfiguration.class
})
@Configuration
public class IntegrationTestFrontendConfiguration {
}
