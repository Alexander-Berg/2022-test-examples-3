package ru.yandex.market.tpl.carrier.driver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.javaframework.tvm.config.TvmSecurityAutoConfiguration;
import ru.yandex.market.starter.tvm.config.TvmClientAutoConfiguration;
import ru.yandex.market.starter.tvm.config.TvmFilterAutoConfiguration;
import ru.yandex.market.starter.tvm.config.TvmPropertiesAutoConfiguration;
import ru.yandex.market.tpl.carrier.core.IntTest;
import ru.yandex.market.tpl.carrier.core.config.CarrierTestConfigurations;
import ru.yandex.market.tpl.carrier.core.config.EmbeddedDataSourceConfiguration;
import ru.yandex.market.tpl.carrier.core.config.MockIntegrationTestsConfig;
import ru.yandex.market.tpl.carrier.driver.config.DriverApiControllerLogTestConfiguration;
import ru.yandex.market.tpl.carrier.driver.config.DriverApiSpringConfiguration;
import ru.yandex.market.tpl.carrier.driver.config.DriverApiWebMvcConfiguration;
import ru.yandex.market.tpl.carrier.driver.config.MockDriverIntegrationTestsConfig;
import ru.yandex.market.tpl.carrier.driver.config.security.DriverApiCommonSecurityConfiguration;
import ru.yandex.market.tpl.carrier.driver.config.security.DriverApiLegacySecurityConfiguration;
import ru.yandex.market.tpl.carrier.driver.config.security.DriverApiTaxiSecurityConfiguration;
import ru.yandex.market.tpl.carrier.driver.config.security.DriverApiYandexMagistralSecurityConfiguration;
import ru.yandex.market.tpl.common.web.config.TplJettyConfiguration;


/**
 * @author ungomma
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)


@IntTest

@SpringBootTest(
        classes = {
                DriverApiSpringConfiguration.class,
                DriverApiLegacySecurityConfiguration.class,
                DriverApiTaxiSecurityConfiguration.class,
                DriverApiCommonSecurityConfiguration.class,
                DriverApiWebMvcConfiguration.class,
                DriverApiYandexMagistralSecurityConfiguration.class,
                TplJettyConfiguration.class,

                CarrierTestConfigurations.Domain.class,
                CarrierTestConfigurations.RegionServiceConfig.class,
                EmbeddedDataSourceConfiguration.class,

                MockIntegrationTestsConfig.class,
                MockIntegrationTestsConfig.DeliveryStaffManagerEmulator.class,
                MockDriverIntegrationTestsConfig.class,

                DriverApiControllerLogTestConfiguration.class,
        }
)
@ImportAutoConfiguration(value = {
        TvmClientAutoConfiguration.class,
        TvmPropertiesAutoConfiguration.class,
        TvmFilterAutoConfiguration.class,
        TvmSecurityAutoConfiguration.class,
})
@PropertySource({
        "classpath:local/10_local-application.properties",
        "classpath:10_application.properties"
})
@AutoConfigureMockMvc(printOnlyOnFailure = false)
public @interface DriverApiIntTest {

}
