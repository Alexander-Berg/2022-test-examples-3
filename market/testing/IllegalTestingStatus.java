package ru.yandex.market.core.testing;

import ru.yandex.market.mbi.exception.CommonException;

/**
 * Операция, связанная изменением статуса магазина  в песочным индексом не может быть выполнена, так как статус магазина
 * в песочном индексе не позволяет этот сделать.
 *
 * @author zoom
 */
public class IllegalTestingStatus extends CommonException {
    public IllegalTestingStatus(String message) {
        super(message);
    }
}
