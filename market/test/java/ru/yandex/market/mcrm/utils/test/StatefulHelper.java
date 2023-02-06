package ru.yandex.market.mcrm.utils.test;

/**
 * Утилитарный класс, имеющий состояние которое должно быть сброшено
 * после прохождения теста
 *
 * @author apershukov
 */
public interface StatefulHelper {

    void setUp();

    void tearDown();
}
