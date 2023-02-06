package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
@RequiredArgsConstructor
public class DOOR extends Cell {
    String prefix = "DOOR";
    LocationType locationType = LocationType.DOOR;
    boolean loseId = false;
    @NonNull
    String zone;
}
