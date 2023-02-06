package ru.yandex.market.tpl.internal.controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.tpl.common.web.config.TplJettyConfiguration;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.config.EmbeddedDataSourceConfiguration;
import ru.yandex.market.tpl.core.config.MockIntegrationTestsConfig;
import ru.yandex.market.tpl.core.config.TplCoreConfiguration;
import ru.yandex.market.tpl.core.config.TplRoutingTestConfigurations;
import ru.yandex.market.tpl.core.config.TplTestConfigurations;
import ru.yandex.market.tpl.internal.config.TplIntSecurityConfiguration;
import ru.yandex.market.tpl.internal.config.TplIntSpringConfiguration;
import ru.yandex.market.tpl.internal.config.TplIntWebMvcConfiguration;
import ru.yandex.market.tpl.internal.config.TplTestIntWebConfiguration;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited

@CoreTest
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = {
                TplIntSpringConfiguration.class,
                TplIntSecurityConfiguration.class,
                TplIntWebMvcConfiguration.class,

                TplJettyConfiguration.class,

                MockIntegrationTestsConfig.class,
                MockIntegrationTestsConfig.TvmTestConfiguration.class,

                EmbeddedDataSourceConfiguration.class,

                TplCoreConfiguration.class,
                TplTestConfigurations.Core.class,
                TplRoutingTestConfigurations.class,
                TplTestIntWebConfiguration.class
        }
)
@PropertySource({
        "classpath:local/10_local-application.properties",
        "classpath:10_application.properties"
})
@AutoConfigureMockMvc
public @interface TplIntWebTest {
}
