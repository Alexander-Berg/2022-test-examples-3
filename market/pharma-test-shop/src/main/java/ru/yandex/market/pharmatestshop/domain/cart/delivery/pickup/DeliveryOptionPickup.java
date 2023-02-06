package ru.yandex.market.pharmatestshop.domain.cart.delivery.pickup;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import ru.yandex.market.pharmatestshop.domain.cart.delivery.DeliveryOption;
import ru.yandex.market.pharmatestshop.domain.cart.delivery.pickup.date.DatesPickup;
import ru.yandex.market.pharmatestshop.domain.cart.delivery.pickup.outlet.Outlet;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryOptionPickup implements DeliveryOption {
    private String type;
    private int price;
    private String serviceName;
    private DatesPickup dates;
    private List<String> paymentMethods;
    private List<Outlet> outlets;
}
