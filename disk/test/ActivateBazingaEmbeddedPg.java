package ru.yandex.chemodan.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.test.context.ActiveProfiles;

import static ru.yandex.chemodan.test.ActivateBazingaEmbeddedPg.BAZINGA_EMBEDDED_PG;
import static ru.yandex.misc.db.embedded.ActivateEmbeddedPg.EMBEDDED_PG;

/**
 * @author vpronto
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ActiveProfiles({BAZINGA_EMBEDDED_PG, EMBEDDED_PG})
public @interface ActivateBazingaEmbeddedPg {
    String BAZINGA_EMBEDDED_PG = "bazinga-embedded-pg";
}
