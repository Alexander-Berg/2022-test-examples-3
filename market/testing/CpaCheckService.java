package ru.yandex.market.core.testing;

/**
 * Работа с тестовым индексом для тестирования CPA.
 *
 * @author zoom
 */
public interface CpaCheckService {

    /**
     * Вычисляет тип CPA-тестирование, которое надо запустить.
     */
    TestingType getTestingType(long shopId);

    /**
     * Магазин может пройти CPA-проверку, если захочет.
     */
    boolean isCheckAllowed(long shopId);

    /**
     * Может ли магазин запустить самопроверку.
     */
    boolean isSelfCheckCanBeStarted(long shopId);
}
