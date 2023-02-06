package ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.dto;

/**
 * Статусы позиций в заказе для кроссдок поставок.
 */
public enum SupplyOrderItemStatus {
    /**
     * Позиция в заказе создана.
     */
    CREATED(0),
    /**
     * Прошел катофф и создан драфт файл с поставкой, в которую вошла данная позиция.
     */
    DRAFT_FINALIZED(10),
    /**
     * Файл с поставкой, в которую входит данная позиция, загружен поставщиком в систему.
     */
    SUPPLY_UPLOADED(20),
    /**
     * Поставка, в которую входит данная позиция, создана на складе.
     */
    SUPPLY_ACCEPTED(30),
    /**
     * Работа с поставкой, в которую входит данная позиция, полностью завершена, дальшейших действий не предвидится.
     */
    SUPPLY_FINISHED(40),
    /**
     * Заказ, в который входит данная позиция, был отменен.
     */
    CANCELLED(100);

    private final int id;

    SupplyOrderItemStatus(int id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }
}
