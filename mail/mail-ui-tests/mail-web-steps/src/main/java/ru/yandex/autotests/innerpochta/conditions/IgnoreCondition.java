package ru.yandex.autotests.innerpochta.conditions;

import org.junit.runner.Description;

/**
 * Интерфейс классов, которые можно указывать
 * в аннотации {@link ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore}.
 *
 * @author pavponn
 */
public interface IgnoreCondition {

    boolean isSatisfied();

    String getMessage();

    default void setFields(Description description) {
    }
}
