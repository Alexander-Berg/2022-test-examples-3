package ru.yandex.market.pharmatestshop.domain.cart;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import ru.yandex.market.pharmatestshop.domain.cart.delivery.DeliveryOption;
import ru.yandex.market.pharmatestshop.domain.cart.item.Item;

@Builder
@Data
@JsonDeserialize
@AllArgsConstructor
@NoArgsConstructor
@JsonRootName(value = "cart")
public class Cart {

    private Long businessId;
    private String deliveryCurrency;
    private List<Item> items;
    private List<DeliveryOption> deliveryOptions;
}
