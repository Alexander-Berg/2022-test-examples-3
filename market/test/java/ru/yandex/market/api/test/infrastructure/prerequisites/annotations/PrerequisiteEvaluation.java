package ru.yandex.market.api.test.infrastructure.prerequisites.annotations;

import ru.yandex.market.api.test.infrastructure.prerequisites.prerequisites.TestPrerequisite;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PrerequisiteEvaluation {

    /**
     * Conditional checker class that will be instantiated and run before test.
     *
     * @return conditional checker class
     */
    @SuppressWarnings("rawtypes")
    Class<? extends TestPrerequisite> value();
}
