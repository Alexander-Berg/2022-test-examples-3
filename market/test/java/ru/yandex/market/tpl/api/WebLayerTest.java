package ru.yandex.market.tpl.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.tpl.common.web.config.TplProfiles;

/**
 * @author ungomma
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)
@TestPropertySource({
        "classpath:local/10_local-application.properties",
        "classpath:10_application.properties"
})
@WebMvcTest
@ActiveProfiles(TplProfiles.TESTS)
public @interface WebLayerTest {

    /**
     * Specifies the controllers to test.
     *
     * @return the controllers to test
     */
    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] value() default {};

}
