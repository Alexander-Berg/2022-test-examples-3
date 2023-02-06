package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class LocationKey {
    String loc;
    String nzn;
}
