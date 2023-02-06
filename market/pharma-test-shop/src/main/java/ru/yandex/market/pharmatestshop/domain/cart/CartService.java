package ru.yandex.market.pharmatestshop.domain.cart;

import org.springframework.stereotype.Service;

@Service
public class CartService {

    public Cart getResponse(CartDto request) {
        return CartMapper.map(request);
    }

}
