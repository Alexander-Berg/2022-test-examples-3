package ru.yandex.market.tpl.carrier.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.tpl.common.web.config.TplProfiles;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)

@ImportAutoConfiguration({HibernateJpaAutoConfiguration.class, ValidationAutoConfiguration.class})

@ExtendWith(CleanupAfterEachEmbeddedDbExtension.class)
@ExtendWith(CleanupAfterEachStatefulMockExtension.class)
@ActiveProfiles({TplProfiles.TESTS, TplProfiles.TESTS_EMBEDDED_DB})
public @interface IntTest {
}
