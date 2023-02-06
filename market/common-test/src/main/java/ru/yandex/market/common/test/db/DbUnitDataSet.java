package ru.yandex.market.common.test.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Описывает поведение DbUnit до и после выполнения теста. Аннотация повторяемая. Для обработки
 * берутся ВСЕ аннотации по иерархии классов и интерфейсов. Множественные атрибуты объединяются,
 * единичные - используется значение из ближайшей к тесту аннотации (метод > класс > родительские классы > интерфейсы).
 *
 * Если вы хотите делать refresh матвьюх, то можете воспользоваться аннотацией {@link DbUnitRefreshMatViews}.
 *
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 * @see DbUnitTestExecutionListener
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
@Repeatable(DbUnitDataSets.class)
public @interface DbUnitDataSet {

    /**
     * @return {@code id} бина в спринге, на котором нужно запускать DbUnit.
     */
    String dataSource() default "dataSource";

    /**
     * @return имя схемы по умолчанию. Применяется ближайшая к тесту аннотация.
     */
    String schema() default "";

    /**
     * @return список наборов данных для применения перед тестом. Значения объединаются по всем аннотациям.
     */
    String[] before() default "";

    /**
     * @return список наборов данных для сравнения после теста. Значения объединаются по всем аннотациям.
     */
    String[] after() default "";

    /**
     * @return формат описания набора данных.
     */
    DataSetType type() default DataSetType.SINGLE_CSV;

    // TODO Надо перенести настройки транкейта в отдельную аннотацию @DbUnitTruncatePolicy

    /**
     * @return нужно ли перед тестом очищать таблицы. Тк по-умолчанию {@code true}, то:
     * <ol>
     *     <li>Упорядочиваем аннотации от базового класса к тестовому методу</li>
     *     <li>Если в цепочке аннотаций встречается {@code false}, то не очищаем,
     *     при условии что дальше (ближе к методу) нету аннотаций
     *     с явно указанными {@link #before()},
     *     или {@link #nonTruncatedTables()},
     *     или {@link #nonRestartedSequences()}
     *     (считаем что автор явно хотел {@code true} в этом случае и не сообщил иного)</li>
     * </ol>
     *
     * Также можно настроить аннотация {@link DbUnitTruncatePolicy} по более точному управлению траннкейтом.
     */
    boolean truncateAllTables() default true;

    /**
     * @return список вида {@code schema.table} таблиц, которые не нужно очищать перед тестом. Значения объединяются по
     * всем указанным аннотациям.
     */
    String[] nonTruncatedTables() default {};

    /**
     * @return список вида {@code schema.sequence} последовательностей, которые не нужно перезапускать перед тестом.
     * Значения объединяются по всем указанным аннотациям.
     */
    String[] nonRestartedSequences() default {};

}
