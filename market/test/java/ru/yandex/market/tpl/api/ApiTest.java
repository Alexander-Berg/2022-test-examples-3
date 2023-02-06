package ru.yandex.market.tpl.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.tpl.api.config.TplApiSpringConfiguration;
import ru.yandex.market.tpl.core.CleanupAfterEachEmbeddedDbExtension;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.config.EmbeddedDataSourceConfiguration;
import ru.yandex.market.tpl.core.config.MockIntegrationTestsConfig;
import ru.yandex.market.tpl.core.config.TplRoutingTestConfigurations;
import ru.yandex.market.tpl.core.config.TplTestConfigurations;

/**
 * @author ungomma
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)

@CoreTest
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {
                TplApiSpringConfiguration.class,
                TplTestConfigurations.Core.class,
                TplRoutingTestConfigurations.class,
                EmbeddedDataSourceConfiguration.class,
                MockIntegrationTestsConfig.class
        })
@ExtendWith(CleanupAfterEachEmbeddedDbExtension.class)
@ContextConfiguration(classes = EmbeddedDataSourceConfiguration.class)
@PropertySource({
        "classpath:local/10_local-application.properties",
        "classpath:10_application.properties"
})
public @interface ApiTest {

}
