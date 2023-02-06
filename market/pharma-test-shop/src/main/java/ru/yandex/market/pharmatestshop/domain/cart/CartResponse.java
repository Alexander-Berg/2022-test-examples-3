package ru.yandex.market.pharmatestshop.domain.cart;

import lombok.Data;


//Обертка над классом Cart
@Data
public class CartResponse {

    private String status;
    private String message;
    private Cart cart;

}
