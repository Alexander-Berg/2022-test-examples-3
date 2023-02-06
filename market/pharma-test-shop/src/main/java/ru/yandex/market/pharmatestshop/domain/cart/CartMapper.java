package ru.yandex.market.pharmatestshop.domain.cart;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import ru.yandex.market.pharmatestshop.domain.cart.item.Item;
import ru.yandex.market.pharmatestshop.domain.cart.item.ItemDto;

@Component
public class CartMapper {

    public static Cart map(CartDto cartDto) {
        List<Item> items = new ArrayList<>();

        if (cartDto.getItems() == null) {
            throw new IllegalArgumentException("Items in cart must not be empty!");
        }
        for (int i = 0; i < cartDto.getItems().size(); i++) {
            items.add(CartMapper.map(i, cartDto.getItems().get(i)));
        }

        return Cart.builder()
                .businessId(cartDto.getBusinessId())
                .deliveryCurrency(cartDto.getCurrency())
                .items(items)
                .build();
    }

    public static Item map(long i, ItemDto itemDto) {
        return Item.builder()
                .id(i)
                .feedId(itemDto.getFeedId())
                .offerId(itemDto.getOfferId())
                .count(itemDto.getCount())
                .delivery(true)
                .build();
    }

}
