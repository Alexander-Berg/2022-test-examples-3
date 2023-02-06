package ru.yandex.market.jmf.db.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <b>Work in progress! You shouldn't use it</b><p/>
 *
 * Позволяет откатывать базу данных к состоянию на момент "после запуска контекста" без перезапуска самого
 * контекста. Точная настройка момента срабатывания отката/очистки осуществляется параметрами
 * {@link #methodMode()} и {@link #classMode()}.<p/>
 *
 * Моменты срабатывания очистки:
 * <ul>
 *     <li>AFTER - после класса/метода</li>
 *     <li>AROUND - до и после класса/метода</li>
 *     <li>BEFORE - до класса/метода</li>
 *     <li>NONE - не вызывать очистку. Пример: аннотация установлена над классом, но очистка требуется только
 *     после класса, тогда аннотации добавляем параметр {@code @CleanDb(methodMode = CleanDb.Mode.NONE)} </li>
 * </ul><p/>
 *
 * @apiNote <b>Важно!</b> Аннотация метода имеет приоритет над аннотацией класса при определении точек срабатывания
 * вокруг метода.
 * @see TestDbCleaner
 * @see CleanDbTestExecutionListener
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface CleanDb {

    /**
     * Определяет режим отката/очистки базы для методов
     */
    Mode methodMode() default Mode.BEFORE;

    /**
     * Определяет режим отката/очистки базы для класса. Не имеет смысла при аннотировании метода
     */
    Mode classMode() default Mode.AFTER;

    enum Mode {
        AFTER, AROUND, BEFORE, NONE
    }
}
