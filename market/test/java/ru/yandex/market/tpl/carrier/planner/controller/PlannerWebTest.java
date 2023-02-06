package ru.yandex.market.tpl.carrier.planner.controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.tpl.carrier.core.IntTest;
import ru.yandex.market.tpl.carrier.core.config.CarrierTestConfigurations;
import ru.yandex.market.tpl.carrier.core.config.EmbeddedDataSourceConfiguration;
import ru.yandex.market.tpl.carrier.core.config.MockIntegrationTestsConfig;
import ru.yandex.market.tpl.carrier.planner.MarketCarrierPlanner;
import ru.yandex.market.tpl.carrier.planner.config.PlannerControllerLogTestConfiguration;
import ru.yandex.market.tpl.carrier.planner.config.PlannerMockConfiguration;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited

@IntTest
@SpringBootTest(classes = MarketCarrierPlanner.class)

@Import({
        MockIntegrationTestsConfig.class,
        MockIntegrationTestsConfig.TvmTestConfiguration.class,
        MockIntegrationTestsConfig.CarrierRegionServiceTestConfiguration.class,
        MockIntegrationTestsConfig.DeliveryStaffManagerEmulator.class,

        PlannerMockConfiguration.class,

        EmbeddedDataSourceConfiguration.class,

        CarrierTestConfigurations.Domain.class,
        CarrierTestConfigurations.RegionServiceConfig.class,

        PlannerControllerLogTestConfiguration.class
})
@PropertySource({
        "classpath:local/10_local-application.properties",
        "classpath:10_application.properties"
})
@AutoConfigureMockMvc
public @interface PlannerWebTest {
}
