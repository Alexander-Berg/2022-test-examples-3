package ru.yandex.market.common.test.transformer;

/**
 * Преобразует один sql-запрос в другой.
 *
 * @author zoom
 */
@FunctionalInterface
public interface StringTransformer {
    String transform(String string);
}
