package ru.yandex.chemodan.app.djfs.core.db.mongo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.test.context.ActiveProfiles;

/**
 * @author eoshch
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ActiveProfiles(ActivateInMemoryMongo.PROFILE)
public @interface ActivateInMemoryMongo {
    String PROFILE = "in-memory-mongo";
}
