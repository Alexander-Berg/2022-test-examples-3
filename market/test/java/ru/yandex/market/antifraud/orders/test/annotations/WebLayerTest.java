package ru.yandex.market.antifraud.orders.test.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.antifraud.orders.config.AppConfig;
import ru.yandex.market.antifraud.orders.test.config.ControllerTestConfiguration;

/**
 * @author dzvyagin
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@WebMvcTest(properties = {"chaosmonkey.service-name=antifraud", "chaosmonkey.rearr-header-name=X-Market-Rearrfactors"})
@AutoConfigureMockMvc(secure = false)
@ContextConfiguration
@Import({ControllerTestConfiguration.class, AppConfig.ControllerConfiguration.ChaosMonkeyConfig.class})
@ActiveProfiles("test")
public @interface WebLayerTest {

    @AliasFor("value")
    Class<?>[] classes() default {};


    @AliasFor(annotation = ContextConfiguration.class, attribute = "classes")
    Class<?>[] value() default {};
}
