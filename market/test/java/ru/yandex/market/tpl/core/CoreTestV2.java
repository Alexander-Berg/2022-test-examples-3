package ru.yandex.market.tpl.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;

import ru.yandex.market.tpl.common.web.config.TplProfiles;
import ru.yandex.market.tpl.core.config.EmbeddedDataSourceConfiguration;
import ru.yandex.market.tpl.core.config.MockIntegrationTestsConfig;
import ru.yandex.market.tpl.core.config.TplCoreConfiguration;
import ru.yandex.market.tpl.core.config.TplRegionActualizationConfiguration;
import ru.yandex.market.tpl.core.config.TplRoutingTestConfigurations;
import ru.yandex.market.tpl.core.config.TplTestConfigurations;

/**
 * @author ungomma
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {
                TplRegionActualizationConfiguration.class,
                TplCoreConfiguration.class,
                TplTestConfigurations.Core.class,
                TplRoutingTestConfigurations.class,
                EmbeddedDataSourceConfiguration.class,
                MockIntegrationTestsConfig.class
        })
@ImportAutoConfiguration({HibernateJpaAutoConfiguration.class, ValidationAutoConfiguration.class})
@PropertySource({
        "classpath:local/10_local-application.properties",
        "classpath:10_application.properties"
})
@ActiveProfiles({TplProfiles.TESTS, TplProfiles.TESTS_EMBEDDED_DB})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
public @interface CoreTestV2 {

}
