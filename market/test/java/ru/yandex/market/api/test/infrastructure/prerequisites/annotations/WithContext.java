package ru.yandex.market.api.test.infrastructure.prerequisites.annotations;

import ru.yandex.market.api.test.infrastructure.prerequisites.prerequisites.ContextPrerequisite;

import java.lang.annotation.*;

/**
 * Инициализация контекста приложения
 */
@Documented
@PrerequisiteEvaluation(ContextPrerequisite.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.METHOD})
public @interface WithContext {
}
