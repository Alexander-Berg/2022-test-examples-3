package ru.yandex.market.tpl.partner.carrier;

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
import ru.yandex.market.tpl.carrier.core.config.CarrierCoreConfiguration;
import ru.yandex.market.tpl.carrier.core.config.CarrierTestConfigurations;
import ru.yandex.market.tpl.carrier.core.config.EmbeddedDataSourceConfiguration;
import ru.yandex.market.tpl.carrier.core.config.MockIntegrationTestsConfig;
import ru.yandex.market.tpl.partner.carrier.config.MockPartnerCarrierTestsConfig;
import ru.yandex.market.tpl.partner.carrier.config.TplPartnerCarrierControllerLogTestConfiguration;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited

@IntTest
@SpringBootTest(classes = TplPartnerCarrier.class)
@Import({
        MockIntegrationTestsConfig.class,
        MockIntegrationTestsConfig.DeliveryStaffManagerEmulator.class,

        EmbeddedDataSourceConfiguration.class,

        CarrierCoreConfiguration.class,
        CarrierTestConfigurations.Domain.class,
        CarrierTestConfigurations.RegionServiceConfig.class,
        MockPartnerCarrierTestsConfig.class,

        TplPartnerCarrierControllerLogTestConfiguration.class,
})
@PropertySource(value = {
        "classpath:10_application.properties",
        "classpath:10_test-application.properties",
})
@AutoConfigureMockMvc(secure = false)
public @interface TplPartnerCarrierWebIntTest {
}
