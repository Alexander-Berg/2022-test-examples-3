package ru.yandex.market.tpl.carrier.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.tpl.carrier.core.config.CarrierCoreConfiguration;
import ru.yandex.market.tpl.carrier.core.config.CarrierTestConfigurations;
import ru.yandex.market.tpl.carrier.core.config.DummyDbQueueProcessingServiceConfiguration;
import ru.yandex.market.tpl.carrier.core.config.EmbeddedDataSourceConfiguration;
import ru.yandex.market.tpl.carrier.core.config.MockIntegrationTestsConfig;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {
                CarrierCoreConfiguration.class,
                CarrierTestConfigurations.Domain.class,
                DummyDbQueueProcessingServiceConfiguration.class,
                CarrierTestConfigurations.RegionServiceConfig.class,
                EmbeddedDataSourceConfiguration.class,
                MockIntegrationTestsConfig.class,
                MockIntegrationTestsConfig.DeliveryStaffManagerEmulator.class
        })
@PropertySource({
        "classpath:test-application.properties"
})
@IntTest
public @interface CoreTestV2 {

}
