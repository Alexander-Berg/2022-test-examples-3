package ru.yandex.market.tpl.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.api.confg.MockApiIntegrationTestsConfig;
import ru.yandex.market.tpl.api.config.TplApiSecurityConfiguration;
import ru.yandex.market.tpl.api.config.TplApiSpringConfiguration;
import ru.yandex.market.tpl.api.config.TplWebMvcConfiguration;
import ru.yandex.market.tpl.common.web.config.TplJettyConfiguration;
import ru.yandex.market.tpl.common.web.config.TplProfiles;
import ru.yandex.market.tpl.core.CleanupAfterAllEmbeddedDbExtension;
import ru.yandex.market.tpl.core.CleanupCachesAfterEachExtension;
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

@SpringBootTest(
        classes = {
                TplApiSpringConfiguration.class,
                TplApiSecurityConfiguration.class,
                TplJettyConfiguration.class,
                TplWebMvcConfiguration.class,

                TplTestConfigurations.Core.class,
                TplRoutingTestConfigurations.class,

                EmbeddedDataSourceConfiguration.class,

                MockIntegrationTestsConfig.class,
                MockApiIntegrationTestsConfig.class,
        }
)
@PropertySource({
        "classpath:local/10_local-application.properties",
        "classpath:10_application.properties"
})
@ImportAutoConfiguration({HibernateJpaAutoConfiguration.class, ValidationAutoConfiguration.class})
@ActiveProfiles({
        TplProfiles.TESTS, TplProfiles.TESTS_EMBEDDED_DB
})
@Transactional
@ExtendWith({CleanupAfterAllEmbeddedDbExtension.class, CleanupCachesAfterEachExtension.class})
@AutoConfigureMockMvc(printOnlyOnFailure = false)
public @interface ApiIntTest {

}
