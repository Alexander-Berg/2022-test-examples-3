package ru.yandex.market.pharmatestshop.domain.cart;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import ru.yandex.market.pharmatestshop.domain.cart.buyer.Buyer;
import ru.yandex.market.pharmatestshop.domain.cart.delivery.Delivery;
import ru.yandex.market.pharmatestshop.domain.cart.item.ItemDto;

@Builder
@Data
@JsonDeserialize
@AllArgsConstructor
@NoArgsConstructor
@JsonRootName(value = "cart")
public class CartDto {

    private Long businessId;
    private String currency;
    private Buyer buyer;
    @NotNull
    private List<ItemDto> items;
    private Delivery delivery;

}
