package ru.yandex.market.api.run.runner;

/**
 * Маркерный интерфейс для тестов запускающих тесты в заданном случайном порядке
 * Все классы, реализующие этот интерфейс, должны иметь конструктор вида:
 *
 * public SomeRandomTestRunner(Random random, Class<?> testClass) {
 *
 * }
 *
 * @author dimkarp93
 */
public interface RandomRunner {
}
