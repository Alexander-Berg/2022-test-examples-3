package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Статус заказа во внешнем API.
 */
public enum ApiOrderStatus {

    SORTING_CENTER_CREATED(100),
    SORTING_CENTER_LOADED(101),
    SORTING_CENTER_ERROR(102),
    SERVICE_CENTER_CANCELED(105), //SORTING_CENTER_CANCELED in docs
    SORTING_CENTER_PACKAGING(110),  //SORTING_CENTER_AT_START in docs
    SORTING_CENTER_OUT_OF_STOCK(113),
    SORTING_CENTER_AWAITING_CLARIFICATION(117),
    SORTING_CENTER_PLACES_CHANGED(118),
    SORTING_CENTER_READY_TO_SHIP(120),  //SORTING_CENTER_PREPARED in docs
    SORTING_CENTER_TRANSMITTED(130),
    SORTING_CENTER_RETURN_PREPARING(160),
    SORTING_CENTER_RETURN_ARRIVED(170),
    SORTING_CENTER_RETURN_PREPARING_SENDER(175),
    SORTING_CENTER_RETURN_TRANSFERRED(177),
    SORTING_CENTER_RETURN_RETURNED(180),
    SORTING_CENTER_RETURN_RFF_PREPARING_FULFILLMENT(195),
    SORTING_CENTER_RETURN_RFF_TRANSMITTED_FULFILLMENT(197),
    SORTING_CENTER_RETURN_RFF_ARRIVED_FULFILLMENT(199);

    private final int id;

    ApiOrderStatus(int id) {
        this.id = id;
    }
    private static final Map<Integer, ApiOrderStatus> intMap = new HashMap<>();

    static {
        for (ApiOrderStatus s : ApiOrderStatus.values()) {
            intMap.put(s.id, s);
        }
    }

    public static ApiOrderStatus get(int i) {
        if (intMap.containsKey(i))
            return intMap.get(i);
        else {
            String errorString = String.format("No WrapOrderStatus found for input: %s", i);
            throw new IllegalArgumentException(errorString);
        }
    }

    public int getId() {
        return id;
    }
}
