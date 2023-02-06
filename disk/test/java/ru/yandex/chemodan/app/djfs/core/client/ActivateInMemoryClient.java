package ru.yandex.chemodan.app.djfs.core.client;

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
@ActiveProfiles(ActivateInMemoryClient.PROFILE)
public @interface ActivateInMemoryClient {
    String PROFILE = "in-memory-client";
}
