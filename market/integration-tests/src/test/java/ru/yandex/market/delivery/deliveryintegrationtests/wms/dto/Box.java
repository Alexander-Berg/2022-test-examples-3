package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder(toBuilder = true)
@Jacksonized
public class Box {
    String boxId;
    String countType;
}
