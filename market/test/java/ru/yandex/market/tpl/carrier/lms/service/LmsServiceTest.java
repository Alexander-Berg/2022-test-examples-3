package ru.yandex.market.tpl.carrier.lms.service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.tpl.carrier.core.IntTest;
import ru.yandex.market.tpl.carrier.core.config.CarrierCoreConfiguration;
import ru.yandex.market.tpl.carrier.core.config.CarrierTestConfigurations;
import ru.yandex.market.tpl.carrier.core.config.EmbeddedDataSourceConfiguration;
import ru.yandex.market.tpl.carrier.core.config.MockIntegrationTestsConfig;
import ru.yandex.market.tpl.carrier.lms.config.CarrierLmsInternalConfiguration;
import ru.yandex.market.tpl.carrier.lms.config.PlannerLmsMockConfiguration;

@IntTest
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {
                CarrierCoreConfiguration.class,
                CarrierTestConfigurations.Domain.class,
                CarrierTestConfigurations.RegionServiceConfig.class,
                CarrierLmsInternalConfiguration.class,
                EmbeddedDataSourceConfiguration.class,
                PlannerLmsMockConfiguration.class,
                MockIntegrationTestsConfig.class,
                MockIntegrationTestsConfig.TvmTestConfiguration.class,
                MockIntegrationTestsConfig.CarrierRegionServiceTestConfiguration.class
        }
)
@PropertySource({
        "classpath:local/10_local-lms.properties",
        "classpath:00_application.properties"
})
public abstract class LmsServiceTest {
}
