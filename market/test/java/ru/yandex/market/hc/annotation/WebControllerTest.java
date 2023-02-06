package ru.yandex.market.hc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.hc.config.TestAppConfig;

/**
 * @author: aproskriakov
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@WebMvcTest
@AutoConfigureMockMvc(secure = false)
@ContextConfiguration
@Import(TestAppConfig.class)
@TestPropertySource(properties = {
        "hc.memcached.exptime=90"
})
public @interface WebControllerTest {

    @AliasFor("value")
    Class<?>[] classes() default {};

    @AliasFor(annotation = ContextConfiguration.class, attribute = "classes")
    Class<?>[] value() default {};
}
