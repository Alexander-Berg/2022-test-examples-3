package ru.yandex.market.pharmatestshop.domain.cart.delivery.pickup.outlet;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@JsonDeserialize
@AllArgsConstructor
@NoArgsConstructor
public class Outlet {
    private String code;
}
