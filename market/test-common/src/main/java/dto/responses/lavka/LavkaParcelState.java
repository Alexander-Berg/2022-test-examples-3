package dto.responses.lavka;

import lombok.Getter;

@Getter
public enum LavkaParcelState {
    //место под посылку зарезервировано
    RESERVED,

    //для посылки заполнен штрихкод
    CREATED,

    //ожидается поставка
    EXPECTING_DELIVERY,

    //посылка прибыла на склад
    IN_DEPOT,

    //пользователь положил посылку в корзину
    ORDERED,

    //пользователь отменил корзину в которой была посылка
    ORDER_CANCELLED,

    //заказ в лавке, готов к выдаче
    READY_FOR_DELIVERY,

    //курьер найден
    COURIER_ASSIGNED,

    //посылка доставляется клиенту
    DELIVERING,

    //посылка доставлена
    DELIVERED;

    private final String value = name().toLowerCase();
}
