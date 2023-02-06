package ru.yandex.autotests.innerpochta.annotations;

import ru.yandex.autotests.innerpochta.conditions.IgnoreCondition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация к тестовым методам, принимает класс, реализующий интерфейс {@link IgnoreCondition},
 * игнорирует тестовый метод, если {@link IgnoreCondition#isSatisfied()} вернёт <code>true</code>,
 * иначе запускает его.
 *
 * @author pavponn
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ConditionalIgnore {
    Class<? extends IgnoreCondition> condition();
}
