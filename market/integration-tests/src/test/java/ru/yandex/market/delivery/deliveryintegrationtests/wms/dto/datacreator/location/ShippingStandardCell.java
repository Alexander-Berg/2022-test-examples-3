package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
@RequiredArgsConstructor
public class ShippingStandardCell extends Cell {
    @NonNull
    String zone;
    String prefix = "ATSC";
    LocationType locationType = LocationType.SHIP_STANDARD;
    boolean loseId = true;
}
