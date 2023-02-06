package ru.yandex.market.pharmatestshop.domain.cart;

import lombok.Data;

//Обертка над CartDto
@Data
public class CartRequest {
    private CartDto cart;
}
