package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

/**
 * Аттрибут партии 08.
 */
public enum Lottable08 {
    /**
     * Сток доступный для заказа.
     */
    YES(1),
    /**
     * Сток недоступный для заказа (например излишек)
     */
    NO(0);

    private final int id;

    Lottable08(int id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }
}
