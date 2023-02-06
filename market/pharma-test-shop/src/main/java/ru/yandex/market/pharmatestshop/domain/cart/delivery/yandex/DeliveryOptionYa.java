package ru.yandex.market.pharmatestshop.domain.cart.delivery.yandex;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import ru.yandex.market.pharmatestshop.domain.cart.delivery.DeliveryOption;
import ru.yandex.market.pharmatestshop.domain.cart.delivery.yandex.date.DatesYa;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryOptionYa implements DeliveryOption {
    private String serviceName;
    private int price;// = 0;
    private String type;//  = "DELIVERY";

    private DatesYa dates;
    private List<String> paymentMethods;
}
