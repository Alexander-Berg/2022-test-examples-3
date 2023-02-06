package ru.yandex.market.pharmatestshop.domain.order.status;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum OrderStatus {
    // Whatever you do, DO NOT CHANGE THE ORDER!
    //@ApiModelProperty("Оформляется, подготовка к резервированию; заказ никому не виден и не доступен")
    PLACING(0),
    // @ApiModelProperty("Зарезервирован, но недооформлен; заказ не виден пользователю")
    RESERVED(1),
    // @ApiModelProperty("Оформлен, но не оплачен")
    UNPAID(2),
    //@ApiModelProperty("В обработке магазином")
    PROCESSING(3),
    // @ApiModelProperty("Передан в службу доставку")
    DELIVERY(4),
    // @ApiModelProperty("Доставлен на пункт выдачи")
    PICKUP(5),
    // @ApiModelProperty("Доставлен (вручен)")
    DELIVERED(6),
    // @ApiModelProperty("Отменен")
    CANCELLED(7),
    //@ApiModelProperty("Заказ ждет реакции магазина в веб-админке")
    PENDING(8),
    //@ApiModelProperty("Отменен без возврата средств (для Яндекс.Еда)")
    CANCELLED_WITHOUT_REFUND(9),

    @JsonEnumDefaultValue
    UNKNOWN(-1);
    private static final Map<Integer, OrderStatus> ORDER_STATUS_MAP = Arrays.stream(OrderStatus.values())
            .collect(Collectors.toMap(OrderStatus::getId, Function.identity()));

    private final int id;

    OrderStatus(int id) {
        this.id = id;
    }


    public int getId() {
        return id;
    }


    public boolean isUnknown() {
        return this == UNKNOWN;
    }

    public static OrderStatus getByIdOrUnknown(Integer id) {
        return id == null ? null : ORDER_STATUS_MAP.getOrDefault(id, OrderStatus.UNKNOWN);
    }


//    public OrderStatus increaseValue(OrderStatus s, DeliveryOption deliveryOption) {
//        if(deliveryOption instanceof DeliveryOptionYa){
//            /////////////////////
//        }
//
//        OrderStatus[] allValues = OrderStatus.values();
//        var newOrdinal = s.ordinal() + 1;
//        return (newOrdinal >= allValues.length) ? null) : allValues[newOrdinal];
//    }
//
//    public Size decreaseValue(Size s) {
//        var newOrdinal = s.ordinal() - 1;
//        return  (newOrdinal < 0) ? null : values()[newOrdinal];
//    }

}
