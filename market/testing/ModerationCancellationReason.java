package ru.yandex.market.core.testing;

/**
 * Причина отмены проверки магазина.
 */
public enum ModerationCancellationReason {

    /**
     * Ошибка в фиде.
     */
    FEED_ERROR,
    /**
     * Отключение программы.
     */
    PROGRAM_DISABLE
}
