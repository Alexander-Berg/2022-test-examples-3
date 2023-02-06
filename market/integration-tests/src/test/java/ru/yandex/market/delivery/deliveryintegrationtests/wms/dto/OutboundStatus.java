package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

/**
 * Статусы изъятий (в терминологии прослойки).
 */
public enum OutboundStatus {

    /**
     * Статус в который мапится всё, чего нет в мапинге.
     */
    UNIDENTIFIED(-1),
    /**
     * Заявка принята внешним сервисом. Пришел идентификатор заявки в складской системе.
     */
    CREATED(1),
    /**
     * Идет сборка товара по заявке
     */
    ASSEMBLING(310),
    /**
     * Сборка окончена, товар готов к передаче поставщику. На этом статусе делается запрос на склад на получение фактического количества собранного товара.
     */
    ASSEMBLED(320),
    /**
     * Передано/отгружено
     */
    TRANSFERRED(330),
    /**
     * Заявка отменена
     */
    CANCELLED(3);

    private final int id;

    OutboundStatus(int id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }
}
