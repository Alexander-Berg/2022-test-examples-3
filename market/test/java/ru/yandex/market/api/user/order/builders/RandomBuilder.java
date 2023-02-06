package ru.yandex.market.api.user.order.builders;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public abstract class RandomBuilder<T> {

    protected final RandomSource random = new RandomSource(0);

    /**
     * Задает случайные значения полям
     */
    public abstract RandomBuilder<T> random();

    /**
     * Возвращает объект
     */
    public abstract T build();
}