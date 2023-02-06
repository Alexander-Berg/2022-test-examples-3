package ru.yandex.market.tpl.internal.controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.config.EmbeddedDataSourceConfiguration;
import ru.yandex.market.tpl.core.config.MockIntegrationTestsConfig;
import ru.yandex.market.tpl.core.config.TplRoutingTestConfigurations;
import ru.yandex.market.tpl.core.config.TplTestConfigurations;
import ru.yandex.market.tpl.internal.config.TplIntSpringConfiguration;

/**
 * deprecated
 */
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)

@CoreTest
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {
                TplIntSpringConfiguration.class,
                TplTestConfigurations.Core.class,
                TplRoutingTestConfigurations.class,
                EmbeddedDataSourceConfiguration.class,
                MockIntegrationTestsConfig.class,
                MockIntegrationTestsConfig.TvmTestConfiguration.class
        })
@ContextConfiguration(classes = EmbeddedDataSourceConfiguration.class)
@PropertySource({
        "classpath:local/10_local-application.properties",
        "classpath:10_application.properties"
})
public @interface TplIntTest {

}
