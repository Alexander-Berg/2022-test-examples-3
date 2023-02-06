package ru.yandex.market.api.test.infrastructure.prerequisites.annotations;

import ru.yandex.market.api.test.infrastructure.prerequisites.prerequisites.MocksPrerequisite;

import java.lang.annotation.*;

/**
 * Инициализация Mock сервисов
 */
@Documented
@PrerequisiteEvaluation(MocksPrerequisite.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.METHOD})
public @interface WithMocks {
}
