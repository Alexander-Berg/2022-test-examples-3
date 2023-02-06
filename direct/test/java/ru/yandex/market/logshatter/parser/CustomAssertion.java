package ru.yandex.market.logshatter.parser;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 22.08.16
 */
public interface CustomAssertion {
    void doAssertion(Object[] expected, Object[] actual);
}
