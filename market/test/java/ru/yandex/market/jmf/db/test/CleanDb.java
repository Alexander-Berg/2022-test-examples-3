package ru.yandex.market.jmf.db.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ru.yandex.market.jmf.db.test.impl.CleanDbTestExecutionListener;
import ru.yandex.market.jmf.db.test.impl.TestDbCleaner;

/**
 * <b>Work in progress! You shouldn't use it</b><p/>
 * <p>
 * Позволяет откатывать базу данных к состоянию на момент "после запуска контекста" без перезапуска самого
 * контекста. Точная настройка момента срабатывания отката/очистки осуществляется параметрами
 * {@link #methodMode()} и {@link #classMode()}.<p/>
 * <p>
 * Моменты срабатывания очистки:
 * <ul>
 *     <li>{@link ClassMode#AFTER_ALL} и отсутствие аннотации на методах - после прохождения всех тестов</li>
 *     <li>{@link ClassMode#AFTER_ALL} и наличие на методах {@link MethodMode#AFTER} - после прохождения всех
 *     тестов и после каждого аннотированного теста</li>
 *     <li>{@link ClassMode#AFTER_ALL} и наличие на методах {@link MethodMode#DEFAULT} либо {@link MethodMode#NONE}
 *     - после прохождения всех тестов</li>
 *     <li>{@link ClassMode#AFTER_EACH_METHOD} и отсутствие аннотации на методах - после каждого теста</li>
 *     <li>{@link ClassMode#AFTER_EACH_METHOD} и наличие на методах {@link MethodMode#AFTER} либо
 *     {@link MethodMode#DEFAULT} - после каждого теста</li>
 *     <li>{@link ClassMode#AFTER_EACH_METHOD} и наличие на методах {@link MethodMode#NONE} - после каждого теста,
 *     кроме аннотированных</li>
 * </ul><p/>
 *
 * @see TestDbCleaner
 * @see CleanDbTestExecutionListener
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface CleanDb {

    /**
     * Определяет режим отката/очистки базы для методов
     */
    MethodMode methodMode() default MethodMode.DEFAULT;

    /**
     * Определяет режим отката/очистки базы для класса
     */
    ClassMode classMode() default ClassMode.AFTER_EACH_METHOD;

    enum ClassMode {
        AFTER_EACH_METHOD, AFTER_ALL
    }

    enum MethodMode {
        AFTER, DEFAULT, NONE
    }
}
