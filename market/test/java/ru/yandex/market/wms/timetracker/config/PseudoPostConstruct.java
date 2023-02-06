package ru.yandex.market.wms.timetracker.config;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Короче, говнотесты хреново работают с PostConstruct (там вызов инциализации бд идет после PostConstruct,
 * ибо спринг таким порядком хэндлит своими постпроцессорами).
 * <p/>
 * Поэтому я принял волевое и болезненное решение выключить PostConstruct в тестах.
 * <p/>
 * Хочешь протестить? Пиши руками вызов, братан/сестра/whoever.
 * <p/>
 * Ну, либо найди более корректное решение, но обязательно сообщи мне, как ты это решил, мне ж интересно.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface PseudoPostConstruct {
}
