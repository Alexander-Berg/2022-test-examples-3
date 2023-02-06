package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Статус заказа в WMS.
 */
public enum OrderStatus {

    CREATED("Создано внешне"),

    NOT_STARTED("Не запущено"),

    RESERVED("Зарезервировано"),

    PACKAGED("Пакетировано"),

    COMPLECTATION_STARTED("Выпущено"),

    COMPLECTATION_FINISHED("Отбор завершен"),

    PACKED("Упаковка завершена"),

    LOADED("Загружено"),

    SHIPPED("Отгрузка завершена"),

    OUT_OF_PICKING_LOT("Требуется пополнение"),

    ITEMS_AUTOMATICALLY_REMOVED("Отгружен с недостачей"),

    CANCELLED_INTERNALLY("Отменен внутренне"),

    ITEMS_OUT_OF_STOCK("Отменён из-за недостачи"),

    UNKNOWN("Неизвестно");

    private final String state;

    OrderStatus(String state) {
        this.state = state;
    }

    private static final Map<String, OrderStatus> strMap = new HashMap<>();

    static {
        for (OrderStatus s : OrderStatus.values()) {
            strMap.put(s.state, s);
        }
    }

    public static OrderStatus get(String s) {
        if (strMap.containsKey(s))
            return strMap.get(s);
        else {
            String errorString = String.format("No OrderStatus found for input: %s", s);
            throw new IllegalArgumentException(errorString);
        }
    }

    public String getState() {
        return state;
    }
}
