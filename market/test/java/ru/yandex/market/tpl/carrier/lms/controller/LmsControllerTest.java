package ru.yandex.market.tpl.carrier.lms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.tpl.carrier.core.IntTest;
import ru.yandex.market.tpl.carrier.core.config.CarrierTestConfigurations;
import ru.yandex.market.tpl.carrier.core.config.EmbeddedDataSourceConfiguration;
import ru.yandex.market.tpl.carrier.core.config.MockIntegrationTestsConfig;
import ru.yandex.market.tpl.carrier.lms.CarrierLms;
import ru.yandex.market.tpl.carrier.lms.config.PlannerLmsMockConfiguration;


@IntTest
@SpringBootTest(classes = {
        CarrierLms.class,

        CarrierTestConfigurations.Domain.class,
        CarrierTestConfigurations.RegionServiceConfig.class,

        EmbeddedDataSourceConfiguration.class,
        PlannerLmsMockConfiguration.class,
        MockIntegrationTestsConfig.class,
        MockIntegrationTestsConfig.TvmTestConfiguration.class,
        MockIntegrationTestsConfig.CarrierRegionServiceTestConfiguration.class,
        MockIntegrationTestsConfig.DeliveryStaffManagerEmulator.class
})
@PropertySource({
        "classpath:local/10_local-lms.properties",
        "classpath:00_application.properties"
})
// TODO: disable security in test?
@AutoConfigureMockMvc(secure = false)
public abstract class LmsControllerTest {
    @Autowired
    protected MockMvc mockMvc;
}
