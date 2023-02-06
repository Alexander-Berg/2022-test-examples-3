package ru.yandex.market.tpl.carrier.tms;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.tpl.carrier.core.IntTest;
import ru.yandex.market.tpl.carrier.core.config.CarrierTestConfigurations;
import ru.yandex.market.tpl.carrier.core.config.EmbeddedDataSourceConfiguration;
import ru.yandex.market.tpl.carrier.core.config.MockIntegrationTestsConfig;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.tms.config.TmsInternalConfiguration;
import ru.yandex.market.tpl.carrier.tms.config.TmsMockConfiguration;
import ru.yandex.market.tpl.carrier.tms.service.TelegramConfig;
import ru.yandex.market.tpl.common.logbroker.config.LogbrokerTestExternalConfig;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)

@IntTest
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {
                TmsInternalConfiguration.class,

                CarrierTestConfigurations.Domain.class,
                CarrierTestConfigurations.RegionServiceConfig.class,
                EmbeddedDataSourceConfiguration.class,
                TmsMockConfiguration.class,
                TelegramConfig.class,
                LogbrokerTestExternalConfig.class,
                MockIntegrationTestsConfig.class,
                MockIntegrationTestsConfig.TvmTestConfiguration.class,
                MockIntegrationTestsConfig.CarrierRegionServiceTestConfiguration.class,
                MockIntegrationTestsConfig.DeliveryStaffManagerEmulator.class
        },
        properties = { "spring.main.allow-bean-definition-overriding=true" })
@PropertySource({
        "classpath:local/10_local-application.properties",
        "classpath:10_application.properties"
})
@Import(TestUserHelper.class)
public @interface TmsIntTest {
}
