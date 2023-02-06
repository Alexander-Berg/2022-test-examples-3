package ru.yandex.direct.jobs.fatconfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.test.context.ContextConfiguration;

/**
 * Аннотация полностью аналогична такой же из основного тест-пака, но она необходима,
 * потому что IDEA рассматривает test classpath как одно целое. Но fat-тесты в действительности
 * собираются и запускаются отдельно. Чтобы не происходило коллизий имён, скопировал аннотацию в другой namespace.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ContextConfiguration(classes = JobsFatTestingConfiguration.class)
public @interface JobsFatTest {
}
