package ru.yandex.chemodan.app.notifier.admin.dao.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * @author vpronto
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(NotificationEmbeddedPgConfiguration.class)
public @interface ImportNotificationEmbeddedPg { }
