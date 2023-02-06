package ru.yandex.market.pharmatestshop.domain.order;

public class OrderMapper {
    public static Order map(OrderDto orderDto) {


        return Order.builder()
                .id(String.valueOf(orderDto.getId()))
                .accepted(true)
                .build();
    }
}
