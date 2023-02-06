package ru.yandex.market.sc.core.test;

/**
 * Класс предназначен для ошибок возникших из-за не корректных параметров тестов
 */
public class InvalidTestParameters extends AssertionError {
    public InvalidTestParameters(Object detailMessage) {
        super(detailMessage);
    }
}
